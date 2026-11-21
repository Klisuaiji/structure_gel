package com.legacy.structure_gel.api.client.gui;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.legacy.structure_gel.core.client.widget.MultiWidget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;

public interface LayerWidgetHolder extends ContainerEventHandler
{
	List<LayerAwareWidget> getLayerAwareWidgets();

	/**
	 * Obtains the layer aware widget that represents the widget passed, or null if
	 * one doesn't exist
	 */
	@Nullable
	default LayerAwareWidget getLayerAwareWidget(@Nullable GuiEventListener widget)
	{
		if (widget != null)
			for (var v : this.getLayerAwareWidgets())
				if (v.widget == widget)
					return v;
		return null;
	}

	/**
	 * Runs the passed action for the widget, if it exists
	 */
	default void forLayerAware(@Nullable GuiEventListener widget, Consumer<LayerAwareWidget> action)
	{
		var layerAware = this.getLayerAwareWidget(widget);
		if (layerAware != null)
			action.accept(layerAware);
	}

	default <T extends GuiEventListener & NarratableEntry> T addLayerAwareWidget(int layer, T widget)
	{
		if (!(widget instanceof MultiWidget))
			this.getLayerAwareWidgets().add(new LayerAwareWidget(layer, widget));
		return widget;
	}

	/**
	 * Changes the layer of the passed widget to the new layer passed, if one exists
	 */
	default void changeWidgetLayer(GuiEventListener widget, int newLayer)
	{
		this.forLayerAware(widget, l -> l.layer = newLayer);
	}

	@Nullable
	default GuiEventListener getTopWidget(double mouseX, double mouseY)
	{
		LayerAwareWidget top = null;
		for (var layerAware : this.getLayerAwareWidgets())
		{
			if (layerAware.widget.isMouseOver(mouseX, mouseY) && (top == null || layerAware.layer > top.layer))
			{
				top = layerAware;
			}
		}
		return top == null ? null : top.widget;
	}

	@Override
	default Optional<GuiEventListener> getChildAt(double mouseX, double mouseY)
	{
		GuiEventListener top = this.getTopWidget(mouseX, mouseY);
		if (top != null)
			return Optional.of(top);
		return ContainerEventHandler.super.getChildAt(mouseX, mouseY);
	}

	// Get the top widget, if present. Default to super
	@Override
	default boolean mouseClicked(double mouseX, double mouseY, int click)
	{
		GuiEventListener top = this.getTopWidget(mouseX, mouseY);
		if (top != null)
		{
			if (top.mouseClicked(mouseX, mouseY, click))
			{
				this.setFocused(top);
				if (click == 0)
				{
					this.setDragging(true);
				}
				return true;
			}
		}
		return ContainerEventHandler.super.mouseClicked(mouseX, mouseY, click);
	}

	// This calls getChildAt to get it's widget
	@Override
	default boolean mouseReleased(double mouseX, double mouseY, int click)
	{
		return ContainerEventHandler.super.mouseReleased(mouseX, mouseY, click);
	}

	// This calls getChildAt to get it's widget
	@Override
	default boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY)
	{
		return ContainerEventHandler.super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
	}

	@Override
	default void mouseMoved(double mouseX, double mouseY)
	{
		GuiEventListener top = this.getTopWidget(mouseX, mouseY);
		if (top != null)
		{
			top.mouseMoved(mouseX, mouseY);
		}
	}

	public static class LayerAwareWidget
	{
		/**
		 * The widget currently on top where the mouse is positioned
		 */
		@Nullable
		public static GuiEventListener topHovered = null;

		protected int layer;
		public final GuiEventListener widget;

		public LayerAwareWidget(int layer, GuiEventListener widget)
		{
			this.layer = layer;
			this.widget = widget;
		}

		/**
		 * @return True if the widget passed is the top most widget
		 */
		public static boolean isTop(GuiEventListener widget)
		{
			if (Minecraft.getInstance().screen instanceof LayerWidgetHolder)
				return topHovered == widget;
			return true;
		}
	}
}