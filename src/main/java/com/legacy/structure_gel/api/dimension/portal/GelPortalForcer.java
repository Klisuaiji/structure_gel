package com.legacy.structure_gel.api.dimension.portal;

import java.util.Comparator;
import java.util.Optional;
import java.util.function.UnaryOperator;

import com.legacy.structure_gel.api.block.GelPortalBlock;
import com.legacy.structure_gel.api.block.GelPortalBlock.GelPortalSize;

import net.minecraft.BlockUtil;
import net.minecraft.BlockUtil.FoundRectangle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.levelgen.Heightmap;

public class GelPortalForcer
{
	public final ServerLevel level;

	public final ResourceKey<PoiType> portalPoi;
	public final BlockState frameBlock;
	public final GelPortalBlock portalBlock;
	public final PortalCreator portalCreator;

	public GelPortalForcer(ServerLevel level, GelPortalBlock portal)
	{
		this(level, portal, portal.getPortalPoi(), portal.getFrameBlock(), portal.getPortalCreator());
	}

	public GelPortalForcer(ServerLevel level, GelPortalBlock portal, ResourceKey<PoiType> portalPoi, BlockState frameBlock, PortalCreator portalCreator)
	{
		this.level = level;
		this.portalBlock = portal;
		this.portalPoi = portalPoi;
		this.frameBlock = frameBlock;
		this.portalCreator = portalCreator;
	}

	public Optional<BlockPos> findClosestPortalPosition(BlockPos exitPos, int searchRadius, WorldBorder worldBorder)
	{
		PoiManager poiManager = this.level.getPoiManager();
		poiManager.ensureLoadedAndValid(this.level, exitPos, searchRadius);
		return poiManager.getInSquare(poi -> poi.is(this.portalPoi), exitPos, searchRadius, PoiManager.Occupancy.ANY).map(PoiRecord::getPos).filter(worldBorder::isWithinBounds).filter(p -> this.level.getBlockState(p).hasProperty(GelPortalBlock.AXIS)).min(Comparator.<BlockPos>comparingDouble(p -> p.distSqr(exitPos)).thenComparingInt(Vec3i::getY));
	}

	public Optional<BlockUtil.FoundRectangle> createPortal(GelPortalForcer portalForcer, BlockPos pos, Direction.Axis axis)
	{
		return this.portalCreator.create(this, pos, axis);
	}

	/**
	 * Used to create a portal and get it's location.
	 *
	 * @author Silver_David
	 */
	@FunctionalInterface
	public interface PortalCreator
	{
		public static final PortalCreator NETHER_LOGIC = new NetherPortalCreator(70, false);
		public static final PortalCreator SURFACE_BIASED = new NetherPortalCreator(70, true);

		Optional<BlockUtil.FoundRectangle> create(GelPortalForcer portalForcer, BlockPos startPos, Direction.Axis enterAxis);

		public static class NetherPortalCreator implements PortalCreator
		{
			protected final int defaultHeight;
			protected final boolean alwaysOnTopBlock;

			public NetherPortalCreator(int defaultHeight, boolean alwaysOnTopBlock)
			{
				this.defaultHeight = defaultHeight;
				this.alwaysOnTopBlock = alwaysOnTopBlock;
			}

			@Override
			public Optional<BlockUtil.FoundRectangle> create(GelPortalForcer portalForcer, BlockPos pos, Direction.Axis axis)
			{
				Direction direction = Direction.get(Direction.AxisDirection.POSITIVE, axis);
				ServerLevel level = portalForcer.level;

				FoundPortalSpot found = this.findPosition(portalForcer, pos, axis, direction, level);
				return this.createPortal(portalForcer, pos, found.foundPos, found.dist, axis, direction, level, UnaryOperator.identity());
			}

