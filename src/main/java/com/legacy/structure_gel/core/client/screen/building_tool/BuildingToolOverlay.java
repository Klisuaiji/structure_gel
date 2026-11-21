package com.legacy.structure_gel.core.client.screen.building_tool;

import java.util.List;

import com.legacy.structure_gel.core.client.SGSprites;
import com.legacy.structure_gel.core.item.building_tool.BuildingToolItem;
import com.legacy.structure_gel.core.item.building_tool.BuildingToolMode;
import com.legacy.structure_gel.core.util.SGText;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.datafixers.util.Pair;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.util.random.WeightedEntry.Wrapper;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class BuildingToolOverlay
{
	public static void render(GuiGraphics graphics, DeltaTracker deltaTracker)
	{
		Minecraft mc = Minecraft.getInstance();
		int screenWidth = graphics.guiWidth();
		int screenHeight = graphics.guiHeight();
		if (!mc.options.hideGui && mc.player.isCreative() && (mc.screen == null || mc.screen instanceof ChatScreen))
		{
			Pair<InteractionHand, ItemStack> tool = BuildingToolItem.getBuildingTool(mc.player);
			if (tool != null)
			{
				final ProfilerFiller profilerfiller = mc.getProfiler();
				profilerfiller.push("building_tool_overlay");
				ItemStack stack = tool.getSecond();
				Font font = mc.font;
				BuildingToolMode mode = BuildingToolItem.getMode(stack);

				Component hudComponent = mode.getHudMessage(stack);
				if (hudComponent != null)
				{
					int width = font.width(hudComponent);
					graphics.drawString(font, hudComponent, screenWidth / 2 - width / 2, screenHeight - 58, SGText.WHITE);
				}

				if (mode.hasBlockPalette())
				{
					WeightedRandomList<Wrapper<BlockState>> palette = BuildingToolItem.getPalette(stack, mc.level).blocks();
					if (!palette.isEmpty())
					{
						List<Wrapper<BlockState>> sorted = palette.unwrap().stream().sorted(BuildingToolOverlay::compare).toList();

						int xSpacing = 20;
						int ySpacing = 20;
						int size = sorted.size();
						int leftEdge = screenWidth / 2 + 126;
						int perRow = Math.max(1, (screenWidth - leftEdge) / xSpacing);
						int rows = size / perRow + (size % perRow == 0 ? 0 : 1) - 1;
						int topEdge = screenHeight - 19 - rows * ySpacing;
						for (int i = 0; i < size; i++)
						{
							Wrapper<BlockState> wrapper = sorted.get(i);
							int horizontalIndex = i % perRow;
							int row = i / perRow;
							int x = leftEdge + (horizontalIndex * xSpacing);
							int y = topEdge + (row * ySpacing);

							// Render gui slots
							RenderSystem.enableBlend();
							int tileWidth = 22;
							int borderWidth = 1;
							int innerWidth = tileWidth - (borderWidth * 2);
							boolean isFirst = horizontalIndex == 0;
							boolean isLast = horizontalIndex == perRow - 1 || i == size - 1;
							boolean isTop = row == 0;
							boolean isBottom = row == rows || i + perRow > size - 1;
							int texX = isFirst ? 0 : borderWidth;
							int texY = isTop ? 0 : borderWidth;
							int xOffset = isFirst ? 0 : 1;
							int yOffset = isTop ? 0 : 1;
							int xSize = innerWidth + (isFirst ? 1 : 0) + (isLast ? 1 : 0);
							int ySize = innerWidth + (isTop ? 1 : 0) + (isBottom ? 1 : 0);
							// Stretch, slide, pos, size
							graphics.blitSprite(RenderType::guiTextured, SGSprites.HUD_ITEM_SLOT, tileWidth, tileWidth, xOffset, yOffset, x - 3 + texX, y - 3 + texY, xSize, ySize);
							RenderSystem.disableBlend();

							// Render item with weight
							ItemStack item = BuildingToolItem.getItemForBlock(wrapper.data(), mc.level);
							graphics.renderItem(item, x, y);
							if (size > 1)
							{
								graphics.renderItemDecorations(font, item, x, y, "x" + wrapper.getWeight().asInt());
							}
						}
					}
				}

				profilerfiller.pop();
			}
		}
	}

	public static int compare(Wrapper<BlockState> w1, Wrapper<BlockState> w2)
	{
		return Integer.compare(w2.getWeight().asInt(), w1.getWeight().asInt());
	}
}
