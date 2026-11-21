package com.legacy.structure_gel.core.asm_hooks;

import com.legacy.structure_gel.api.block.GelPortalBlock;
import com.legacy.structure_gel.core.SGAccessor;
import com.legacy.structure_gel.core.asm_hooks.base.ASMCallback;
import com.legacy.structure_gel.core.capability.GelCapability;
import com.legacy.structure_gel.core.capability.entity.GelEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

public class EntityHooks {
   public static boolean handleGelPortal(Entity entity) {
      ASMCallback<Boolean> shouldReturn = new ASMCallback<>(false);
      GelCapability.ifPresent(entity, gelEntity -> {
         if (gelEntity.getPortal() != null) {
            if (entity.level() instanceof ServerLevel serverLevel) {
               if (entity.isInsidePortal) {
                  GelPortalBlock portal = gelEntity.getPortal();
                  int maxTime = portal.getMaxTimeInside(entity);
                  ServerLevel destWorld = serverLevel.getServer().getLevel(portal.getTeleporter(serverLevel).getOpposite());
                  if (destWorld != null && !entity.isPassenger() && entity.portalTime++ >= maxTime) {
                     serverLevel.getProfiler().push("structure_gel portal");
                     entity.portalTime = maxTime;
                     entity.setPortalCooldown();
                     entity.changeDimension(destWorld, portal.getTeleporter(destWorld));
                     serverLevel.getProfiler().pop();
                  }

                  entity.isInsidePortal = false;
                  gelEntity.setPortal(null);
               } else {
                  if (entity.portalTime > 0) {
                     entity.portalTime -= 4;
                  }

                  if (entity.portalTime < 0) {
                     entity.portalTime = 0;
                  }
               }

               SGAccessor.ENTITY_PROCESS_PORTAL_COOLDOWN.invoke(entity);
               shouldReturn.setReturnVal(true);
            }

            gelEntity.setPortal(null);
         } else if (entity.isInsidePortal) {
            GelEntity.setPortalClient(entity, null);
         }
      });
      return shouldReturn.getReturnVal();
   }
}
