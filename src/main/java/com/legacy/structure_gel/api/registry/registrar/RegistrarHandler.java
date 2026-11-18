package com.legacy.structure_gel.api.registry.registrar;

import com.legacy.structure_gel.core.StructureGelMod;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.registries.RegisterEvent.RegisterHelper;

public class RegistrarHandler<T> {
   private static final Map<String, Map<ResourceKey<Registry<?>>, RegistrarHandler<?>>> HANDLERS = new HashMap<>();
   protected final ResourceKey<Registry<T>> registry;
   protected final String modID;
   private boolean registeredToBus = false;
   protected final List<Registrar.Static<?>> needsRegistered = new ArrayList<>();
   private final List<Consumer<RegisterHelper<T>>> registerEventListeners = new ArrayList<>();
   protected final List<Registrar.DataGenPointer<?>> needsGenerated = new ArrayList<>(0);
   private final List<RegistrarHandler.BootstrapInit<T>> bootstraps = new ArrayList<>(0);

   protected RegistrarHandler(ResourceKey<Registry<T>> registry, String modID) {
      this.registry = registry;
      this.modID = modID;
   }

   public String getModID() {
      return this.modID;
   }

   public ResourceKey<? extends Registry<?>> getRegistry() {
      return this.registry;
   }

   public static <T> RegistrarHandler<T> getOrCreate(ResourceKey<Registry<T>> registry, String modID) {
      return getOrCreate(
         registry,
         modID,
         r -> {
            if (r.equals(Registries.BLOCK)) {
               return new RegistrarHandler.BlockHandler(modID);
            } else {
               return (RegistrarHandler<?>)(r.equals(Registries.ITEM)
                  ? new RegistrarHandler.ItemHandler(modID)
                  : new RegistrarHandler((ResourceKey<Registry<T>>)r, modID));
            }
         }
      );
   }

   public static RegistrarHandler.BlockHandler getOrCreateBlocks(String modID) throws ClassCastException {
      RegistrarHandler<Block> handler = getOrCreate(Registries.BLOCK, modID, r -> new RegistrarHandler.BlockHandler(modID));
      if (handler instanceof RegistrarHandler.BlockHandler) {
         return (RegistrarHandler.BlockHandler)handler;
      } else {
         throw new ClassCastException(
            "[Structure Gel] Cannot get a "
               + RegistrarHandler.BlockHandler.class.getSimpleName()
               + " for "
               + modID
               + " because it already owns one as a "
               + handler.getClass().getSimpleName()
         );
      }
   }

   public static RegistrarHandler.ItemHandler getOrCreateItems(String modID) throws ClassCastException {
      RegistrarHandler<Item> handler = getOrCreate(Registries.ITEM, modID, r -> new RegistrarHandler.ItemHandler(modID));
      if (handler instanceof RegistrarHandler.ItemHandler) {
         return (RegistrarHandler.ItemHandler)handler;
      } else {
         throw new ClassCastException(
            "[Structure Gel] Cannot get a "
               + RegistrarHandler.ItemHandler.class.getSimpleName()
               + " for "
               + modID
               + " because it already owns one as a "
               + handler.getClass().getSimpleName()
         );
      }
   }

   private static <T> RegistrarHandler<T> getOrCreate(
      ResourceKey<Registry<T>> registry, String modID, Function<ResourceKey<Registry<?>>, RegistrarHandler<?>> handlerFactory
   ) {
      synchronized (HANDLERS) {
         return (RegistrarHandler<T>)HANDLERS.computeIfAbsent(modID, s -> new HashMap<>()).computeIfAbsent(registry, handlerFactory);
      }
   }

   public <V extends T> Registrar.Static<V> createStatic(String key, Supplier<V> value) {
      return this.createStatic(new ResourceLocation(this.modID, key), value);
   }

   public <V extends T> Registrar.Static<V> createStatic(ResourceLocation key, Supplier<V> value) {
      Registrar.Static<V> ret = Registrar.createStatic(this.registry, key, value);
      this.needsRegistered.add(ret);
      return ret;
   }

