package com.legacy.structure_gel.core.client.widget;

import com.legacy.structure_gel.core.client.screen.building_tool.BuildingToolScreen;
import com.legacy.structure_gel.core.item.building_tool.ToolModeProperty;
import java.util.List;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;

public class PropertyImageButton<T extends StringRepresentable> extends TooltipImageButton {
   protected final Component name;
   protected final ToolModeProperty.SelectionProp<T> property;
   protected T currentValue;

   public PropertyImageButton(
      int x, int y, PropertyImageButton.OnPress<PropertyImageButton<T>> onPress, Component name, ToolModeProperty.SelectionProp<T> property, T currentVal
   ) {
      this(x, y, BuildingToolScreen.WIDGETS, 256, 256, onPress, name, property, currentVal);
   }

   public PropertyImageButton(
      int x,
      int y,
      ResourceLocation texture,
      int texWidth,
      int texHeight,
      PropertyImageButton.OnPress<PropertyImageButton<T>> onPress,
      Component name,
      ToolModeProperty.SelectionProp<T> property,
      T currentVal
   ) {
      super(x, y, 100, 24, 50, 0, 24, texture, texWidth, texHeight, b -> {
         if (b instanceof PropertyImageButton) {
            onPress.onPress((PropertyImageButton<T>)b);
         }
      }, property.getNameComponent());
      this.name = name;
      this.property = property;
      this.currentValue = currentVal;
   }

   @Override
   public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      super.renderWidget(graphics, mouseX, mouseY, partialTick);
      Minecraft mc = Minecraft.getInstance();
      int color = this.isHoveredOrFocused() ? 16777215 : 13487565;
      Component keyComp = Component.literal(this.name.getString() + ": ");
      int keyStartX = this.getX() + 4;
      int keyY = this.getY() + 8;
      graphics.drawString(mc.font, keyComp, keyStartX, keyY, color);
      int start = keyStartX + mc.font.width(keyComp);
      int end = this.getX() + this.getWidth() - 3;
      Component valueComp = this.property.getValueComponent(this.currentValue);
      renderLeftAlignedScrollingString(graphics, mc.font, valueComp, start, end, keyY, this.getHeight() + 10, color);
   }

   protected static void renderLeftAlignedScrollingString(
      GuiGraphics graphics, Font font, Component text, int xMinBounds, int xMaxBounds, int yPlacement, int yMargin, int color
   ) {
      int width = font.width(text);
      int xBounds = xMaxBounds - xMinBounds;
      if (width > xBounds) {
         int delta = width - xBounds;
         double d0 = Util.getMillis() / 1000.0;
         double d1 = Math.max(delta * 0.5, 3.0);
         double d2 = Math.sin((Math.PI / 2) * Math.cos((Math.PI * 2) * d0 / d1)) / 2.0 + 0.5;
         double scrollAmount = Mth.lerp(d2, 0.0, delta);
         graphics.enableScissor(xMinBounds, yPlacement - yMargin, xMaxBounds, yPlacement + yMargin);
         graphics.drawString(font, text, xMinBounds - (int)scrollAmount, yPlacement, color);
         graphics.disableScissor();
      } else {
         graphics.drawString(font, text, xMinBounds, yPlacement, color);
      }
   }

   public void cycleValue(boolean forward) {
      List<T> vals = this.property.getAllValues();
      int index = vals.indexOf(this.currentValue);
      if (forward) {
         index--;
      } else {
         index++;
      }

      int size = vals.size();
      if (index >= size) {
         index = 0;
      }

      if (index < 0) {
         index = size - 1;
      }

      this.currentValue = vals.get(index);
   }

   @Override
   public void onPress() {
      this.cycleValue(Screen.hasShiftDown());
      super.onPress();
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
      boolean up = deltaY > 0.0;
      this.cycleValue(!up);
      this.playDownSound(Minecraft.getInstance().getSoundManager());
      super.onPress();
      return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
   }

   public ToolModeProperty.SelectionProp<T> getProperty() {
      return this.property;
   }

   public T getSelectedValue() {
      return this.currentValue;
   }

   @Override
   public String toString() {
      return String.format("%s[%s=%s]", this.getClass().getName(), this.property.getKey(), this.currentValue.getSerializedName());
   }

   public interface OnPress<T extends PropertyImageButton<?>> {
      void onPress(T var1);
   }
}
