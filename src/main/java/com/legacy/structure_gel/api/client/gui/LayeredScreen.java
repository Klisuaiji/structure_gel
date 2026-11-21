package com.legacy.structure_gel.api.client.gui;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public abstract class LayeredScreen extends Screen implements LayerWidgetHolder {
   protected final List<LayerWidgetHolder.LayerAwareWidget> layerAwareWidgets = new ArrayList<>();

   protected LayeredScreen(Component title) {
      super(title);
   }

   @Override
   public List<LayerWidgetHolder.LayerAwareWidget> getLayerAwareWidgets() {
      return this.layerAwareWidgets;
   }

   protected <T extends GuiEventListener & NarratableEntry> T addWidget(T widget) {
      return this.addWidget(0, widget);
   }

   protected <T extends GuiEventListener & NarratableEntry> T addWidget(int layer, T widget) {
      return (T)super.addWidget(this.addLayerAwareWidget(layer, widget));
   }

   protected <T extends GuiEventListener & Renderable & NarratableEntry> T addRenderableWidget(T widget) {
      return this.addRenderableWidget(0, widget);
   }

   protected <T extends GuiEventListener & Renderable & NarratableEntry> T addRenderableWidget(int layer, T widget) {
      return (T)super.addRenderableWidget(this.addLayerAwareWidget(layer, widget));
   }

   protected void removeWidget(GuiEventListener widget) {
      super.removeWidget(widget);
      if (widget instanceof GuiEventListener) {
         this.forLayerAware(widget, this.layerAwareWidgets::remove);
      }
   }

   protected void clearWidgets() {
      super.clearWidgets();
      LayerWidgetHolder.LayerAwareWidget.topHovered = null;
      this.layerAwareWidgets.clear();
   }

   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      LayerWidgetHolder.LayerAwareWidget.topHovered = this.getTopWidget(mouseX, mouseY);
      super.render(graphics, mouseX, mouseY, partialTick);
   }

   public void onClose() {
      LayerWidgetHolder.LayerAwareWidget.topHovered = null;
      super.onClose();
   }
}
