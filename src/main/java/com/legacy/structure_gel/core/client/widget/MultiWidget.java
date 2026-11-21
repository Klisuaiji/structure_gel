package com.legacy.structure_gel.core.client.widget;

import com.legacy.structure_gel.api.client.gui.LayerWidgetHolder;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.narration.NarratableEntry.NarrationPriority;

public class MultiWidget implements Renderable, GuiEventListener, NarratableEntry, ContainerEventHandler, LayerWidgetHolder {
   @Nullable
   protected GuiEventListener topHovered = null;
   private final List<LayerWidgetHolder.LayerAwareWidget> layerAwareWidgets = new ArrayList<>();
   private final List<GuiEventListener> listeners = new ArrayList<>();
   private final List<Renderable> renderables = new ArrayList<>();
   private int focusIndex = -1;

   @Override
   public List<LayerWidgetHolder.LayerAwareWidget> getLayerAwareWidgets() {
      return this.layerAwareWidgets;
   }

   public void clear() {
      this.listeners.clear();
      LayerWidgetHolder.LayerAwareWidget.topHovered = null;
      this.renderables.clear();
      this.layerAwareWidgets.clear();
      this.setFocused(null);
   }

   public boolean isFocused() {
      return false;
   }

   public void setFocused(boolean isFocused) {
   }

   public void setFocused(GuiEventListener listener) {
      for (GuiEventListener l : this.listeners) {
         l.setFocused(l == listener);
      }

      if (listener != null) {
         this.focusIndex = this.listeners.indexOf(listener);
      } else {
         this.focusIndex = -1;
      }
   }

   @Nullable
   public GuiEventListener getFocused() {
      return this.focusIndex > -1 && this.focusIndex < this.listeners.size() ? this.listeners.get(this.focusIndex) : null;
   }

   public List<? extends GuiEventListener> children() {
      return this.listeners;
   }

   public boolean isDragging() {
      return false;
   }

   public void setDragging(boolean dragging) {
   }

   public <T extends GuiEventListener & NarratableEntry & Renderable> void addListener(T listener) {
      this.addListener(0, listener);
   }

   public <T extends GuiEventListener & NarratableEntry & Renderable> void addListener(int layer, T listener) {
      this.listeners.add(listener);
      this.renderables.add(listener);
      this.addLayerAwareWidget(layer, listener);
   }

   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      GuiEventListener oldTop = LayerWidgetHolder.LayerAwareWidget.topHovered;
      LayerWidgetHolder.LayerAwareWidget.topHovered = this.getTopWidget(mouseX, mouseY);

      for (Renderable renderable : this.renderables) {
         RenderSystem.enableBlend();
         renderable.render(graphics, mouseX, mouseY, partialTicks);
         RenderSystem.disableBlend();
      }

      LayerWidgetHolder.LayerAwareWidget.topHovered = oldTop;
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int clickType) {
      GuiEventListener top = this.getTopWidget(mouseX, mouseY);
      if (top != null && top.mouseClicked(mouseX, mouseY, clickType)) {
         this.focusIndex = this.listeners.indexOf(top);
         this.setFocused(top);
         return true;
      } else {
         this.setFocused(null);
         return false;
      }
   }

   @Override
   public boolean mouseReleased(double mouseX, double mouseY, int clickType) {
      GuiEventListener top = this.getTopWidget(mouseX, mouseY);
      return top != null && top.mouseReleased(mouseX, mouseY, clickType);
   }

   public boolean mouseDragged(double mouseX, double mouseY, int clickType, double toX, double toZ) {
      GuiEventListener top = this.getTopWidget(mouseX, mouseY);
      return top != null && top.mouseDragged(mouseX, mouseY, clickType, toX, toZ);
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
      GuiEventListener top = this.getTopWidget(mouseX, mouseY);
      return top != null && top.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      return this.focusIndex > -1 && this.focusIndex < this.listeners.size()
         ? this.listeners.get(this.focusIndex).keyPressed(keyCode, scanCode, modifiers)
         : false;
   }

   public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
      return this.focusIndex > -1 && this.focusIndex < this.listeners.size()
         ? this.listeners.get(this.focusIndex).keyReleased(keyCode, scanCode, modifiers)
         : false;
   }

   public boolean charTyped(char character, int modifiers) {
      return this.focusIndex > -1 && this.focusIndex < this.listeners.size() ? this.listeners.get(this.focusIndex).charTyped(character, modifiers) : false;
   }

   public boolean isMouseOver(double mouseX, double mouseY) {
      GuiEventListener top = this.getTopWidget(mouseX, mouseY);
      return top != null && top.isMouseOver(mouseX, mouseY);
   }

   public void updateNarration(NarrationElementOutput output) {
   }

   public NarrationPriority narrationPriority() {
      return NarrationPriority.NONE;
   }
}
