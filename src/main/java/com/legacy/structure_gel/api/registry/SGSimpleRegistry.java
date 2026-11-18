package com.legacy.structure_gel.api.registry;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;
import com.legacy.structure_gel.core.StructureGelMod;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.ModLoader;
import net.neoforged.fml.event.IModBusEvent;
import net.neoforged.neoforge.common.NeoForge;

public class SGSimpleRegistry<K, V> implements Iterable<V> {
   private final BiMap<K, V> registry = HashBiMap.create();
   private final ResourceLocation name;
   private final Supplier<V> defaultValue;
   @Nullable
   private final Supplier<Event> event;
   private boolean frozen = false;

   public SGSimpleRegistry(ResourceLocation name, Supplier<V> defaultValue, @Nullable Supplier<Event> event) {
      this.name = name;
      this.defaultValue = defaultValue;
      this.event = event;
   }

   public final void init() {
      if (!this.frozen && this.event != null) {
         StructureGelMod.LOGGER.info("Registering data for " + this.getRegistryName());
         this.preInit();
         Event event = this.event.get();
         if (event instanceof IModBusEvent) {
            ModLoader.get().postEvent((Event & IModBusEvent)event);
         } else {
            NeoForge.EVENT_BUS.post(event);
         }

         this.frozen = true;
      }
   }

   protected void preInit() {
   }

   @Nullable
   public K getKey(V value) {
      return (K)this.registry.inverse().get(value);
   }

   @Nullable
   public K getKey(V value, @Nullable K defaultKey) {
      return (K)this.registry.inverse().getOrDefault(value, defaultKey);
   }

   @Nullable
   public V get(K key) {
      return this.get(key, this.getDefaultValue());
   }

   @Nullable
   public V get(K key, @Nullable V defaultValue) {
      return (V)this.registry.getOrDefault(key, defaultValue);
   }

   public boolean containsKey(K key) {
      return this.registry.containsKey(key);
   }

   public boolean containsValue(V value) {
      return this.registry.containsValue(value);
   }

   public Set<K> getKeys() {
      return Collections.unmodifiableSet(this.registry.keySet());
   }

   public Set<V> getValues() {
      return Collections.unmodifiableSet(this.registry.values());
   }

   public BiMap<K, V> getRegistry() {
      return Maps.unmodifiableBiMap(this.registry);
   }

   public <O extends V> O register(K key, O value) throws IllegalStateException, IllegalArgumentException {
      if (this.frozen) {
         throw new IllegalStateException(
            "Attempted to register an object after registry data has been frozen. Registry = " + this.name + ", Entry = [" + key + " = " + value + "]"
         );
      } else if (this.registry.putIfAbsent(key, value) != null) {
         throw new IllegalArgumentException(
            "Attempted to register an object under an existing name. Registry = " + this.name + ", Entry = [" + key + " = " + value + "]"
         );
      } else {
         return value;
      }
   }

   protected <O extends V> O forceRegister(K key, O value) throws IllegalStateException {
      if (this.frozen) {
         throw new IllegalStateException(
            "Attempted to register an object after registry data has been frozen. Registry = " + this.name + ", Entry = [" + key + " = " + value + "]"
         );
      } else {
         this.registry.forcePut(key, value);
         return value;
      }
   }

   public ResourceLocation getRegistryName() {
      return this.name;
   }

   public V getDefaultValue() {
      return this.defaultValue.get();
   }

   public Stream<Entry<K, V>> stream() {
      return this.registry.entrySet().stream();
   }

   public void forEach(BiConsumer<K, V> action) {
      this.registry.forEach(action);
   }

   @Override
   public Iterator<V> iterator() {
      return this.registry.values().iterator();
   }

   @Override
   public String toString() {
      return "SGRegistry: " + this.name;
   }
}
