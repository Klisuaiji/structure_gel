package com.legacy.structure_gel.core.client.widget;

import javax.annotation.Nullable;

import com.legacy.structure_gel.api.client.gui.LayerWidgetHolder.LayerAwareWidget;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class TooltipImageButton extends Button
{
	private final ResourceLocation texture;
	public int u;
	public int v;
	private final int yOffset;
	private final int textureWidth;
	private final int textureHeight;
	@Nullable
	private OnTooltip<TooltipImageButton> onTooltip = null;
	public boolean canPress = true;

	public TooltipImageButton(int x, int y, int width, int height, int u, int v, ResourceLocation texture, Button.OnPress onPress)
	{
		this(x, y, width, height, u, v, height, texture, 256, 256, onPress);
	}

	public TooltipImageButton(int x, int y, int width, int height, int u, int v, int yOffset, ResourceLocation texture, Button.OnPress onPress)
	{
		this(x, y, width, height, u, v, yOffset, texture, 256, 256, onPress);
	}

	public TooltipImageButton(int x, int y, int width, int height, int u, int v, int yOffset, ResourceLocation texture, int textureW, int textureH, Button.OnPress onPress)
	{
		this(x, y, width, height, u, v, yOffset, texture, textureW, textureH, onPress, CommonComponents.EMPTY);
	}

	public TooltipImageButton(int x, int y, int width, int height, int u, int v, int yOffset, ResourceLocation texture, int textureW, int textureH, Button.OnPress onPress, Component narration)
	{
		super(x, y, width, height, CommonComponents.EMPTY, onPress, DEFAULT_NARRATION);
		this.textureWidth = textureW;
		this.textureHeight = textureH;
		this.u = u;
		this.v = v;
		this.yOffset = yOffset;
		this.texture = texture;
	}

	public TooltipImageButton onTooltip(OnTooltip<TooltipImageButton> onTooltip)
	{
		this.onTooltip = onTooltip;
		return this;
	}

	@Override
	public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
	{
		int i = this.v;
		if (!this.isActive())
		{
			i += this.yOffset * 2;
		}
		else if (this.isHoveredOrFocused() && this.canPress)
		{
			i += this.yOffset;
		}

		RenderSystem.enableDepthTest();
		graphics.blit(RenderType::guiTextured, this.texture, this.getX(), this.getY(), this.u, i, this.width, this.height, this.textureWidth, this.textureHeight);
		if (this.isHoveredOrFocused() && this.onTooltip != null)
			this.onTooltip.onTooltip(this, graphics, mouseX, mouseY);
	}

	@Override
	public void onPress()
	{
		if (this.canPress)
			super.onPress();
	}
	
	@Override
	public boolean isHoveredOrFocused()
	{
		return super.isHoveredOrFocused() && LayerAwareWidget.isTop(this);
	}
	
	@Override
	public String toString()
	{
		return String.format("%s", this.getClass().getName());
	}
}
