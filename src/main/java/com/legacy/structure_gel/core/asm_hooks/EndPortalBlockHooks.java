package com.legacy.structure_gel.core.asm_hooks;

import com.legacy.structure_gel.core.capability.entity.GelEntity;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

public class EndPortalBlockHooks {
   public static void onCollide(Entity entity) {
      if (FMLEnvironment.dist == Dist.CLIENT) {
         GelEntity.setPortalClient(entity, null);
      }
   }
}
