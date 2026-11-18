package com.legacy.structure_gel.api.registry.registrar;

import com.legacy.structure_gel.api.util.LazyOptional;
import com.legacy.structure_gel.core.util.Internal;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.Holder.Reference;
import net.minecraft.data.worldgen.BootstapContext;
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

public abstract class Registrar<T> {
   protected final ResourceKey<Registry<T>> registryKey;
   protected final ResourceKey<T> key;

   protected Registrar(ResourceKey<Registry<T>> registry, ResourceLocation name) {
      this.registryKey = registry;
      this.key = ResourceKey.create(registry, name);
   }

   public static <T, V extends T> Registrar.Pointer<V> createPointer(ResourceKey<Registry<T>> registry, ResourceLocation name, Supplier<V> value) {
      return createPointer(registry, name, c -> value.get());
   }

   public static <T, V extends T> Registrar.Pointer<V> createPointer(
      ResourceKey<Registry<T>> registry, ResourceLocation name, Function<BootstapContext<?>, V> value
   ) {
      return (Registrar.Pointer<V>)(DatagenModLoader.isRunningDataGen()
         ? new Registrar.DataGenPointer<>(registry, name, value)
         : new Registrar.Pointer<>(registry, name));
   }

   public static <T, V extends T> Registrar.Static<V> createStatic(ResourceKey<Registry<T>> registry, ResourceLocation name, Supplier<V> value) {
      return new Registrar.Static<>(registry, name, value);
   }

   public static <T, V extends Block> Registrar.BlockRef<V> createBlockRef(ResourceKey<Registry<T>> registry, ResourceLocation name, Supplier<V> value) {
      return new Registrar.BlockRef<>(registry, name, value);
   }

   public static <T, V extends Item> Registrar.ItemRef<V> createItemRef(ResourceKey<Registry<T>> registry, ResourceLocation name, Supplier<V> value) {
      return new Registrar.ItemRef<>(registry, name, value);
   }

   @Nullable
   public T get(RegistryAccess registryAccess) {
      return this.get(registryAccess.registryOrThrow(this.getRegistryKey()));
   }

   @Nullable
   public T get(CommonLevelAccessor level) {
      return this.get(level.registryAccess());
   }

   @Nullable
   public T get(Registry<?> registry) {
      return (T)registry.get(this.getKey());
   }

   @Nullable
   public T orElseGet(Registry<?> registry, Supplier<T> orElse) {
      T val = this.get(registry);
      return val != null ? val : orElse.get();
   }

   public boolean isPresent(Registry<?> registry) {
      return registry.containsKey(this.getName());
   }

   @Nullable
   public <R> R map(Registry<?> registry, Function<T, R> mapFunc) {
      return this.isPresent(registry) ? mapFunc.apply(this.get(registry)) : null;
   }

   public Optional<Holder<T>> getHolder(RegistryAccess registryAccess) {
      return this.getHolder(registryAccess.registryOrThrow(this.getRegistryKey()));
   }

   public Optional<Holder<T>> getHolder(CommonLevelAccessor level) {
      return this.getHolder(level.registryAccess());
   }

   public Optional<Holder<T>> getHolder(Registry<?> registry) {
      return registry.getHolder(this.getKey());
   }

   public Optional<Reference<T>> getHolder(BootstapContext<?> bootstrapContext) {
      return this.getHolder(bootstrapContext.lookup(this.getRegistryKey()));
   }

   public Optional<Reference<T>> getHolder(HolderGetter<?> holderGetter) {
      return holderGetter.get(this.getKey());
   }

   public ResourceKey<? extends Registry<?>> getRegistryKey() {
      return this.registryKey;
   }

   public ResourceKey<T> getKey() {
      return this.key;
   }

   public ResourceLocation getName() {
      return this.getKey().location();
   }

   @Override
   public String toString() {
      return this.getClass().getSimpleName() + "[registry=" + this.getRegistryKey().location() + ", key=" + this.getKey().location() + "]";
   }

   public static class BlockRef<T extends Block> extends Registrar.Static<T> implements ItemLike {
      protected BlockRef(ResourceKey<Registry<T>> registry, ResourceLocation name, Supplier<T> value) {
         super(registry, name, value);
      }

      public Item asItem() {
         return ((Block)this.get()).asItem();
      }

      public BlockState defaultBlockState() {
         return ((Block)this.get()).defaultBlockState();
      }
   }

   @Internal
   protected static class DataGenPointer<T> extends Registrar.Pointer<T> {
      protected final Function<BootstapContext<?>, T> valueFactory;

      protected DataGenPointer(ResourceKey<Registry<T>> registry, ResourceLocation name, Function<BootstapContext<?>, T> valueFactory) {
         super(registry, name);
         this.valueFactory = valueFactory;
      }

      public void registerData(BootstapContext<?> context) {
         context.register(this.getKey(), this.valueFactory.apply(context));
      }
   }

   public static class ItemRef<T extends Item> extends Registrar.Static<T> implements ItemLike {
      protected ItemRef(ResourceKey<Registry<T>> registry, ResourceLocation name, Supplier<T> value) {
         super(registry, name, value);
      }

      public Item asItem() {
         return (Item)this.get();
      }

      public ItemStack getDefaultInstance() {
         return ((Item)this.get()).getDefaultInstance();
      }
   }

   public static class Pointer<T> extends Registrar<T> {
      protected Pointer(ResourceKey<Registry<T>> registry, ResourceLocation name) {
         super(registry, name);
      }
   }

   public static class Static<T> extends Registrar<T> implements Supplier<T> {
      protected final LazyOptional<T> valueLazy;

      protected Static(ResourceKey<Registry<T>> registry, ResourceLocation name, Supplier<T> value) {
         super(registry, name);
         this.valueLazy = LazyOptional.of(value);
      }

      public void register(RegisterEvent event) {
         if (!this.valueLazy.isPresent()) {
            event.register(this.registryKey, this.getName(), this.valueLazy);
         }
      }

      @Nullable
      @Override
      public T get() {
         return this.valueLazy.getOrThrow("Attempted to get a value from a Registrar before it was registered. " + this.getKey());
      }

      public T orElseGet(Supplier<T> orElse) {
         return this.valueLazy.orElseGet(orElse);
      }

      public boolean isPresent() {
         return this.valueLazy.isPresent();
      }

      @Nullable
      public <R> R map(Function<T, R> mapFunc) {
         return this.isPresent() ? mapFunc.apply(this.get()) : null;
      }
   }
}
