package com.legacy.structure_gel.api.config;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.util.function.Function;
import java.util.function.Supplier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.config.ModConfigEvent.Loading;
import net.neoforged.fml.event.config.ModConfigEvent.Reloading;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;

public class ConfigValueWrapper<O, P> implements Supplier<P> {
   private static final Multimap<String, ConfigValueWrapper<?, ?>> LISTENERS = HashMultimap.create();
   private final ConfigValue<O> configValue;
   private final Function<O, P> parseFunction;
   private P cached = (P)null;
   private boolean resolved = false;

   public static <O, P> ConfigValueWrapper<O, P> create(ConfigValue<O> configValue, Function<O, P> parseFunction, IEventBus modEventBus, String modID) {
      return new ConfigValueWrapper<>(configValue, parseFunction, modEventBus, modID);
   }

   public static <O> ConfigValueWrapper<O, O> create(ConfigValue<O> configValue, IEventBus modEventBus, String modID) {
      return create(configValue, Function.identity(), modEventBus, modID);
   }

   private ConfigValueWrapper(ConfigValue<O> configValue, Function<O, P> parseFunction, IEventBus modEventBus, String modID) {
      this.configValue = configValue;
      this.parseFunction = parseFunction;
      LISTENERS.put(modID, this);
      if (!LISTENERS.containsKey(modID)) {
         modEventBus.register(ConfigValueWrapper.class);
      }
   }

   @Override
   public P get() {
      if (!this.resolved) {
         this.cached = this.parseFunction.apply((O)this.configValue.get());
         this.resolved = true;
      }

      return this.cached;
   }

   public P getDefault() {
      return this.parseFunction.apply((O)this.configValue.getDefault());
   }

   public ConfigValue<O> getConfigValue() {
      return this.configValue;
   }

   public boolean isResolved() {
      return this.resolved;
   }

   protected void clearCache() {
      this.cached = null;
      this.resolved = false;
   }

   @SubscribeEvent
   protected static void onConfigLoad(Loading event) {
      LISTENERS.get(event.getConfig().getModId()).forEach(ConfigValueWrapper::clearCache);
   }

   @SubscribeEvent
   protected static void onConfigReload(Reloading event) {
      LISTENERS.get(event.getConfig().getModId()).forEach(ConfigValueWrapper::clearCache);
   }
}
