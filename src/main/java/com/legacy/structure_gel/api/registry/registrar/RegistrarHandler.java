package com.legacy.structure_gel.api.registry.registrar;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableSet;
import com.legacy.structure_gel.api.registry.registrar.Registrar.DataGenPointer;
import com.legacy.structure_gel.core.StructureGelMod;

import net.minecraft.core.Registry;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.registries.RegisterEvent.RegisterHelper;

/**
 * Creates {@link Registrar}s and stores them for later use in registry. Call
 * {@linkplain #register(IEventBus) RegistrarHandler.register} during your mod's
 * constructor to register this instance to the event bus passed. Doing so will
 * allow it to register all held values.
 * <p>
 * For datagen, call {@link RegistrarHandler#injectRegistries} during the
 * {@link GatherDataEvent}. This will create a data generator that contains
 * registry data from all registrars in the mod IDs supplied.
 * <p>
 * This class is very similar to Forge's {@link DeferredRegister} system, but
 * comes with different variations of {@link RegistrarHandler}, such as
 * {@link BlockHandler} for registering a block that has an item.
 * 
 * @see #getOrCreate(ResourceKey, String)
 * 
 * @author Silver_David
 *
 * @param <T>
 */
public class RegistrarHandler<T>
{
	/**
	 * Map of registrar handlers by mod ID
	 */
	private static final Map<String, Map<ResourceKey<Registry<?>>, RegistrarHandler<?>>> HANDLERS = new HashMap<>();
	protected final ResourceKey<Registry<T>> registry;
	protected final String modID;

	private boolean registeredToBus = false;
	protected final LinkedHashMap<ResourceKey<T>, FutureRegister<T>> needsRegistered = new LinkedHashMap<>();
	private final List<Consumer<RegisterHelper<T>>> registerEventListeners = new ArrayList<>();
	private final List<Consumer<RegistrarHandler<T>>> handlerListeners = new ArrayList<>();

	protected final List<Registrar.DataGenPointer<?>> needsGenerated = new ArrayList<>(0);
	private final List<BootstrapInit<T>> bootstraps = new ArrayList<>(0);

	protected RegistrarHandler(ResourceKey<Registry<T>> registry, String modID)
	{
		this.registry = registry;
		this.modID = modID;
	}

	public String getModID()
	{
		return this.modID;
	}

	public ResourceKey<? extends Registry<?>> getRegistry()
	{
		return this.registry;
	}

