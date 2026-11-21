package com.legacy.structure_gel.api.client.gui;

import java.util.function.BooleanSupplier;

import com.legacy.structure_gel.api.registry.RegistryHelper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public class GelPortalScreen extends ReceivingLevelScreen
{
	protected final ReceivingLevelScreen.Reason reason;
	protected final ResourceKey<Block> portal;

	protected TextureAtlasSprite cachedSprite = null;

	public GelPortalScreen(BooleanSupplier levelReceived, Reason reason, ResourceKey<Block> portal)
	{
		super(levelReceived, reason);
		this.reason = reason;
		this.portal = portal;
	}

	@Override
	public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick)
	{
		// Something vanilla got passed in. Use their logic instead
		if (this.reason != ReceivingLevelScreen.Reason.OTHER)
		{
			super.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
			return;
		}

		guiGraphics.blit(0, 0, 0, guiGraphics.guiWidth(), guiGraphics.guiHeight(), this.getSprite());
	}

	protected TextureAtlasSprite getSprite()
	{
		if (this.cachedSprite != null)
			return this.cachedSprite;

		Block block = RegistryHelper.get(Minecraft.getInstance().level.registryAccess(), this.portal).orElse(Blocks.NETHER_PORTAL);
		this.cachedSprite = this.minecraft.getBlockRenderer().getBlockModelShaper().getParticleIcon(block.defaultBlockState());
		return this.cachedSprite;
	}
}
