package com.legacy.structure_gel.core.client.widget;

import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.legacy.structure_gel.core.util.SGText;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetTooltipHolder;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;

public class RenderWidget implements Renderable, LayoutElement, GuiEventListener
{
	final Font font;
	int x, y, width, height;
	boolean visible = true, isHovered = false;
	@Nullable
	private WidgetTooltipHolder tooltip;
	@Nullable
	Component label;
	int textColor;
	@Nullable
	Renderable customRender;

	private RenderWidget(Font font, int x, int y, int width, int height, @Nullable WidgetTooltipHolder tooltip, @Nullable Component label, int textColor, @Nullable Renderable customRender)
	{
		this.font = font;
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
		this.tooltip = tooltip;
		this.label = label;
		this.textColor = textColor;
		this.customRender = customRender;
	}

	public static Builder builder(Font font, int x, int y, int width, int height)
	{
		return new Builder(font, x, y, width, height);
	}

	public static class Builder
	{
		final Font font;
		final int x, y, width, height;
		@Nullable
		WidgetTooltipHolder tooltip;
		@Nullable
		Component label;
		int labelColor = SGText.WHITE;
		@Nullable
		Renderable customRender;

		private Builder(Font font, int x, int y, int width, int height)
		{
			this.font = font;
			this.x = x;
			this.y = y;
			this.width = width;
			this.height = height;
		}

		public Builder setTooltip(Tooltip tooltip)
		{
			this.tooltip = new WidgetTooltipHolder();
			this.tooltip.set(tooltip);
			return this;
		}

		public Builder setTooltip(String translationKey, ChatFormatting format)
		{
			return this.setTooltip(Component.translatable(translationKey).withStyle(format));
		}

		public Builder setTooltip(Component tooltip)
		{
			return this.setTooltip(Tooltip.create(tooltip));
		}

		public Builder setLabel(Component label)
		{
			this.label = label;
			return this;
		}

		public Builder setLabel(Component label, int color)
		{
			this.label = label;
			this.labelColor = color;
			return this;
		}

		public Builder setCustomRender(Renderable renderable)
		{
			this.customRender = renderable;
			return this;
		}

		public RenderWidget build()
		{
			return new RenderWidget(font, x, y, width, height, tooltip, label, labelColor, customRender);
		}
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
	{
		if (this.visible)
		{
			int hoverMargin = label == null ? 0 : 2;
			this.isHovered = mouseX >= this.getX() - hoverMargin && mouseY >= this.getY() - hoverMargin && mouseX < this.getX() + this.getWidth() + hoverMargin && mouseY < this.getY() + this.getHeight() + hoverMargin;

			if (label != null)
			{
				graphics.drawString(font, label, x, y, textColor);
			}

			if (customRender != null)
			{
				graphics.pose().pushPose();
				customRender.render(graphics, mouseX, mouseY, partialTick);
				graphics.pose().popPose();
			}

			if (tooltip != null)
			{
				tooltip.refreshTooltipForNextRenderPass(this.isHovered, this.isFocused(), this.getRectangle());
			}
		}
	}

	public void setVisible(boolean visible)
	{
		this.visible = visible;
	}

	public boolean isVisible()
	{
		return visible;
	}

	@Nullable
	public Tooltip getTooltip()
	{
		return tooltip.get();
	}

	public void setTooltip(@Nullable Tooltip tooltip)
	{
		if (this.tooltip != null)
			this.tooltip.set(tooltip);
	}

	@Override
	public void setX(int pX)
	{
		x = pX;
	}

	@Override
	public void setY(int pY)
	{
		y = pY;
	}

	@Override
	public int getX()
	{
		return x;
	}

	@Override
	public int getY()
	{
		return y;
	}

	@Override
	public int getWidth()
	{
		return label == null ? width : font.width(label);
	}

	@Override
	public int getHeight()
	{
		return label == null ? height : font.lineHeight;
	}

	@Override
	public void visitWidgets(Consumer<AbstractWidget> pConsumer)
	{
	}

	@Override
	public void setFocused(boolean pFocused)
	{
		// Cannot focus because it's renderable only
	}

	@Override
	public boolean isFocused()
	{
		// Cannot focus because it's renderable only
		return false;
	}

	@Override
	public ScreenRectangle getRectangle()
	{
		return LayoutElement.super.getRectangle();
	}
}
