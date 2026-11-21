package com.legacy.structure_gel.api.dimension.portal;

import com.legacy.structure_gel.api.block.GelPortalBlock;
import com.legacy.structure_gel.api.block.GelPortalBlock.GelPortalSize;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;

public class GelPortalShape
{
	private final Direction.Axis axis;
	private final Direction rightDir;
	private final int numPortalBlocks;
	private final BlockPos bottomLeft;
	private final int height;
	private final int width;
	private final GelPortalFinder logic;

	public GelPortalShape(Direction.Axis axis, int portalBlocks, Direction rightDir, BlockPos bottomLeft, int width, int height, GelPortalFinder logic)
	{
		this.axis = axis;
		this.numPortalBlocks = portalBlocks;
		this.rightDir = rightDir;
		this.bottomLeft = bottomLeft;
		this.width = width;
		this.height = height;
		this.logic = logic;
	}
	
	public int numberOfPortalBlocks()
	{
		return this.numPortalBlocks;
	}

	public boolean isValid()
	{
		GelPortalSize sizeRules = logic.sizeRules();
		return this.width >= sizeRules.minWidth() && this.width <= sizeRules.maxWidth() && this.height >= sizeRules.minHeight() && this.height <= sizeRules.maxHeight();
	}

	public void createPortalBlocks(LevelAccessor level)
	{
		BlockState blockstate = this.logic.portal.defaultBlockState().setValue(GelPortalBlock.AXIS, this.axis);
		BlockPos.betweenClosed(this.bottomLeft, this.bottomLeft.relative(Direction.UP, this.height - 1).relative(this.rightDir, this.width - 1)).forEach(p -> level.setBlock(p, blockstate, 18));
	}

	public boolean isComplete()
	{
		return this.isValid() && this.numPortalBlocks == this.width * this.height;
	}
}
