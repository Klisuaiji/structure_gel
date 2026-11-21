package com.legacy.structure_gel.api.client.gui;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Implementaion of the Screen class with support for widget layering. Meaning
 * widgets can be above or below other widgets, overlapping, and mouse
 * interactions get the correct widget. This does not affect rendering calls, so
 * PoseStack transformations may be needed.
 */
public abstract class LayeredScreen extends Screen implements LayerWidgetHolder
{
	/**
	 * Widgets with layer awareness. The default layer is 0.
	 */
	protected final List<LayerAwareWidget> layerAwareWidgets = new ArrayList<>();

	protected LayeredScreen(Component title)
	{
		super(title);
	}

	@Override
	public List<LayerAwareWidget> getLayerAwareWidgets()
	{
		return this.layerAwareWidgets;
	}

	@Override
	protected <T extends GuiEventListener & NarratableEntry> T addWidget(T widget)
	{
		return this.addWidget(0, widget);
	}

	/**
	 * Adds the widget for the passed layer. The default layer is 0.
	 */
	protected <T extends GuiEventListener & NarratableEntry> T addWidget(int layer, T widget)
	{
		return super.addWidget(this.addLayerAwareWidget(layer, widget));
	}

	@Override
	protected <T extends GuiEventListener & Renderable & NarratableEntry> T addRenderableWidget(T widget)
	{
		return this.addRenderableWidget(0, widget);
	}

	/**
	 * Adds the widget for the passed layer. The default layer is 0.
	 */
	protected <T extends GuiEventListener & Renderable & NarratableEntry> T addRenderableWidget(int layer, T widget)
	{
		return super.addRenderableWidget(this.addLayerAwareWidget(layer, widget));
	}

	@Override
	protected void removeWidget(GuiEventListener widget)
	{
		super.removeWidget(widget);

		if (widget instanceof GuiEventListener)
			this.forLayerAware(widget, this.layerAwareWidgets::remove);
	}

	@Override
	protected void clearWidgets()
	{
		super.clearWidgets();
		LayerAwareWidget.topHovered = null;
		this.layerAwareWidgets.clear();
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
	{
		LayerAwareWidget.topHovered = this.getTopWidget(mouseX, mouseY);
		super.render(graphics, mouseX, mouseY, partialTick);
	}

	@Override
	public void onClose()
	{
		LayerAwareWidget.topHovered = null;
		super.onClose();
	}
}