	/**
	 * Creates a new {@link RegistrarHandler}, or gets an existing one if present.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <T> RegistrarHandler<T> getOrCreate(ResourceKey<Registry<T>> registry, String modID)
	{
		return getOrCreate(registry, modID, r ->
		{
			if (r.equals(Registries.BLOCK))
				return new BlockHandler(modID);
			else if (r.equals(Registries.ITEM))
				return new ItemHandler(modID);
			else if (r.equals(Registries.DATA_COMPONENT_TYPE))
				return new DataComponentHandler(modID);
			else if (r.equals(Registries.POINT_OF_INTEREST_TYPE))
				return new PoiTypeHandler(modID);
			return new RegistrarHandler(r, modID);
		});
	}

	public static RegistrarHandler.BlockHandler getOrCreateBlocks(String modID) throws ClassCastException
	{
		var handler = getOrCreate(Registries.BLOCK, modID, r -> new BlockHandler(modID));
		if (handler instanceof BlockHandler)
			return (BlockHandler) handler;
		throw new ClassCastException("[Structure Gel] Cannot get a " + RegistrarHandler.BlockHandler.class.getSimpleName() + " for " + modID + " because it already owns one as a " + handler.getClass().getSimpleName());
	}

	public static RegistrarHandler.ItemHandler getOrCreateItems(String modID) throws ClassCastException
	{
		var handler = getOrCreate(Registries.ITEM, modID, r -> new ItemHandler(modID));
		if (handler instanceof ItemHandler)
			return (ItemHandler) handler;
		throw new ClassCastException("[Structure Gel] Cannot get a " + RegistrarHandler.ItemHandler.class.getSimpleName() + " for " + modID + " because it already owns one as a " + handler.getClass().getSimpleName());
	}

	public static RegistrarHandler.DataComponentHandler getOrCreateDataComponents(String modID) throws ClassCastException
	{
		var handler = getOrCreate(Registries.DATA_COMPONENT_TYPE, modID, r -> new DataComponentHandler(modID));
		if (handler instanceof DataComponentHandler)
			return (DataComponentHandler) handler;
		throw new ClassCastException("[Structure Gel] Cannot get a " + RegistrarHandler.DataComponentHandler.class.getSimpleName() + " for " + modID + " because it already owns one as a " + handler.getClass().getSimpleName());
	}
	
	public static RegistrarHandler.PoiTypeHandler getOrCreatePoi(String modID) throws ClassCastException
	{
		var handler = getOrCreate(Registries.POINT_OF_INTEREST_TYPE, modID, r -> new PoiTypeHandler(modID));
		if (handler instanceof PoiTypeHandler)
			return (PoiTypeHandler) handler;
		throw new ClassCastException("[Structure Gel] Cannot get a " + RegistrarHandler.PoiTypeHandler.class.getSimpleName() + " for " + modID + " because it already owns one as a " + handler.getClass().getSimpleName());
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static <T> RegistrarHandler<T> getOrCreate(ResourceKey<Registry<T>> registry, String modID, Function<ResourceKey<Registry<?>>, RegistrarHandler<?>> handlerFactory)
	{
		synchronized (HANDLERS)
		{
			return HANDLERS.computeIfAbsent(modID, s -> new HashMap<>()).computeIfAbsent((ResourceKey) registry, handlerFactory);
		}
	}

	// ------- Static variables and register values to them later -------
	public Registrar.Static<T> createStatic(String key)
	{
		return this.createStatic(key, null);
	}

	public <R extends T, V extends R> void register(Registrar.Static<R> registrar, Supplier<V> value)
	{
		FutureRegister<T> future = this.needsRegistered.get(registrar.key);
		if (future != null)
			future.value = value;
	}

	// ------- Static registrars -------
	public <V extends T> Registrar.Static<V> createStatic(String name, Supplier<V> value)
	{
		return this.createStatic(this.key(name), value);
	}

	public <V extends T> Registrar.Static<V> createStatic(ResourceKey<T> name, Supplier<V> value)
	{
		Registrar.Static<V> ret = Registrar.createStatic(name);
		this.needsRegistered.put(name, new FutureRegister<>(ret, value));
		return ret;
	}

	// ------- Datapack registrars -------
	public <V extends T> Registrar.Pointer<V> createPointer(String name, Supplier<V> value)
	{
		return this.createPointer(name, c -> value.get());
	}

	public <V extends T> Registrar.Pointer<V> createPointer(String name, Function<BootstrapContext<?>, V> value)
	{
		return this.createPointer(this.key(name), value);
	}

	public <V extends T> Registrar.Pointer<V> createPointer(ResourceKey<T> name, Function<BootstrapContext<?>, V> value)
	{
		var ret = Registrar.createPointer(name, value);
		if (ret instanceof DataGenPointer)
			this.needsGenerated.add((DataGenPointer<?>) ret);
		return ret;
	}

	/**
	 * Runs the bootstrap instance passed when running datagen to register values.
	 * Bootstraps will run before registrars are handled.
	 */
	public RegistrarHandler<T> bootstrap(BootstrapInit<T> bootstrap)
	{
		this.bootstraps.add(bootstrap);
		return this;
	}

	public RegistrarHandler<T> addListener(Consumer<RegisterHelper<T>> registerHelper)
	{
		this.registerEventListeners.add(registerHelper);
		return this;
	}

	@SuppressWarnings({ "unchecked", "rawtypes"})
	public <R extends RegistrarHandler<T>> R addHandlerListener(Consumer<R> handlerListener)
	{
		this.handlerListeners.add((Consumer) handlerListener);
		return (R) this;
	}

	/**
	 * @return A resource key for then name passed
	 */
	public ResourceKey<T> key(String name)
	{
		return ResourceKey.create(this.registry, ResourceLocation.tryBuild(this.modID, name));
	}

