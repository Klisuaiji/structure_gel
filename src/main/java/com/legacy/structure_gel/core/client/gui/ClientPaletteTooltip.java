package com.legacy.structure_gel.core.client.gui;

import java.util.List;

import com.legacy.structure_gel.core.client.screen.building_tool.BuildingToolOverlay;
import com.legacy.structure_gel.core.item.building_tool.BuildingToolItem;
import com.legacy.structure_gel.core.item.util.PaletteTooltip;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.util.random.WeightedEntry.Wrapper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class ClientPaletteTooltip implements ClientTooltipComponent
{
	final List<Wrapper<BlockState>> palette;

	public ClientPaletteTooltip(PaletteTooltip tooltip)
	{
		this.palette = tooltip.palette().blocks().unwrap().stream().sorted(BuildingToolOverlay::compare).toList();
	}

	@Override
	public void renderImage(Font font, int x, int y, GuiGraphics graphics)
	{
		Minecraft mc = Minecraft.getInstance();

		int size = palette.size();
		int perRow = itemsPerRow();

		for (int i = 0; i < size; i++)
		{
			Wrapper<BlockState> wrapper = palette.get(i);
			int horizontalIndex = i % perRow;
			int row = i / perRow;
			int itemX = x + horizontalIndex * this.getXSpacing();
			int itemY = y + row * this.getYSpacing();

			// Render item with weight
			ItemStack item = BuildingToolItem.getItemForBlock(wrapper.data(), mc.level);
			graphics.renderItem(item, itemX, itemY);
			if (size > 1)
			{
				graphics.renderItemDecorations(font, item, itemX, itemY, "x" + wrapper.getWeight().asInt());
			}
		}
	}

	public int itemsPerRow()
	{
		return 8;
	}

	public int getRows()
	{
		int size = palette.size();
		int perRow = itemsPerRow();
		return size / perRow + (size % perRow == 0 ? 0 : 1);
	}

	public int getXSpacing()
	{
		return 18;
	}

	public int getYSpacing()
	{
		return 18;
	}

	@Override
	public int getHeight()
	{
		return getRows() * getYSpacing() + 4;
	}

	@Override
	public int getWidth(Font font)
	{
		return getXSpacing() * Math.min(palette.size(), itemsPerRow());
	}

}
