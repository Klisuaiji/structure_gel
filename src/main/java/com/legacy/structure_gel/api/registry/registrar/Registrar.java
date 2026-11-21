package com.legacy.structure_gel.api.registry.registrar;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.legacy.structure_gel.core.util.Internal;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.CommonLevelAccessor;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.data.loading.DatagenModLoader;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * Pointer to a registry object. Data is registered manually either manually by
 * the user, or automatically with {@link RegistrarHandler}.
 * <p>
 * This class behaves similarly to an {@link Optional} and a {@link Supplier}.
 *
 * @author Silver_David
 */
public abstract class Registrar<T>
{
	protected final ResourceKey<T> key;

	public Registrar(ResourceKey<T> key)
	{
		this.key = key;
	}
	
	/**
	 * Used for objects that have data pack registries where obtaining a value from
	 * current registry data is necessary, such as biomes. The value passed in is
	 * only used for datagen.
	 */
	public static <T, V extends T> Registrar.Pointer<V> createPointer(ResourceKey<T> key, Supplier<V> value)
	{
		return createPointer(key, c -> value.get());
	}

	/**
	 * Used for objects that have data pack registries where obtaining a value from
	 * current registry data is necessary, such as biomes. The value passed in is
	 * only used for datagen.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static <T, V extends T> Registrar.Pointer<V> createPointer(ResourceKey<T> key, Function<BootstrapContext<?>, V> value)
	{
		return DatagenModLoader.isRunningDataGen() ? new Registrar.DataGenPointer(key, value) : new Registrar.Pointer(key);
	}

	/**
	 * Used for objects that are created on startup and can safely be stored in a
	 * static instance, such as blocks or entity types.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static <T, V extends T> Registrar.Static<V> createStatic(ResourceKey<T> key)
	{
		return new Registrar.Static(key);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static <T, V extends Block> Registrar.BlockRef<V> createBlockRef(ResourceKey<T> key)
	{
		return new Registrar.BlockRef(key);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static <T, V extends Item> Registrar.ItemRef<V> createItemRef(ResourceKey<T> key)
	{
		return new Registrar.ItemRef(key);
	}

	@Nullable
	public T get(RegistryAccess registryAccess)
	{
		Registry<?> registry = registryAccess.registryOrThrow(this.getRegistryKey());
		return this.get(registry);
	}

	@Nullable
	public T get(CommonLevelAccessor level)
	{
		return this.get(level.registryAccess());
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Nullable
	public T get(Registry<?> registry)
	{
		return (T) registry.get((ResourceKey) this.getKey());
	}

	@Nullable
	public T orElseGet(Registry<?> registry, Supplier<T> orElse)
	{
		T val = this.get(registry);
		return val != null ? val : orElse.get();
	}

	public boolean isPresent(Registry<?> registry)
	{
		return registry.containsKey(this.getName());
	}

	@Nullable
	public <R> R map(Registry<?> registry, Function<T, R> mapFunc)
	{
		if (this.isPresent(registry))
			return mapFunc.apply(this.get(registry));
		return null;
	}

	public Optional<Holder<T>> getHolder(RegistryAccess registryAccess)
	{
		Registry<?> registry = registryAccess.registryOrThrow(this.getRegistryKey());
		return this.getHolder(registry);
	}

	public Optional<Holder<T>> getHolder(CommonLevelAccessor level)
	{
		return this.getHolder(level.registryAccess());
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Optional<Holder<T>> getHolder(Registry<?> registry)
	{
		return (Optional<Holder<T>>) (Optional<?>) registry.getHolder((ResourceKey) this.getKey());
	}

	public Optional<Holder.Reference<T>> getHolder(BootstrapContext<?> bootstrapContext)
	{
		return this.getHolder(bootstrapContext.lookup(this.getRegistryKey()));
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Optional<Holder.Reference<T>> getHolder(HolderGetter<?> holderGetter)
	{
		return holderGetter.get((ResourceKey) this.getKey());
	}

	public ResourceKey<T> getKey()
	{
		return this.key;
	}

	public ResourceKey<? extends Registry<?>> getRegistryKey()
	{
		return this.getKey().registryKey();
	}

	public ResourceLocation getName()
	{
		return this.getKey().location();
	}

	@Override
	public String toString()
	{
		return this.getClass().getSimpleName() + "[key=" + this.getKey() + "]";
	}

	/**
	 * A {@link Registrar} to be used by objects that have data pack registries
	 * where obtaining a value from current registry data is necessary, such as
	 * biomes.
	 * 
	 * @author Silver_David
	 *
	 * @param <T>
	 */
	public static class Pointer<T> extends Registrar<T>
	{		
		protected Pointer(ResourceKey<T> key)
		{
			super(key);
		}
	}

