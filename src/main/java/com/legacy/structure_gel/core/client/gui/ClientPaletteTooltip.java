package com.legacy.structure_gel.core.client.gui;

import com.legacy.structure_gel.core.client.screen.building_tool.BuildingToolOverlay;
import com.legacy.structure_gel.core.item.building_tool.BuildingToolItem;
import com.legacy.structure_gel.core.item.util.PaletteTooltip;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.util.random.WeightedEntry.Wrapper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class ClientPaletteTooltip implements ClientTooltipComponent {
   final List<Wrapper<BlockState>> palette;

   public ClientPaletteTooltip(PaletteTooltip tooltip) {
      this.palette = tooltip.palette().unwrap().stream().sorted(BuildingToolOverlay::compare).toList();
   }

   public void renderImage(Font font, int x, int y, GuiGraphics graphics) {
      Minecraft mc = Minecraft.getInstance();
      int size = this.palette.size();
      int perRow = this.itemsPerRow();

      for (int i = 0; i < size; i++) {
         Wrapper<BlockState> wrapper = this.palette.get(i);
         int horizontalIndex = i % perRow;
         int row = i / perRow;
         int itemX = x + horizontalIndex * this.getXSpacing();
         int itemY = y + row * this.getYSpacing();
         ItemStack item = BuildingToolItem.getItemForBlock((BlockState)wrapper.getData(), mc.level);
         graphics.renderItem(item, itemX, itemY);
         if (size > 1) {
            graphics.renderItemDecorations(font, item, itemX, itemY, "x" + wrapper.getWeight().asInt());
         }
      }
   }

   public int itemsPerRow() {
      return 8;
   }

   public int getRows() {
      int size = this.palette.size();
      int perRow = this.itemsPerRow();
      return size / perRow + (size % perRow == 0 ? 0 : 1);
   }

   public int getXSpacing() {
      return 18;
   }

   public int getYSpacing() {
      return 18;
   }

   public int getHeight() {
      return this.getRows() * this.getYSpacing() + 4;
   }

   public int getWidth(Font font) {
      return this.getXSpacing() * Math.min(this.palette.size(), this.itemsPerRow());
   }
}
