package com.legacy.structure_gel.core.item.building_tool.modes;

import com.legacy.structure_gel.core.item.building_tool.BuildingToolMode;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class NoneTool extends BuildingToolMode
{
	public NoneTool(String name, int modelIndex)
	{
		super(name, modelIndex);
	}
	
	@Override
	public boolean isNone()
	{
		return true;
	}
	
	@Override
	public void onRightClickBlock(Level level, Player player, BlockPos clickedPos, ItemStack stack, Direction clickedFace)
	{
	}

	@Override
	public boolean hasBlockPalette()
	{
		return false;
	}

	@Override
	public Object[] getDescArgs()
	{
		return new Object[0];
	}
}
