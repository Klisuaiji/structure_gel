package com.legacy.structure_gel.core.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.legacy.structure_gel.core.client.ClientProxy;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
@Mixin(Gui.class)
public class GuiMixin
{
	// Renders the portal overlay
	@Inject(at = @At("HEAD"), method = "renderPortalOverlay", cancellable = true)
	private void renderPortal(GuiGraphics guiGraphics, float alpha, CallbackInfo callback)
	{
		if (ClientProxy.lastPortal != null)
		{
			ClientProxy.lastPortal.renderPortal(guiGraphics, alpha);
			callback.cancel();
		}
	}
}
