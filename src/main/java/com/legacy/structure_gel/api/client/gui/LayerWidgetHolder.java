package com.legacy.structure_gel.api.client.gui;

import com.legacy.structure_gel.core.client.widget.MultiWidget;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;

public interface LayerWidgetHolder extends ContainerEventHandler {
   List<LayerWidgetHolder.LayerAwareWidget> getLayerAwareWidgets();

   @Nullable
   default LayerWidgetHolder.LayerAwareWidget getLayerAwareWidget(@Nullable GuiEventListener widget) {
      if (widget != null) {
         for (LayerWidgetHolder.LayerAwareWidget v : this.getLayerAwareWidgets()) {
            if (v.widget == widget) {
               return v;
            }
         }
      }

      return null;
   }

   default void forLayerAware(@Nullable GuiEventListener widget, Consumer<LayerWidgetHolder.LayerAwareWidget> action) {
      LayerWidgetHolder.LayerAwareWidget layerAware = this.getLayerAwareWidget(widget);
      if (layerAware != null) {
         action.accept(layerAware);
      }
   }

   default <T extends GuiEventListener & NarratableEntry> T addLayerAwareWidget(int layer, T widget) {
      if (!(widget instanceof MultiWidget)) {
         this.getLayerAwareWidgets().add(new LayerWidgetHolder.LayerAwareWidget(layer, widget));
      }

      return widget;
   }

   default void changeWidgetLayer(GuiEventListener widget, int newLayer) {
      this.forLayerAware(widget, l -> l.layer = newLayer);
   }

   @Nullable
   default GuiEventListener getTopWidget(double mouseX, double mouseY) {
      LayerWidgetHolder.LayerAwareWidget top = null;

      for (LayerWidgetHolder.LayerAwareWidget layerAware : this.getLayerAwareWidgets()) {
         if (layerAware.widget.isMouseOver(mouseX, mouseY) && (top == null || layerAware.layer > top.layer)) {
            top = layerAware;
         }
      }

      return top == null ? null : top.widget;
   }

   default Optional<GuiEventListener> getChildAt(double mouseX, double mouseY) {
      GuiEventListener top = this.getTopWidget(mouseX, mouseY);
      return top != null ? Optional.of(top) : super.getChildAt(mouseX, mouseY);
   }

   default boolean mouseClicked(double mouseX, double mouseY, int click) {
      GuiEventListener top = this.getTopWidget(mouseX, mouseY);
      if (top != null && top.mouseClicked(mouseX, mouseY, click)) {
         this.setFocused(top);
         if (click == 0) {
            this.setDragging(true);
         }

         return true;
      } else {
         return super.mouseClicked(mouseX, mouseY, click);
      }
   }

   default boolean mouseReleased(double mouseX, double mouseY, int click) {
      return super.mouseReleased(mouseX, mouseY, click);
   }

   default boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
      return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
   }

   default void mouseMoved(double mouseX, double mouseY) {
      GuiEventListener top = this.getTopWidget(mouseX, mouseY);
      if (top != null) {
         top.mouseMoved(mouseX, mouseY);
      }
   }

   public static class LayerAwareWidget {
      @Nullable
      public static GuiEventListener topHovered = null;
      protected int layer;
      public final GuiEventListener widget;

      public LayerAwareWidget(int layer, GuiEventListener widget) {
         this.layer = layer;
         this.widget = widget;
      }

      public static boolean isTop(GuiEventListener widget) {
         return topHovered == widget;
      }
   }
}