			protected FoundPortalSpot findPosition(GelPortalForcer portalForcer, BlockPos pos, Direction.Axis axis, Direction direction, ServerLevel level)
			{
				double dist = -1.0;
				BlockPos foundPos = null;
				double alternativeDist = -1.0;
				BlockPos alternativeFoundPos = null;
				WorldBorder worldBorder = level.getWorldBorder();
				int maxHeight = Math.min(level.getMaxBuildHeight(), level.getMinBuildHeight() + level.getLogicalHeight() - 1);
				int portalWidth = 1;
				// This only gets used to store values instead of making a new BlockPos
				BlockPos.MutableBlockPos mutablePos = pos.mutable();
				GelPortalSize size = portalForcer.portalBlock.getSizeRules();
				int minWidth = size.minWidth(), minHeight = size.minHeight();
				
				search: for (BlockPos.MutableBlockPos searchPos : BlockPos.spiralAround(pos, 16, Direction.EAST, Direction.SOUTH))
				{
					int surfaceHeight = Math.min(maxHeight, level.getHeight(Heightmap.Types.MOTION_BLOCKING, searchPos.getX(), searchPos.getZ()));
					if (worldBorder.isWithinBounds(searchPos) && worldBorder.isWithinBounds(searchPos.move(direction, portalWidth)))
					{
						searchPos.move(direction.getOpposite(), portalWidth);

						for (int h = surfaceHeight; h >= level.getMinBuildHeight(); h--)
						{
							searchPos.setY(h);
							if (this.canPortalReplaceBlock(level, searchPos))
							{
								int hCopy = h;

								while (h > level.getMinBuildHeight() && (this.canPortalReplaceBlock(level, searchPos.move(Direction.DOWN)) || this.shouldIgnoreForHeight(level, searchPos) /*seachPos already gets moved by the first check*/))
								{
									h--;
								}

								if (h + 4 <= maxHeight)
								{
									int hDelta = hCopy - h;
									if (hDelta <= 0 || hDelta >= 3)
									{
										searchPos.setY(h);
										if (this.canHostFrame(level, searchPos, mutablePos, direction, 0, minWidth, minHeight))
										{
											// If the portal can fit here, store the search distance and position
											double distToStart = pos.distSqr(searchPos);
											if (this.canHostFrame(level, searchPos, mutablePos, direction, -1, minWidth, minHeight) && this.canHostFrame(level, searchPos, mutablePos, direction, 1, minWidth, minHeight) && (dist == -1.0 || dist > distToStart))
											{
												dist = distToStart;
												foundPos = searchPos.immutable();
												if (this.alwaysOnTopBlock)
												{
													break search;
												}
											}

											if (dist == -1.0 && (alternativeDist == -1.0 || alternativeDist > distToStart))
											{
												// Tbh not sure how these are used. Didn't spend enough time reading the stuff above
												alternativeDist = distToStart;
												alternativeFoundPos = searchPos.immutable();
											}
										}
									}
								}
							}
						}
					}
				}

				if (dist == -1.0 && alternativeDist != -1.0)
				{
					foundPos = alternativeFoundPos;
					dist = alternativeDist;
				}

				return new FoundPortalSpot(foundPos, dist);
			}

