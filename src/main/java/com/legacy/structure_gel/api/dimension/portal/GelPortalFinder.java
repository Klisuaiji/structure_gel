package com.legacy.structure_gel.api.dimension.portal;

import java.util.Optional;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import org.apache.commons.lang3.mutable.MutableInt;

import com.legacy.structure_gel.api.block.GelPortalBlock;
import com.legacy.structure_gel.api.block.GelPortalBlock.GelPortalSize;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class GelPortalFinder
{
	private GelPortalSize size = GelPortalSize.DEFAULT;
	final GelPortalBlock portal;
	final BlockBehaviour.StatePredicate isFrame, canReplace;

	public GelPortalFinder(GelPortalBlock portal)
	{
		this(portal, portal::isFrame, portal::canReplace);
	}

	public GelPortalFinder(GelPortalBlock portal, BlockBehaviour.StatePredicate isFrame, BlockBehaviour.StatePredicate canReplace)
	{
		this.portal = portal;
		this.isFrame = isFrame;
		this.canReplace = canReplace;
		this.size = portal.getSizeRules();
	}

	public GelPortalSize sizeRules()
	{
		return this.size;
	}
	
	public GelPortalBlock portal()
	{
		return this.portal;
	}
	
	public Optional<GelPortalShape> findEmptyPortalShape(LevelAccessor level, BlockPos bottomLeft, Direction.Axis axis)
	{
		return findPortalShape(level, bottomLeft, shape -> shape.isValid() && shape.numberOfPortalBlocks() == 0, axis);
	}

	public Optional<GelPortalShape> findPortalShape(LevelAccessor level, BlockPos bottomLeft, Predicate<GelPortalShape> predicate, Direction.Axis axis)
	{
		Optional<GelPortalShape> optional = Optional.of(findAnyShape(level, bottomLeft, axis)).filter(predicate);
		if (optional.isPresent())
		{
			return optional;
		}
		else
		{
			Direction.Axis direction$axis = axis == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
			return Optional.of(findAnyShape(level, bottomLeft, direction$axis)).filter(predicate);
		}
	}

	public GelPortalShape findAnyShape(BlockGetter level, BlockPos pos, Direction.Axis axis)
	{
		Direction direction = axis == Direction.Axis.X ? Direction.WEST : Direction.SOUTH;
		BlockPos blockpos = calculateBottomLeft(level, direction, pos);
		if (blockpos == null)
		{
			return new GelPortalShape(axis, 0, direction, pos, 0, 0, this);
		}
		else
		{
			int width = calculateWidth(level, blockpos, direction);
			if (width == 0)
			{
				return new GelPortalShape(axis, 0, direction, blockpos, 0, 0, this);
			}
			else
			{
				MutableInt mutableint = new MutableInt();
				int height = calculateHeight(level, blockpos, direction, width, mutableint);
				return new GelPortalShape(axis, mutableint.getValue(), direction, blockpos, width, height, this);
			}
		}
	}

	@Nullable
	private BlockPos calculateBottomLeft(BlockGetter level, Direction dir, BlockPos pos)
	{
		int i = Math.max(level.getMinY(), pos.getY() - size.maxHeight());

		while (pos.getY() > i && isEmpty(level.getBlockState(pos.below()), level, pos.below()))
		{
			pos = pos.below();
		}

		Direction direction = dir.getOpposite();
		int j = getDistanceUntilEdgeAboveFrame(level, pos, direction) - 1;
		return j < 0 ? null : pos.relative(direction, j);
	}

	private int calculateWidth(BlockGetter level, BlockPos pos, Direction dir)
	{
		int width = getDistanceUntilEdgeAboveFrame(level, pos, dir);
		return width >= size.minWidth() && width <= size.maxWidth() ? width : 0;
	}

	private int getDistanceUntilEdgeAboveFrame(BlockGetter level, BlockPos pos, Direction dir)
	{
		BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

		for (int i = 0; i <= size.maxWidth(); i++)
		{
			blockpos$mutableblockpos.set(pos).move(dir, i);
			BlockState blockstate = level.getBlockState(blockpos$mutableblockpos);
			if (!isEmpty(blockstate, level, blockpos$mutableblockpos))
			{
				if (this.isFrame.test(blockstate, level, blockpos$mutableblockpos))
				{
					return i;
				}
				break;
			}

			BlockState blockstate1 = level.getBlockState(blockpos$mutableblockpos.move(Direction.DOWN));
			if (!this.isFrame.test(blockstate1, level, blockpos$mutableblockpos))
			{
				break;
			}
		}

		return 0;
	}

	private int calculateHeight(BlockGetter level, BlockPos pos, Direction dir, int width, MutableInt portalCount)
	{
		BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();
		int height = getDistanceUntilTop(level, pos, dir, blockpos$mutableblockpos, width, portalCount);
		return height >= size.minHeight() && height <= size.maxHeight() && hasTopFrame(level, pos, dir, blockpos$mutableblockpos, width, height) ? height : 0;
	}

	private boolean hasTopFrame(BlockGetter level, BlockPos pos, Direction dir, BlockPos.MutableBlockPos mutPos, int width, int height)
	{
		for (int i = 0; i < width; i++)
		{
			BlockPos.MutableBlockPos blockpos$mutableblockpos = mutPos.set(pos).move(Direction.UP, height).move(dir, i);
			if (!this.isFrame.test(level.getBlockState(blockpos$mutableblockpos), level, blockpos$mutableblockpos))
			{
				return false;
			}
		}

		return true;
	}

	private int getDistanceUntilTop(BlockGetter level, BlockPos pos, Direction dir, BlockPos.MutableBlockPos mutPos, int width, MutableInt portalCount)
	{
		for (int i = 0; i < size.maxHeight(); i++)
		{
			mutPos.set(pos).move(Direction.UP, i).move(dir, -1);
			if (!this.isFrame.test(level.getBlockState(mutPos), level, mutPos))
			{
				return i;
			}

			mutPos.set(pos).move(Direction.UP, i).move(dir, width);
			if (!this.isFrame.test(level.getBlockState(mutPos), level, mutPos))
			{
				return i;
			}

			for (int j = 0; j < width; j++)
			{
				mutPos.set(pos).move(Direction.UP, i).move(dir, j);
				BlockState blockstate = level.getBlockState(mutPos);
				if (!isEmpty(blockstate, level, mutPos))
				{
					return i;
				}

				if (blockstate.is(this.portal))
				{
					portalCount.increment();
				}
			}
		}

		return size.maxHeight();
	}

	private boolean isEmpty(BlockState state, BlockGetter level, BlockPos pos)
	{
		return this.canReplace.test(state, level, pos);
	}
}