	/**
	 * Internal variation of Pointer used specifically for datagen
	 * 
	 * @param <T>
	 */
	@Internal
	protected static class DataGenPointer<T> extends Pointer<T>
	{
		protected final Function<BootstrapContext<?>, T> valueFactory;

		protected DataGenPointer(ResourceKey<T> key, Function<BootstrapContext<?>, T> valueFactory)
		{
			super(key);
			this.valueFactory = valueFactory;
		}

		@SuppressWarnings({ "rawtypes", "unchecked" })
		public void registerData(BootstrapContext<?> context)
		{
			((BootstrapContext) context).register(this.getKey(), this.valueFactory.apply(context));
		}
	}

	/**
	 * A {@link Registrar} to be used for objects that are created on startup and
	 * can safely be stored in a static instance, such as blocks or entity types.
	 * 
	 * @author Silver_David
	 *
	 * @param <T>
	 */
	public static class Static<T> extends Registrar<T> implements Supplier<T>
	{
		@Nullable
		protected Holder.Reference<T> holder;

		protected Static(ResourceKey<T> key)
		{
			super(key);
		}
		
		protected void register(RegisterEvent event, Supplier<T> valueSupplier)
		{
			if (this.holder == null)
			{
				var registry = event.getRegistry(this.getKey().registryKey());
				if (registry != null)
				{
					if (valueSupplier != null)
					{
						this.holder = Registry.registerForHolder(registry, this.getKey(), valueSupplier.get());
					}
					else
						throw new IllegalStateException("The Static Registrar for " + key + " was not assigned a value");
				}
			}
		}

		@Override
		public ResourceKey<T> getKey()
		{
			return this.key;
		}
		
		@SuppressWarnings("unchecked")
		public <R> ResourceKey<R> getKey(ResourceKey<Registry<R>> registryKey)
		{
			return (ResourceKey<R>) this.key;
		}
		
		@Override
		@Nullable
		public T get()
		{
			return this.getHolder().value();
		}

		@Nullable
		public Holder.Reference<T> getHolder()
		{
			if (this.holder == null)
				throw new IllegalStateException("Attempted to get a holder before it was registered.");
			return this.holder;
		}

		public T orElseGet(Supplier<T> orElse)
		{
			return this.isPresent() ? this.get() : orElse.get();
		}

		public boolean isPresent()
		{
			return this.holder != null;
		}

		@Nullable
		public <R> R map(Function<T, R> mapFunc)
		{
			if (this.isPresent())
				return mapFunc.apply(this.get());
			return null;
		}
	}

	public static class BlockRef<T extends Block> extends Registrar.Static<T> implements ItemLike
	{
		protected BlockRef(ResourceKey<T> key)
		{
			super(key);
		}

		@Override
		public Item asItem()
		{
			return this.get().asItem();
		}

		public BlockState defaultBlockState()
		{
			return this.get().defaultBlockState();
		}
	}

	public static class ItemRef<T extends Item> extends Registrar.Static<T> implements ItemLike
	{
		protected ItemRef(ResourceKey<T> key)
		{
			super(key);
		}

		@Override
		public Item asItem()
		{
			return this.get();
		}

		public ItemStack getDefaultInstance()
		{
			return this.get().getDefaultInstance();
		}
	}
}