   public <V extends T> Registrar.Pointer<V> createPointer(String key, Supplier<V> value) {
      return this.createPointer(key, c -> value.get());
   }

   public <V extends T> Registrar.Pointer<V> createPointer(ResourceLocation key, Supplier<V> value) {
      return this.createPointer(key, c -> value.get());
   }

   public <V extends T> Registrar.Pointer<V> createPointer(String key, Function<BootstapContext<?>, V> value) {
      return this.createPointer(new ResourceLocation(this.modID, key), value);
   }

   public <V extends T> Registrar.Pointer<V> createPointer(ResourceLocation key, Function<BootstapContext<?>, V> value) {
      Registrar.Pointer<V> ret = Registrar.createPointer(this.registry, key, value);
      if (ret instanceof Registrar.DataGenPointer) {
         this.needsGenerated.add((Registrar.DataGenPointer<?>)ret);
      }

      return ret;
   }

   public RegistrarHandler<T> bootstrap(RegistrarHandler.BootstrapInit<T> bootstrap) {
      this.bootstraps.add(bootstrap);
      return this;
   }

   public RegistrarHandler<T> addListener(Consumer<RegisterHelper<T>> registerHelper) {
      this.registerEventListeners.add(registerHelper);
      return this;
   }

   public ResourceKey<T> key(String name) {
      return ResourceKey.create(this.registry, new ResourceLocation(this.modID, name));
   }

   public TagKey<T> tagKey(String name) {
      return TagKey.create(this.registry, new ResourceLocation(this.modID, name));
   }

   public static void registerHandlers(String modID, IEventBus modBus, RegistrarHandler<?>... handlers) {
      registerHandlers(modID, modBus, List.of(handlers));
   }

   public static void registerHandlers(String modID, IEventBus modBus, Collection<RegistrarHandler<?>> handlers) {
      RegistrarHandler.GroupedRegistryListener grouped = new RegistrarHandler.GroupedRegistryListener();
      synchronized (HANDLERS) {
         HANDLERS.getOrDefault(modID, Map.of()).values().forEach(grouped::add);
      }

      handlers.forEach(grouped::add);
      modBus.addListener(EventPriority.NORMAL, grouped::onRegister);
   }

   public void register(IEventBus modBus) {
      if (!this.registeredToBus) {
         modBus.addListener(EventPriority.NORMAL, this::registerValues);
         this.registeredToBus = true;
      }
   }

   public void registerValues(RegisterEvent event) {
      if (this.registry.equals(event.getRegistryKey())) {
         this.registerEventListeners.forEach(c -> event.register(this.registry, c));
         int s = this.needsRegistered.size();

         for (int i = 0; i < s; i++) {
            this.needsRegistered.get(i).register(event);
         }

         this.needsRegistered.clear();
      }
   }

   public static RegistrySetBuilder injectRegistries(RegistrySetBuilder builder) {
      return injectRegistries(builder, HANDLERS.keySet());
   }

   public static RegistrySetBuilder injectRegistries(RegistrySetBuilder builder, Set<String> mods) {
      Map<ResourceKey<Registry<?>>, List<RegistrarHandler<?>>> registrars = new HashMap<>();

      for (String mod : mods) {
         Map<ResourceKey<Registry<?>>, RegistrarHandler<?>> handlers = HANDLERS.get(mod);
         if (handlers != null) {
            for (Entry<ResourceKey<Registry<?>>, RegistrarHandler<?>> entry : handlers.entrySet()) {
               RegistrarHandler<?> handler = entry.getValue();
               if (!handler.isBuiltIn()) {
                  registrars.computeIfAbsent(entry.getKey(), r -> new ArrayList<>()).add(handler);
               }
            }
         }
      }

      for (Entry<ResourceKey<Registry<?>>, List<RegistrarHandler<?>>> entryx : registrars.entrySet()) {
         builder.add(entryx.getKey(), c -> entry.getValue().forEach(r -> r.registerForDatagen(c)));
      }

      return builder;
   }

