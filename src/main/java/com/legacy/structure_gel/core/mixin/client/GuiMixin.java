package com.legacy.structure_gel.core.mixin.client;

import com.legacy.structure_gel.core.asm_hooks.GuiHooks;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@OnlyIn(Dist.CLIENT)
@Mixin({Gui.class})
public class GuiMixin {
   @Inject(
      at = {@At("HEAD")},
      method = {"renderPortalOverlay"},
      cancellable = true
   )
   private void renderPortal(GuiGraphics guiGraphics, float timeInPortal, CallbackInfo callback) {
      if (GuiHooks.renderGelPortalOverlay(timeInPortal, guiGraphics.guiHeight(), guiGraphics.guiWidth())) {
         callback.cancel();
      }
   }
}
