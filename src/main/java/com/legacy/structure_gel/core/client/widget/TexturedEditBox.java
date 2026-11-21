package com.legacy.structure_gel.core.client.widget;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.legacy.structure_gel.api.client.gui.LayerWidgetHolder.LayerAwareWidget;
import com.legacy.structure_gel.core.client.widget.SuggestionEditBox.SuggestionComponent;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;

public class TexturedEditBox extends EditBox
{
	private final WidgetSprites sprite;
	private int textColor, hoveredTextColor;
	@Nullable
	private OnTooltip<TexturedEditBox> onTooltip;
	@Nullable
	private SuggestionComponent<String> suggestions;

	public TexturedEditBox(Font font, int x, int y, int width, int height, WidgetSprites sprite, Component name)
	{
		super(font, x, y, width, height, name);
		this.sprite = sprite;
		this.textColor = DEFAULT_TEXT_COLOR;
		this.hoveredTextColor = DEFAULT_TEXT_COLOR;
		this.suggestions = new SuggestionComponent<>(this, Collections::emptyList, Function.identity(), 0);
		this.setBordered(false);
	}

	public void onTooltip(OnTooltip<TexturedEditBox> onTooltip)
	{
		this.onTooltip = onTooltip;
	}

	@Override
	public void setFocused(boolean isFocused)
	{
		super.setFocused(isFocused);
		if (isFocused)
			this.suggestions.update(this.getValue());
	}

	@Override
	public boolean isMouseOver(double mouseX, double mouseY)
	{
		boolean isOverText = super.isMouseOver(mouseX, mouseY);
		if (this.isFocused() && (isOverText || this.suggestions.isMouseOver(mouseX, mouseY)))
			return true;
		return isOverText;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY)
	{
		if (this.isFocused() && this.suggestions.mouseScrolled(mouseX, mouseY, deltaX, deltaY) && this.isActive())
			return true;
		return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int clickType)
	{
		if (this.isFocused() && this.suggestions.isMouseOver(mouseX, mouseY) && this.isActive())
		{
			return this.suggestions.mouseClicked(mouseX, mouseY, clickType);
		}
		return super.mouseClicked(mouseX, mouseY, clickType);
	}

	@Override
	public boolean keyPressed(int keyCode, int keyB, int modifiers)
	{
		if (this.isActive())
		{
			if (keyCode == 256) // Escape
			{
				if (this.isFocused())
				{
					this.setFocused(false);
					return true;
				}
			}
			if (this.suggestions.keyPressed(keyCode, keyB, modifiers))
				return true;
		}
		return super.keyPressed(keyCode, keyB, modifiers);
	}

	@Override
	public void setResponder(Consumer<String> responder)
	{
		super.setResponder(s ->
		{
			responder.accept(s);
			this.suggestions.update(this.getValue());
		});
	}

	@Override
	public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks)
	{
		if (this.isVisible())
		{
			this.renderTexture(graphics, mouseX, mouseY, partialTicks);
			if (this.isHoveredOrFocused())
				this.renderToolTip(graphics, mouseX, mouseY);
		}

		int oldX = this.getX();
		int oldY = this.getY();
		this.setX(oldX + 4);
		this.setY(oldY + 8);
		this.setTextColor(this.isHoveredOrFocused() ? this.hoveredTextColor : this.textColor);
		super.renderWidget(graphics, mouseX, mouseY, partialTicks);
		this.setX(oldX);
		this.setY(oldY);

		if (this.isFocused() && this.isActive())
		{
			this.suggestions.x = this.getX() - 1;
			this.suggestions.y = this.getY() + +this.getHeight() + 1;
			this.suggestions.render(graphics, mouseX, mouseY, partialTicks);
		}
	}

	@Override
	public boolean isHoveredOrFocused()
	{
		return super.isHoveredOrFocused() && LayerAwareWidget.isTop(this);
	}

	public void renderTexture(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks)
	{
		RenderSystem.enableBlend();
		graphics.blitSprite(this.sprite.get(this.isActive(), this.isHoveredOrFocused()), this.getX(), this.getY(), this.width, this.height);
		RenderSystem.disableBlend();
	}

	public void renderToolTip(GuiGraphics graphics, int mouseX, int mouseY)
	{
		if (this.onTooltip != null)
			this.onTooltip.onTooltip(this, graphics, mouseX, mouseY);
	}

	/**
	 * @deprecated see #setTextColor(int, int)
	 */
	@Deprecated
	@Override
	public void setTextColor(int color)
	{
		super.setTextColor(color);
	}

	public void setTextColor(int textColor, int hoveredTextColor)
	{
		super.setTextColor(textColor);
		this.textColor = textColor;
		this.hoveredTextColor = hoveredTextColor;
	}

	public void setSuggestions(Supplier<Collection<String>> suggestions)
	{
		this.suggestions = new SuggestionComponent<>(this, suggestions, Function.identity(), 10);
	}
}