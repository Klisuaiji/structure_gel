package com.legacy.structure_gel.core.item.util;

import com.legacy.structure_gel.core.data_components.BlockPalette;

import net.minecraft.world.inventory.tooltip.TooltipComponent;

public record PaletteTooltip(BlockPalette palette) implements TooltipComponent
{

}
