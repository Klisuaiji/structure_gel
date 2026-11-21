package com.legacy.structure_gel.core.client.widget;

import com.legacy.structure_gel.core.client.screen.building_tool.BuildingToolScreen;
import com.legacy.structure_gel.core.item.building_tool.ToolModeProperty;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class PanelSelectionButton<T extends ToolModeProperty.IPanelSelection> extends PropertyImageButton<T> {
   private final PanelSelectionButton<T>.Panel panel;

   public PanelSelectionButton(
      int x, int y, PropertyImageButton.OnPress<PropertyImageButton<T>> onPress, Component name, ToolModeProperty.SelectionProp<T> property, T currentVal
   ) {
      this(x, y, BuildingToolScreen.WIDGETS, 256, 256, onPress, name, property, currentVal);
   }

   public PanelSelectionButton(
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
      super(x, y, texture, texWidth, texHeight, onPress, name, property, currentVal);
      this.panel = new PanelSelectionButton.Panel(x + 10, y + 25, Component.literal("selection_panel"), property.getAllValues(), currentVal);
   }

   public PanelSelectionButton<T>.Panel getPanel() {
      return this.panel;
   }

   private void setValue(int index) {
      List<T> vals = this.property.getAllValues();
      index = Mth.clamp(index, 0, vals.size() - 1);
      this.currentValue = vals.get(index);
      this.onPress.onPress(this);
   }

   @Override
   public boolean isHoveredOrFocused() {
      return super.isHoveredOrFocused() || this.panel.visible;
   }

   @Override
   public void onPress() {
      this.panel.visible = !this.panel.visible;
   }

   public class Panel extends AbstractWidget {
      static final int BUTTON_SIZE = 20;
      static final int BUTTON_PADDING = 1;
      static final int BUTTON_COLUMNS = 4;
      final List<T> values;
      final ResourceLocation[] textures;
      final ResourceLocation backgroundTexture;
      final int backgroundWidth;
      final int backgroundHeight;

      public Panel(int x, int y, Component name, List<T> values, ToolModeProperty.IPanelSelection panelSelectionProp) {
         super(x, y, 20, 20, name);
         this.values = values;
         this.textures = new ResourceLocation[values.size()];

         for (int i = 0; i < values.size(); i++) {
            this.textures[i] = panelSelectionProp.getTextureFolder().withSuffix("/" + values.get(i).getSerializedName() + ".png");
         }

         this.backgroundTexture = panelSelectionProp.getSpriteFolder().withSuffix("/background");
         int s = 20;
         this.width = 4 * s;
         this.height = (Math.max(1, values.size() / 4) + (values.size() % 4 > 0 ? 1 : 0)) * s;
         this.backgroundWidth = 4 * ++s;
         this.backgroundHeight = (Math.max(1, values.size() / 4) + (values.size() % 4 > 0 ? 1 : 0)) * s;
         this.visible = false;
      }

      protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
         PoseStack poseStack = graphics.pose();
         poseStack.pushPose();
         poseStack.translate(0.0F, 0.0F, 10.0F);
         int texWidth = 21;
         graphics.blitSprite(this.backgroundTexture, this.getX() - 5, this.getY() - 5, this.backgroundWidth + 9, this.backgroundHeight + 9);

         for (int i = 0; i < this.values.size(); i++) {
            int x = this.getX() + i % 4 * texWidth;
            int y = this.getY() + i / 4 * texWidth;
            int buttonW = 40;
            boolean isHovered = mouseX >= x && mouseX <= x + 20 && mouseY >= y && mouseY <= y + 20;
            graphics.blit(this.textures[i], x, y, 0.0F, isHovered ? 20.0F : 0.0F, 20, 20, buttonW, buttonW);
            if (isHovered) {
               graphics.renderTooltip(Minecraft.getInstance().font, PanelSelectionButton.this.property.getValueComponent(this.values.get(i)), mouseX, mouseY);
            }
         }

         poseStack.popPose();
      }

      protected void updateWidgetNarration(NarrationElementOutput narration) {
      }

      public void onClick(double mouseX, double mouseY) {
         int texWidth = 21;
         int column = ((int)mouseX - this.getX()) / texWidth;
         int row = ((int)mouseY - this.getY()) / texWidth;
         int index = row * 4 + column;
         PanelSelectionButton.this.setValue(index);
         this.visible = false;
      }
   }
}
