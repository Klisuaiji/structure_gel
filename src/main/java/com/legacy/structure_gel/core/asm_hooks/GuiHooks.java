package com.legacy.structure_gel.core.asm_hooks;

import com.legacy.structure_gel.api.block.GelPortalBlock;
import com.legacy.structure_gel.core.asm_hooks.base.ASMCallback;
import com.legacy.structure_gel.core.capability.GelCapability;
import com.legacy.structure_gel.core.capability.entity.GelEntity;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

public class GuiHooks {
   public static boolean renderGelPortalOverlay(float timeInPortal, int screenHeight, int screenWidth) {
      ASMCallback<Boolean> shouldReturn = new ASMCallback<>(false);
      if (FMLEnvironment.dist == Dist.CLIENT) {
         Minecraft mc = Minecraft.getInstance();
         GelCapability.ifPresent(mc.player, gelEntity -> {
            GelPortalBlock portal = GelEntity.getPortalClient();
            if (portal != null) {
               portal.renderPortal(timeInPortal, screenHeight, screenWidth);
               shouldReturn.setReturnVal(true);
            }
         });
      }

      return shouldReturn.getReturnVal();
   }
}