   public boolean isBuiltIn() {
      return BuiltInRegistries.REGISTRY.containsKey(this.registry.location());
   }

   private void registerForDatagen(BootstapContext<T> context) {
      StructureGelMod.LOGGER.info("Registering {} ({})", this.registry.location(), this.modID);
      this.bootstraps.forEach(b -> b.run(context));
      int s = this.needsGenerated.size();

      for (int i = 0; i < s; i++) {
         this.needsGenerated.get(i).registerData(context);
      }
   }

   @Override
   public String toString() {
      return this.getClass().getSimpleName() + "[owner = " + this.modID + ", registry = " + this.registry.location() + "]";
   }

   public static final class BlockHandler extends RegistrarHandler<Block> {
      protected BlockHandler(String modID) {
         super(Registries.BLOCK, modID);
      }

      public <V extends Block> Registrar.BlockRef<V> createStatic(String key, Supplier<V> value) {
         return this.createStatic(new ResourceLocation(this.modID, key), value);
      }

      public <V extends Block> Registrar.BlockRef<V> createStatic(ResourceLocation key, Supplier<V> value) {
         Registrar.BlockRef<V> ret = Registrar.createBlockRef((ResourceKey<Registry<T>>)this.registry, key, value);
         this.needsRegistered.add(ret);
         return ret;
      }

      public <V extends Block> Registrar.BlockRef<V> createStatic(String key, Supplier<V> value, Properties itemProperties) {
         return this.createStatic(key, value, itemProperties, BlockItem::new);
      }

      public <V extends Block> Registrar.BlockRef<V> createStatic(ResourceLocation key, Supplier<V> value, Properties itemProperties) {
         return this.createStatic(key, value, itemProperties, BlockItem::new);
      }

      public <V extends Block> Registrar.BlockRef<V> createStatic(
         String key, Supplier<V> value, Properties itemProperties, BiFunction<V, Properties, BlockItem> blockItemFactory
      ) {
         return this.createStatic(new ResourceLocation(this.modID, key), value, itemProperties, blockItemFactory);
      }

      public <V extends Block> Registrar.BlockRef<V> createStatic(
         ResourceLocation key, Supplier<V> value, Properties itemProperties, BiFunction<V, Properties, BlockItem> blockItemFactory
      ) {
         Registrar.BlockRef<V> ret = this.createStatic(key, value);
         getOrCreateItems(this.modID).createStatic(key, () -> (V)((BlockItem)blockItemFactory.apply(ret.get(), itemProperties)));
         return ret;
      }
   }

   public interface BootstrapInit<T> {
      void run(BootstapContext<T> var1);
   }

   private static class GroupedRegistryListener {
      public final Map<ResourceKey<? extends Registry<?>>, List<Consumer<RegisterEvent>>> registerListeners = new HashMap<>();

      public void add(RegistrarHandler<?> handler) {
         if (!handler.registeredToBus) {
            this.registerListeners.computeIfAbsent(handler.getRegistry(), r -> new ArrayList<>(1)).add(handler::registerValues);
            handler.registeredToBus = true;
         }
      }

      public void onRegister(RegisterEvent event) {
         List<Consumer<RegisterEvent>> listeners = this.registerListeners.get(event.getRegistryKey());
         if (listeners != null) {
            for (Consumer<RegisterEvent> listener : listeners) {
               listener.accept(event);
            }
         }
      }
   }

   public static final class ItemHandler extends RegistrarHandler<Item> {
      protected ItemHandler(String modID) {
         super(Registries.ITEM, modID);
      }

      public <V extends Item> Registrar.ItemRef<V> createStatic(String key, Supplier<V> value) {
         return this.createStatic(new ResourceLocation(this.modID, key), value);
      }

      public <V extends Item> Registrar.ItemRef<V> createStatic(ResourceLocation key, Supplier<V> value) {
         Registrar.ItemRef<V> ret = Registrar.createItemRef((ResourceKey<Registry<T>>)this.registry, key, value);
         this.needsRegistered.add(ret);
         return ret;
      }
   }
}