	/**
	 * @return A tag key for the name passed
	 */
	public TagKey<T> tagKey(String name)
	{
		return TagKey.create(this.registry, ResourceLocation.tryBuild(this.modID, name));
	}

	/**
	 * Registers all initialized {@link RegistrarHandler}s owned by the modID passed
	 * and any additional handlers passed to the mod event bus.
	 * 
	 * @param modID
	 *            Your mod ID
	 * @param modBus
	 *            Your mod's event bus
	 * @param handlers
	 *            {@link RegistrarHandler}s associated with your mod
	 */
	public static void registerHandlers(String modID, IEventBus modBus, RegistrarHandler<?>... handlers)
	{
		registerHandlers(modID, modBus, List.of(handlers));
	}

	/**
	 * Registers all initialized {@link RegistrarHandler}s owned by the modID passed
	 * and any additional handlers passed to the mod event bus.
	 * 
	 * @param modID
	 *            Your mod ID
	 * @param modBus
	 *            Your mod's event bus
	 * @param handlers
	 *            {@link RegistrarHandler}s associated with your mod
	 */
	public static void registerHandlers(String modID, IEventBus modBus, Collection<RegistrarHandler<?>> handlers)
	{
		GroupedRegistryListener grouped = new GroupedRegistryListener();
		synchronized (HANDLERS)
		{
			HANDLERS.getOrDefault(modID, Map.of()).values().forEach(grouped::add);
		}
		handlers.forEach(grouped::add);

		modBus.addListener(EventPriority.NORMAL, grouped::onRegister);
	}

	private static class GroupedRegistryListener
	{
		public final Map<ResourceKey<? extends Registry<?>>, List<Consumer<RegisterEvent>>> registerListeners = new HashMap<>();

		public void add(RegistrarHandler<?> handler)
		{
			if (!handler.registeredToBus) // Prevents being registered twice
			{
				this.registerListeners.computeIfAbsent(handler.getRegistry(), r -> new ArrayList<>(1)).add(handler::registerValues);
				handler.registeredToBus = true;
			}
		}

		public void onRegister(RegisterEvent event)
		{
			List<Consumer<RegisterEvent>> listeners = this.registerListeners.get(event.getRegistryKey());
			if (listeners != null)
				for (var listener : listeners)
					listener.accept(event);
		}
	}

	/**
	 * Registers this handler to the event bus passed.
	 * 
	 * @param modBus
	 *            Your mod's event bus
	 */
	public void register(IEventBus modBus)
	{
		if (!this.registeredToBus) // Prevents being registered twice
		{
			modBus.addListener(EventPriority.NORMAL, this::registerValues);
			this.registeredToBus = true;
		}
	}

