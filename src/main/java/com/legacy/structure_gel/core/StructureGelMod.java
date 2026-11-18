package com.legacy.structure_gel.core;

import com.legacy.structure_gel.core.client.ClientProxy;
import com.legacy.structure_gel.core.util.Internal;
import com.legacy.structure_gel.core.util.LoggerWrapper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig.Type;
import net.neoforged.fml.loading.FMLEnvironment;

@Mod("structure_gel")
@Internal
public class StructureGelMod {
   public static final String MODID = "structure_gel";
   public static final LoggerWrapper LOGGER = new LoggerWrapper("structure_gel");
   public static SGProxy proxy = new SGProxy();
   public static final boolean IS_IDE = isRunningFromIDE();

   public StructureGelMod(IEventBus modBus) {
      ModLoadingContext.get().registerConfig(Type.COMMON, SGConfig.COMMON_SPEC);
      if (FMLEnvironment.dist == Dist.CLIENT) {
         proxy = new ClientProxy();
         ModLoadingContext.get().registerConfig(Type.CLIENT, SGConfig.CLIENT_SPEC);
      }

      SGAccessor.init();
   }

   public static ResourceLocation locate(String key) {
      return new ResourceLocation("structure_gel", key);
   }

   private static boolean isRunningFromIDE() {
      String p = System.getProperty(String.format("%s.iside", "structure_gel"));
      return Boolean.parseBoolean(p);
   }
}
