package com.legacy.structure_gel.core.item.util;

import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.util.random.WeightedEntry.Wrapper;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.level.block.state.BlockState;

public record PaletteTooltip(WeightedRandomList<Wrapper<BlockState>> palette) implements TooltipComponent {
}
