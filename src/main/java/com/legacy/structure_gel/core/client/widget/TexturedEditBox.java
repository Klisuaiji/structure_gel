package com.legacy.structure_gel.core.client.widget;

import com.legacy.structure_gel.api.client.gui.LayerWidgetHolder;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class TexturedEditBox extends EditBox {
   private final int u;
   private final int v;
   private final int texWidth;
   private final int texHeight;
   private final ResourceLocation texture;
   private int textColor;
   private int hoveredTextColor;
   @Nullable
   private OnTooltip<TexturedEditBox> onTooltip;
   @Nullable
   private SuggestionEditBox.SuggestionComponent<String> suggestions;

   public TexturedEditBox(Font font, int x, int y, int width, int height, int u, int v, int texWidth, int texHeight, ResourceLocation texture, Component name) {
      super(font, x, y, width, height, name);
      this.u = u;
      this.v = v;
      this.texWidth = texWidth;
      this.texHeight = texHeight;
      this.texture = texture;
      this.textColor = 14737632;
      this.hoveredTextColor = 14737632;
      this.suggestions = new SuggestionEditBox.SuggestionComponent<>(this, Collections::emptyList, Function.identity(), 0);
      this.setBordered(false);
   }

   public TexturedEditBox(Font font, int x, int y, int width, int height, int u, int v, ResourceLocation texture, Component name) {
      this(font, x, y, width, height, u, v, 256, 256, texture, name);
   }

   public void onTooltip(OnTooltip<TexturedEditBox> onTooltip) {
      this.onTooltip = onTooltip;
   }

   public void setFocused(boolean isFocused) {
      super.setFocused(isFocused);
      if (isFocused) {
         this.suggestions.update(this.getValue());
      }
   }

   public boolean isMouseOver(double mouseX, double mouseY) {
      boolean isOverText = super.isMouseOver(mouseX, mouseY);
      return !this.isFocused() || !isOverText && !this.suggestions.isMouseOver(mouseX, mouseY) ? isOverText : true;
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
      return this.isFocused() && this.suggestions.mouseScrolled(mouseX, mouseY, deltaX, deltaY) && this.isActive()
         ? true
         : super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
   }

   public boolean mouseClicked(double mouseX, double mouseY, int clickType) {
      return this.isFocused() && this.suggestions.isMouseOver(mouseX, mouseY) && this.isActive()
         ? this.suggestions.mouseClicked(mouseX, mouseY, clickType)
         : super.mouseClicked(mouseX, mouseY, clickType);
   }

   public boolean keyPressed(int keyCode, int keyB, int modifiers) {
      if (this.isActive()) {
         if (keyCode == 256 && this.isFocused()) {
            this.setFocused(false);
            return true;
         }

         if (this.suggestions.keyPressed(keyCode, keyB, modifiers)) {
            return true;
         }
      }

      return super.keyPressed(keyCode, keyB, modifiers);
   }

   public void setResponder(Consumer<String> responder) {
      super.setResponder(s -> {
         responder.accept(s);
         this.suggestions.update(this.getValue());
      });
   }

   public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      if (this.isVisible()) {
         this.renderTexture(graphics, mouseX, mouseY, partialTicks);
         if (this.isHoveredOrFocused()) {
            this.renderToolTip(graphics, mouseX, mouseY);
         }
      }

      int oldX = this.getX();
      int oldY = this.getY();
      this.setX(oldX + 4);
      this.setY(oldY + 8);
      this.setTextColor(this.isHoveredOrFocused() ? this.hoveredTextColor : this.textColor);
      super.renderWidget(graphics, mouseX, mouseY, partialTicks);
      this.setX(oldX);
      this.setY(oldY);
      if (this.isFocused() && this.isActive()) {
         this.suggestions.x = this.getX() - 1;
         this.suggestions.y = this.getY() + this.getHeight() + 1;
         this.suggestions.render(graphics, mouseX, mouseY, partialTicks);
      }
   }

   public boolean isHoveredOrFocused() {
      return super.isHoveredOrFocused() && LayerWidgetHolder.LayerAwareWidget.isTop(this);
   }

   public void renderTexture(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      RenderSystem.enableBlend();
      int h = this.height;
      graphics.blit(
         this.texture, this.getX(), this.getY(), this.u, this.isHoveredOrFocused() ? this.v + h : this.v, this.width, h, this.texWidth, this.texHeight
      );
      RenderSystem.disableBlend();
   }

   public void renderToolTip(GuiGraphics graphics, int mouseX, int mouseY) {
      if (this.onTooltip != null) {
         this.onTooltip.onTooltip(this, graphics, mouseX, mouseY);
      }
   }

   @Deprecated
   public void setTextColor(int color) {
      super.setTextColor(color);
   }

   public void setTextColor(int textColor, int hoveredTextColor) {
      super.setTextColor(textColor);
      this.textColor = textColor;
      this.hoveredTextColor = hoveredTextColor;
   }

   public void setSuggestions(Supplier<Collection<String>> suggestions) {
      this.suggestions = new SuggestionEditBox.SuggestionComponent<>(this, suggestions, Function.identity(), 10);
   }
}