	/**
	 * Registers the {@link Registrar}s associated with this handler. Automatically
	 * checks if the registry from the event matches the registry for this
	 * registrar.
	 * 
	 * @param event
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void registerValues(RegisterEvent event)
	{
		if (this.registry.equals(event.getRegistryKey()))
		{
			this.handlerListeners.forEach(h -> h.accept(this));

			this.registerEventListeners.forEach(c -> event.register(this.registry, c));
			
			this.needsRegistered.values().forEach(f -> f.registrar.register(event, (Supplier) f.value));
			this.needsRegistered.clear();
		}
	}

	public static RegistrySetBuilder injectRegistries(RegistrySetBuilder builder)
	{
		return injectRegistries(builder, HANDLERS.keySet());
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static RegistrySetBuilder injectRegistries(RegistrySetBuilder builder, Set<String> mods)
	{
		Map<ResourceKey<Registry<?>>, List<RegistrarHandler<?>>> registrars = new HashMap<>();
		for (var mod : mods)
		{
			var handlers = HANDLERS.get(mod);
			if (handlers != null)
			{
				for (var entry : handlers.entrySet())
				{
					RegistrarHandler<?> handler = entry.getValue();
					if (!handler.isBuiltIn())
						registrars.computeIfAbsent(entry.getKey(), r -> new ArrayList<>()).add(handler);
				}
			}
		}

		for (var entry : registrars.entrySet())
		{
			builder.add((ResourceKey) entry.getKey(), c -> entry.getValue().forEach(r -> r.registerForDatagen((BootstrapContext) c))); // gradlew publish fails if I don't cast ResourceKey. Oh well.
		}
		return builder;
	}

	public boolean isBuiltIn()
	{
		return BuiltInRegistries.REGISTRY.containsKey(this.registry.location());
	}

	private void registerForDatagen(BootstrapContext<T> context)
	{
		StructureGelMod.LOGGER.info("Registering {} ({})", this.registry.location(), this.modID);

		this.bootstraps.forEach(b -> b.run(context));

		int s = this.needsGenerated.size();
		for (int i = 0; i < s; i++)
		{
			this.needsGenerated.get(i).registerData(context);
		}
	}

	@Override
	public String toString()
	{
		return this.getClass().getSimpleName() + "[owner = " + this.modID + ", registry = " + this.registry.location() + "]";
	}

	public static final class BlockHandler extends RegistrarHandler<Block>
	{
		protected BlockHandler(String modID)
		{
			super(Registries.BLOCK, modID);
		}

		//--------------- Inline holder register --------------
		public <V extends Block> Registrar.BlockRef<V> blockOnly(String name, Function<BlockBehaviour.Properties, V> blockFactory, Supplier<BlockBehaviour.Properties> properties)
		{
			ResourceKey<Block> key = this.key(name);
			Registrar.BlockRef<V> ret = Registrar.createBlockRef(key);
			this.needsRegistered.put(key, new FutureRegister<>(ret, () -> blockFactory.apply(properties.get())));
			return ret;
		}

		public <V extends Block> Registrar.BlockRef<V> block(String key, Function<BlockBehaviour.Properties, V> blockFactory, Supplier<BlockBehaviour.Properties> properties)
		{
			return this.block(key, blockFactory, properties, () -> new Item.Properties());
		}
		
		public <V extends Block> Registrar.BlockRef<V> block(String key, Function<BlockBehaviour.Properties, V> blockFactory, Supplier<BlockBehaviour.Properties> properties, Supplier<Item.Properties> itemProperties)
		{
			return this.block(key, blockFactory, properties, BlockItem::new, itemProperties);
		}

		public <V extends Block, I extends Item> Registrar.BlockRef<V> block(String name, Function<BlockBehaviour.Properties, V> blockFactory, Supplier<BlockBehaviour.Properties> properties, BiFunction<V, Item.Properties, I> itemFactory, Supplier<Item.Properties> itemProperties)
		{
			Registrar.BlockRef<V> ret = this.blockOnly(name, blockFactory, properties);
			getOrCreateItems(this.modID).item(name, p -> itemFactory.apply(ret.get(), p), itemProperties);
			return ret;
		}
		
		//--------------- Static value holder register --------------
		public <V extends Block> Registrar.BlockRef<V> block(String name)
		{
			ResourceKey<Block> key = this.key(name);
			Registrar.BlockRef<V> ret = Registrar.createBlockRef(key);
			this.needsRegistered.put(key, new FutureRegister<>(ret, null));
			return ret;
		}
		
		public <V extends Block> void registerBlockOnly(Registrar.Static<V> registrar, Function<BlockBehaviour.Properties, V> blockFactory, Supplier<BlockBehaviour.Properties> properties)
		{
			super.register(registrar, () -> blockFactory.apply(properties.get()));
		}

		public <V extends Block> void registerBlock(Registrar.Static<V> registrar, Function<BlockBehaviour.Properties, V> blockFactory, Supplier<BlockBehaviour.Properties> properties)
		{
			this.registerBlock(registrar, blockFactory, properties, () -> new Item.Properties());
		}
		
		public <V extends Block> void registerBlock(Registrar.Static<V> registrar, Function<BlockBehaviour.Properties, V> blockFactory, Supplier<BlockBehaviour.Properties> properties, Supplier<Item.Properties> itemProperties)
		{
			this.registerBlock(registrar, blockFactory, properties, BlockItem::new, itemProperties);
		}

		public <V extends Block, I extends Item> void registerBlock(Registrar.Static<V> registrar, Function<BlockBehaviour.Properties, V> blockFactory, Supplier<BlockBehaviour.Properties> properties, BiFunction<V, Item.Properties, I> itemFactory, Supplier<Item.Properties> itemProperties)
		{
			this.registerBlockOnly(registrar, blockFactory, properties);
			getOrCreateItems(this.modID).item(registrar.key.location().getPath(), p -> itemFactory.apply(registrar.get(), p), itemProperties);
		}
	}

	public static final class ItemHandler extends RegistrarHandler<Item>
	{
		protected ItemHandler(String modID)
		{
			super(Registries.ITEM, modID);
		}

		//--------------- Inline holder register --------------
		public <V extends Item> Registrar.ItemRef<V> item(String name, Function<Item.Properties, V> itemFactory, Supplier<Item.Properties> properties)
		{
			ResourceKey<Item> key = this.key(name);
			Registrar.ItemRef<V> ret = Registrar.createItemRef(key);
			this.needsRegistered.put(key, new FutureRegister<>(ret, () -> itemFactory.apply(properties.get())));
			return ret;
		}

		//--------------- Static value holder register --------------
		public <V extends Item> Registrar.ItemRef<V> registerItem(String name)
		{
			ResourceKey<Item> key = this.key(name);
			Registrar.ItemRef<V> ret = Registrar.createItemRef(key);
			this.needsRegistered.put(key, new FutureRegister<>(ret, null));
			return ret;
		} 
		
		public <V extends Item> void registerItem(Registrar.Static<V> registrar, Function<Item.Properties, V> itemFactory, Supplier<Item.Properties> properties)
		{
			super.register(registrar, () -> itemFactory.apply(properties.get()));
		}
	}

	public static final class DataComponentHandler extends RegistrarHandler<DataComponentType<?>>
	{
		protected DataComponentHandler(String modID)
		{
			super(Registries.DATA_COMPONENT_TYPE, modID);
		}

		//--------------- Inline holder register --------------
		public <T> Registrar.Static<DataComponentType<T>> component(String key, UnaryOperator<DataComponentType.Builder<T>> builder)
		{
			return this.createStatic(key, builder.apply(DataComponentType.builder())::build);
		}
	}
	
	public static final class PoiTypeHandler extends RegistrarHandler<PoiType>
	{
		protected PoiTypeHandler(String modID)
		{
			super(Registries.POINT_OF_INTEREST_TYPE, modID);
		}

		//--------------- Inline holder register --------------
		public Registrar.Static<PoiType> poi(String key, UnaryOperator<PoiBuilder> value)
		{
			return this.createStatic(key, () -> value.apply(new PoiBuilder()).build());
		}
		
		public static class PoiBuilder
		{
			final Set<BlockState> states = new HashSet<>();
			int maxTickets = 0;
			int validRange = 1;
			
			public PoiBuilder states(Block... blocks)
			{
				Stream.of(blocks).flatMap(b -> b.getStateDefinition().getPossibleStates().stream()).forEach(states::add);
				return this;
			}
			
			public PoiBuilder maxTickets(int maxTickets)
			{
				this.maxTickets = maxTickets;
				return this;
			}
			
			public PoiBuilder validRange(int validRange)
			{
				this.validRange = validRange;
				return this;
			}
			
			private PoiType build()
			{
				return new PoiType(ImmutableSet.copyOf(states), maxTickets, validRange);
			}
		}
	}

	/**
	 * Initializes registry data using {@link BootstrapContext}
	 * 
	 * @author Silver_David
	 *
	 * @param <T>
	 */
	public static interface BootstrapInit<T>
	{
		void run(BootstrapContext<T> context);
	}

	private static class FutureRegister<T>
	{
		final Registrar.Static<? extends T> registrar;
		Supplier<? extends T> value;

		FutureRegister(Registrar.Static<? extends T> registrar, Supplier<? extends T> value)
		{
			this.registrar = registrar;
			this.value = value;
		}
	}
}
