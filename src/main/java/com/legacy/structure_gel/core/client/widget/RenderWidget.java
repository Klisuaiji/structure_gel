package com.legacy.structure_gel.core.client.widget;

import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;

public class RenderWidget implements Renderable, LayoutElement, GuiEventListener {
   final Font font;
   int x;
   int y;
   int width;
   int height;
   boolean visible = true;
   boolean isHovered = false;
   @Nullable
   Tooltip tooltip;
   @Nullable
   Component label;
   int textColor;
   @Nullable
   Renderable customRender;

   private RenderWidget(
      Font font, int x, int y, int width, int height, @Nullable Tooltip tooltip, @Nullable Component label, int textColor, @Nullable Renderable customRender
   ) {
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

   public static RenderWidget.Builder builder(Font font, int x, int y, int width, int height) {
      return new RenderWidget.Builder(font, x, y, width, height);
   }

   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      if (this.visible) {
         int hoverMargin = this.label == null ? 0 : 2;
         this.isHovered = mouseX >= this.getX() - hoverMargin
            && mouseY >= this.getY() - hoverMargin
            && mouseX < this.getX() + this.getWidth() + hoverMargin
            && mouseY < this.getY() + this.getHeight() + hoverMargin;
         if (this.label != null) {
            graphics.drawString(this.font, this.label, this.x, this.y, this.textColor);
         }

         if (this.customRender != null) {
            graphics.pose().pushPose();
            this.customRender.render(graphics, mouseX, mouseY, partialTick);
            graphics.pose().popPose();
         }

         if (this.tooltip != null) {
            this.tooltip.refreshTooltipForNextRenderPass(this.isHovered, this.isFocused(), this.getRectangle());
         }
      }
   }

   public void setVisible(boolean visible) {
      this.visible = visible;
   }

   public boolean isVisible() {
      return this.visible;
   }

   @Nullable
   public Tooltip getTooltip() {
      return this.tooltip;
   }

   public void setTooltip(@Nullable Tooltip tooltip) {
      this.tooltip = tooltip;
   }

   public void setX(int pX) {
      this.x = pX;
   }

   public void setY(int pY) {
      this.y = pY;
   }

   public int getX() {
      return this.x;
   }

   public int getY() {
      return this.y;
   }

   public int getWidth() {
      return this.label == null ? this.width : this.font.width(this.label);
   }

   public int getHeight() {
      return this.label == null ? this.height : 9;
   }

   public void visitWidgets(Consumer<AbstractWidget> pConsumer) {
   }

   public void setFocused(boolean pFocused) {
   }

   public boolean isFocused() {
      return false;
   }

   public ScreenRectangle getRectangle() {
      return super.getRectangle();
   }

   public static class Builder {
      final Font font;
      final int x;
      final int y;
      final int width;
      final int height;
      @Nullable
      Tooltip tooltip;
      @Nullable
      Component label;
      int labelColor = 14737632;
      @Nullable
      Renderable customRender;

      private Builder(Font font, int x, int y, int width, int height) {
         this.font = font;
         this.x = x;
         this.y = y;
         this.width = width;
         this.height = height;
      }

      public RenderWidget.Builder setTooltip(Tooltip tooltip) {
         this.tooltip = tooltip;
         return this;
      }

      public RenderWidget.Builder setTooltip(String translationKey, ChatFormatting format) {
         return this.setTooltip(Component.translatable(translationKey).withStyle(format));
      }

      public RenderWidget.Builder setTooltip(Component tooltip) {
         return this.setTooltip(Tooltip.create(tooltip));
      }

      public RenderWidget.Builder setLabel(Component label) {
         this.label = label;
         return this;
      }

      public RenderWidget.Builder setLabel(Component label, int color) {
         this.label = label;
         this.labelColor = color;
         return this;
      }

      public RenderWidget.Builder setCustomRender(Renderable renderable) {
         this.customRender = renderable;
         return this;
      }

      public RenderWidget build() {
         return new RenderWidget(this.font, this.x, this.y, this.width, this.height, this.tooltip, this.label, this.labelColor, this.customRender);
      }
   }
}