			protected Optional<FoundRectangle> createPortal(GelPortalForcer portalForcer, BlockPos pos, BlockPos foundPos, double dist, Direction.Axis axis, Direction direction, ServerLevel level, UnaryOperator<BlockState> portalState)
			{
				WorldBorder worldBorder = level.getWorldBorder();
				int maxHeight = Math.min(level.getMaxBuildHeight(), level.getMinBuildHeight() + level.getLogicalHeight() - 1);
				BlockPos.MutableBlockPos mutablePos = pos.mutable();
				GelPortalSize size = portalForcer.portalBlock.getSizeRules();
				int minWidth = size.minWidth(), minHeight = size.minHeight();
				
				// No valid location was found. Attempt to use the default height and flag to make a platform
				boolean shouldMakePlatform = false;
				if (dist == -1.0)
				{
					int lowest = Math.max(level.getMinBuildHeight() + 1, this.getDefaultHeight());
					int heighest = maxHeight - 9;
					if (heighest < lowest)
					{
						return Optional.empty();
					}

					foundPos = new BlockPos(pos.getX() - direction.getStepX() * 1, Mth.clamp(pos.getY(), lowest, heighest), pos.getZ() - direction.getStepZ() * 1).immutable();
					foundPos = worldBorder.clampToBounds(foundPos);

					shouldMakePlatform = true;
				}

				// Create a platform and air pocket
				if (shouldMakePlatform || (this.alwaysOnTopBlock && this.isOverWater(level, foundPos.immutable(), direction)))
				{
					Direction direction1 = direction.getClockWise();
					for (int i = -1; i < 2; i++) // front and back of portal
					{
						for (int w = 0; w < minWidth; w++) // width
						{
							for (int y = -1; y < minHeight; y++) // height
							{
								BlockState state = y < 0 ? portalForcer.frameBlock : Blocks.AIR.defaultBlockState();
								mutablePos.setWithOffset(foundPos, w * direction.getStepX() + i * direction1.getStepX(), y, w * direction.getStepZ() + i * direction1.getStepZ());
								level.setBlockAndUpdate(mutablePos, state);
							}
						}
					}
				}

				// Place the frame
				for (int w = -1; w < minWidth + 1; w++)
				{
					for (int y = -1; y < minHeight + 1; y++)
					{
						if (w == -1 || w == minWidth || y == -1 || y == minHeight)
						{
							mutablePos.setWithOffset(foundPos, w * direction.getStepX(), y, w * direction.getStepZ());
							level.setBlock(mutablePos, portalForcer.frameBlock, Block.UPDATE_ALL);
						}
					}
				}

				// Place the portal blocks
				BlockState portal = portalState.apply(portalForcer.portalBlock.defaultBlockState().setValue(GelPortalBlock.AXIS, axis));
				for (int w = 0; w < minWidth; w++)
				{
					for (int y = 0; y < minHeight; y++)
					{
						mutablePos.setWithOffset(foundPos, w * direction.getStepX(), y, w * direction.getStepZ());
						level.setBlock(mutablePos, portal, Block.UPDATE_CLIENTS + Block.UPDATE_KNOWN_SHAPE);
					}
				}

				return Optional.of(new BlockUtil.FoundRectangle(foundPos.immutable(), minWidth, minHeight));
			}

			public static record FoundPortalSpot(BlockPos foundPos, double dist)
			{

			}

			/**
			 * @param pos
			 *            The lowest position of a portal block
			 * @param direction
			 *            The direction the portal will be expanded in
			 * @return True if extra blocks should be placed to create a platform
			 */
			public boolean isOverWater(ServerLevel level, BlockPos pos, Direction direction)
			{
				return level.getBlockState(pos.below()).liquid() && level.getBlockState(pos.below().relative(direction)).liquid();
			}

			public boolean canPortalReplaceBlock(ServerLevel level, BlockPos.MutableBlockPos pos)
			{
				BlockState state = level.getBlockState(pos);
				return state.canBeReplaced() && state.getFluidState().isEmpty();
			}

			public boolean canHostFrame(ServerLevel level, BlockPos searchPos, BlockPos.MutableBlockPos mutablePos, Direction dir, int offsetScale, int minWidth, int minHeight)
			{
				Direction direction = dir.getClockWise();

				for (int i = -1; i < minWidth + 1; i++)
				{
					for (int y = -1; y < minHeight + 1; y++)
					{
						mutablePos.setWithOffset(searchPos, dir.getStepX() * i + direction.getStepX() * offsetScale, y, dir.getStepZ() * i + direction.getStepZ() * offsetScale);
						// If always on surface, we basically get to ignore this bit. It allows the portal to place over water. Otherwise, use default logic.
						if (y < 0 && (!this.alwaysOnTopBlock && !level.getBlockState(mutablePos).isSolid()))
						{
							return false;
						}

						if (y >= 0 && !this.canPortalReplaceBlock(level, mutablePos))
						{
							return false;
						}
					}
				}

				return true;
			}

			public int getDefaultHeight()
			{
				return this.defaultHeight;
			}

			public boolean shouldIgnoreForHeight(ServerLevel level, BlockPos pos)
			{
				// This should only run if we only want to place on the world surface, ignoring stuff like trees
				if (!this.alwaysOnTopBlock)
					return false;
				BlockState state = level.getBlockState(pos);
				return state.is(BlockTags.LEAVES) || state.is(BlockTags.LOGS) || level.isEmptyBlock(pos) || (state.getCollisionShape(level, pos).isEmpty() && !state.liquid());
			}
		}
	}
}
