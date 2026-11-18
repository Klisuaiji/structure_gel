package com.legacy.structure_gel.core.client.widget;

import com.legacy.structure_gel.api.client.gui.LayerWidgetHolder;
import com.legacy.structure_gel.core.client.screen.building_tool.BuildingToolScreen;
import com.legacy.structure_gel.core.item.building_tool.ToolModeProperty;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

public class PropertyEditBox<T> extends TexturedEditBox {
   private final ToolModeProperty<T> property;
   private final String prefix;

   public PropertyEditBox(Font font, int x, int y, ToolModeProperty<T> property, String prefix, T currentVal) {
      super(font, x, y, 100, 24, 50, 0, BuildingToolScreen.WIDGETS, property.getNameComponent());
      this.property = property;
      this.prefix = prefix;
      this.setValue(property.write(currentVal));
      this.setTextColor(13487565, 16777215);
      this.setMaxLength(100);
   }

   public PropertyEditBox(Font font, int x, int y, ToolModeProperty<T> property, T currentVal) {
      this(font, x, y, property, property.getNameComponent().getString(), currentVal);
   }

   @Override
   public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      String oldValue = this.getValue();
      int color = this.property.canRead(oldValue) ? 14737632 : 14703708;
      this.setTextColor(color, color);
      this.setValue(this.prefix + ": " + oldValue);
      super.renderWidget(graphics, mouseX, mouseY, partialTicks);
      this.setValue(oldValue);
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
      if (this.property instanceof ToolModeProperty.NumberProp numProp) {
         boolean up = deltaY > 0.0;
         Number val = numProp.read(this.getValue());
         if (numProp.getNumberClass() == Integer.class) {
            int scale = Screen.hasControlDown() ? 5 : 1;
            Number newVal = numProp.clamp(val.intValue() + (up ? scale : -scale));
            if (numProp.isValid(newVal)) {
               this.setValue(numProp.write(newVal));
               return true;
            }
         } else if (numProp.getNumberClass() == Double.class) {
            double scale = Screen.hasControlDown() ? 0.05 : 0.01;
            Number newVal = numProp.clamp(val.doubleValue() + (up ? scale : -scale));
            if (numProp.isValid(newVal.doubleValue())) {
               this.setValue(numProp.write(newVal));
               return true;
            }
         }
      }

      return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
   }

   public ToolModeProperty<T> getProperty() {
      return this.property;
   }

   public void setValue(T val) {
      super.setValue(this.property.write(val));
   }

   @Override
   public boolean isHoveredOrFocused() {
      return super.isHoveredOrFocused() && LayerWidgetHolder.LayerAwareWidget.isTop(this);
   }

   public String toString() {
      return String.format("%s[%s]", this.getClass().getName(), this.getValue());
   }
}
