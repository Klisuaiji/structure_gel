package com.legacy.structure_gel.core.client.widget;

import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.legacy.structure_gel.api.client.gui.LayerWidgetHolder.LayerAwareWidget;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public class TooltipSpriteButton extends Button
{
	@Nullable
	private OnTooltip<TooltipSpriteButton> onTooltip = null;
	private final Supplier<WidgetSprites> sprites;

	public TooltipSpriteButton(int x, int y, int width, int height, WidgetSprites sprites, Button.OnPress onPress)
	{
		this(x, y, width, height, () -> sprites, onPress, CommonComponents.EMPTY);
	}
	
	public TooltipSpriteButton(int x, int y, int width, int height, Supplier<WidgetSprites> sprites, Button.OnPress onPress)
	{
		this(x, y, width, height, sprites, onPress, CommonComponents.EMPTY);
	}

	public TooltipSpriteButton(int x, int y, int width, int height, WidgetSprites sprites, Button.OnPress onPress, Component message)
	{
		this(x, y, width, height, () -> sprites, onPress, message);
	}
	
	public TooltipSpriteButton(int x, int y, int width, int height, Supplier<WidgetSprites> sprites, Button.OnPress onPress, Component message)
	{
		super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
		this.sprites = sprites;
	}

	public TooltipSpriteButton onTooltip(OnTooltip<TooltipSpriteButton> onTooltip)
	{
		this.onTooltip = onTooltip;
		return this;
	}
	
	@Override
	public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
	{
		RenderSystem.enableDepthTest();
		graphics.blitSprite(this.sprites.get().get(this.isActive(), this.isHoveredOrFocused()), this.getX(), this.getY(), this.width, this.height);
		if (this.isHoveredOrFocused() && this.onTooltip != null)
			this.onTooltip.onTooltip(this, graphics, mouseX, mouseY);
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
