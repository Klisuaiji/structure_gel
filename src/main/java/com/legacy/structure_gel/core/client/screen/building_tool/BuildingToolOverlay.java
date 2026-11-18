package com.legacy.structure_gel.core.client.screen.building_tool;

import com.legacy.structure_gel.core.StructureGelMod;
import com.legacy.structure_gel.core.item.building_tool.BuildingToolItem;
import com.legacy.structure_gel.core.item.building_tool.BuildingToolMode;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.datafixers.util.Pair;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.util.random.WeightedEntry.Wrapper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.gui.overlay.ExtendedGui;

public class BuildingToolOverlay {
   private static final ResourceLocation TEXTURE = StructureGelMod.locate("textures/gui/building_tool/overlay.png");

   public static void render(ExtendedGui gui, GuiGraphics graphics, float partialTicks, int screenWidth, int screenHeight) {
      Minecraft mc = Minecraft.getInstance();
      if (!mc.options.hideGui && mc.player.isCreative() && (mc.screen == null || mc.screen instanceof ChatScreen)) {
         Pair<InteractionHand, ItemStack> tool = BuildingToolItem.getBuildingTool(mc.player);
         if (tool != null) {
            mc.getProfiler().push("building_tool_overlay");
            ItemStack stack = (ItemStack)tool.getSecond();
            Font font = gui.getFont();
            BuildingToolMode mode = BuildingToolItem.getMode(stack);
            Component hudComponent = mode.getHudMessage(stack);
            if (hudComponent != null) {
               int width = font.width(hudComponent);
               graphics.drawString(font, hudComponent, screenWidth / 2 - width / 2, screenHeight - 58, 14737632);
            }

            if (mode.hasBlockPalette()) {
               WeightedRandomList<Wrapper<BlockState>> palette = BuildingToolItem.getPalette(stack, mc.level);
               if (!palette.isEmpty()) {
                  List<Wrapper<BlockState>> sorted = palette.unwrap().stream().sorted(BuildingToolOverlay::compare).toList();
                  int xSpacing = 20;
                  int ySpacing = 20;
                  int size = sorted.size();
                  int leftEdge = screenWidth / 2 + 126;
                  int perRow = Math.max(1, (screenWidth - leftEdge) / xSpacing);
                  int rows = size / perRow + (size % perRow == 0 ? 0 : 1) - 1;
                  int topEdge = screenHeight - 19 - rows * ySpacing;

                  for (int i = 0; i < size; i++) {
                     Wrapper<BlockState> wrapper = sorted.get(i);
                     int horizontalIndex = i % perRow;
                     int row = i / perRow;
                     int x = leftEdge + horizontalIndex * xSpacing;
                     int y = topEdge + row * ySpacing;
                     RenderSystem.enableBlend();
                     int tileWidth = 22;
                     int borderWidth = 1;
                     boolean isFirst = horizontalIndex == 0;
                     boolean isLast = horizontalIndex == perRow - 1 || i == size - 1;
                     boolean isTop = row == 0;
                     boolean isBottom = row == rows || i + perRow > size - 1;
                     int texX = isFirst ? 0 : borderWidth;
                     int texY = isTop ? 0 : borderWidth;
                     int texU = tileWidth - texX - (isLast ? 0 : borderWidth);
                     int texV = tileWidth - texY - (isBottom ? 0 : borderWidth);
                     graphics.blit(TEXTURE, x - 3 + texX, y - 3 + texY, texX, texY, texU, texV);
                     RenderSystem.disableBlend();
                     ItemStack item = BuildingToolItem.getItemForBlock((BlockState)wrapper.getData(), mc.level);
                     graphics.renderItem(item, x, y);
                     if (size > 1) {
                        graphics.renderItemDecorations(font, item, x, y, "x" + wrapper.getWeight().asInt());
                     }
                  }
               }
            }

            mc.getProfiler().pop();
         }
      }
   }

   public static int compare(Wrapper<BlockState> w1, Wrapper<BlockState> w2) {
      return Integer.compare(w2.getWeight().asInt(), w1.getWeight().asInt());
   }
}
