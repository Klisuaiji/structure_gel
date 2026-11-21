package com.legacy.structure_gel.api.block;

import java.util.Optional;
import java.util.Random;

import javax.annotation.Nullable;

import com.legacy.structure_gel.api.dimension.portal.GelPortalFinder;
import com.legacy.structure_gel.api.dimension.portal.GelPortalForcer;
import com.legacy.structure_gel.api.dimension.portal.GelPortalShape;
import com.legacy.structure_gel.core.StructureGelMod;
import com.mojang.serialization.MapCodec;

import net.minecraft.BlockUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Portal;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.portal.PortalShape;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * A portal block designed for easy mod compatibility.
 *
 * @author Silver_David
 */
public abstract class GelPortalBlock extends Block implements Portal
{
	public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
	protected static final int AABB_OFFSET = 2;
	protected static final VoxelShape X_AXIS_AABB = Block.box(0.0, 0.0, 6.0, 16.0, 16.0, 10.0);
	protected static final VoxelShape Z_AXIS_AABB = Block.box(6.0, 0.0, 0.0, 10.0, 16.0, 16.0);

	public GelPortalBlock(BlockBehaviour.Properties properties)
	{
		super(properties);
		this.registerDefaultState(this.stateDefinition.any().setValue(AXIS, Direction.Axis.X));
	}

	public void tryIgnite(Level level, BlockPos pos)
	{
		if (this.inPortalDimension(level.dimension()))
		{
			Optional<GelPortalShape> optional = this.getPortalFinder().findEmptyPortalShape(level, pos, Direction.Axis.X);
			if (optional.isPresent())
			{
				optional.get().createPortalBlocks(level);
			}
		}
	}

	public abstract MapCodec<? extends GelPortalBlock> codec();

	/**
	 * @return The dimension the portal takes you to.
	 */
	public abstract ResourceKey<Level> getDestination();

	/**
	 * @return The dimension you would originally make the portal. For a nether
	 *         portal, this would be the overworld.
	 */
	public abstract ResourceKey<Level> getSource();

	public abstract GelPortalForcer.PortalCreator getPortalCreator();

	public abstract ResourceKey<PoiType> getPortalPoi();

	public abstract BlockState getFrameBlock();

	public abstract BlockState getIgnitionBlock();

	public boolean isFrame(BlockState state, BlockGetter level, BlockPos pos)
	{
		return state.is(this.getFrameBlock().getBlock());
	}

	public boolean canReplace(BlockState state, BlockGetter level, BlockPos pos)
	{
		return state.isAir() || state.is(this) || state.is(this.getIgnitionBlock().getBlock());
	}

	public GelPortalFinder getPortalFinder()
	{
		return new GelPortalFinder(this);
	}

	public GelPortalSize getSizeRules()
	{
		return GelPortalSize.DEFAULT;
	}

	public static record GelPortalSize(int minWidth, int maxWidth, int minHeight, int maxHeight)
	{
		public static final GelPortalSize DEFAULT = new GelPortalSize(2, 21, 3, 21);
	}

	public GelPortalForcer getPortalForcer(ServerLevel from)
	{
		return new GelPortalForcer(from, this);
	}

	public boolean inPortalDimension(ResourceKey<Level> levelKey)
	{
		return levelKey == this.getSource() || levelKey == this.getDestination();
	}

	/**
	 * Code to execute when rendering portal texture on the player's screen. Mimics
	 * vanilla rendering by default
	 */
	@OnlyIn(Dist.CLIENT)
	public void renderPortal(GuiGraphics graphics, float alpha)
	{
		if (alpha < 1.0F)
		{
			alpha *= alpha;
			alpha *= alpha;
			alpha = alpha * 0.8F + 0.2F;
		}
		TextureAtlasSprite textureatlassprite = this.getPortalTexture();
		graphics.setColor(1.0F, 1.0F, 1.0F, alpha);
		graphics.blit(0, 0, 0, graphics.guiWidth(), graphics.guiHeight(), textureatlassprite,
				textureatlassprite.getU0(), textureatlassprite.getV0(),
				textureatlassprite.getU1(), textureatlassprite.getV1());
		graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
	}

	/**
	 * @return The texture to overlay on the screen. Uses this block's particle
	 *         texture by default
	 */
	@SuppressWarnings("deprecation")
	@OnlyIn(Dist.CLIENT)
	public net.minecraft.client.renderer.texture.TextureAtlasSprite getPortalTexture()
	{
		net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
		return mc.getBlockRenderer().getBlockModelShaper().getParticleIcon(this.defaultBlockState());
	}

	/**
	 * @return The sound that plays when the player steps in the portal. Uses nether
	 *         portal sounds by default
	 */
	@OnlyIn(Dist.CLIENT)
	public net.minecraft.client.resources.sounds.SoundInstance getTriggerSound()
	{
		return net.minecraft.client.resources.sounds.SimpleSoundInstance.forLocalAmbience(SoundEvents.PORTAL_TRIGGER, new Random().nextFloat() * 0.4F + 0.8F, 0.25F);
	}

	/**
	 * @return The sound that plays when the player travels through the portal. Uses
	 *         nether portal sounds by default
	 */
	@OnlyIn(Dist.CLIENT)
	public net.minecraft.client.resources.sounds.SoundInstance getTravelSound()
	{
		return net.minecraft.client.resources.sounds.SimpleSoundInstance.forLocalAmbience(SoundEvents.PORTAL_TRAVEL, new Random().nextFloat() * 0.4F + 0.8F, 0.25F);
	}

	@Override
	protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity)
	{
		if (entity.canUsePortal(true))
		{
			entity.setAsInsidePortal(this, pos);
		}
	}

	@Override
	public int getPortalTransitionTime(ServerLevel level, Entity entity)
	{
		if (Blocks.NETHER_PORTAL instanceof Portal portal)
			return portal.getPortalTransitionTime(level, entity);

		// Copied straight from the nether portal block. We use the above check by default to account for any changes made by other mods.
		return entity instanceof Player player ? Math.max(0, level.getGameRules().getInt(player.getAbilities().invulnerable ? GameRules.RULE_PLAYERS_NETHER_PORTAL_CREATIVE_DELAY : GameRules.RULE_PLAYERS_NETHER_PORTAL_DEFAULT_DELAY)) : 0;
	}

	/**
	 * @return An offset to apply when searching for an exit portal, before teleportation scaling is accounted for
	 */
	public Vec3i getExitOffset(BlockState entryPortal, BlockPos entryPortalPos, ServerLevel level, Entity entity)
	{
		return Vec3i.ZERO;
	}
	
	@Nullable
	@Override
	public DimensionTransition getPortalDestination(ServerLevel level, Entity entity, BlockPos entryPos)
	{
		BlockState portal = level.getBlockState(entryPos);
		Vec3i exitOffset = portal.is(this) ? this.getExitOffset(portal, entryPos, level, entity) : Vec3i.ZERO;
		
		ResourceKey<Level> destKey = level.dimension() == this.getDestination() ? this.getSource() : this.getDestination();
		ServerLevel destination = level.getServer().getLevel(destKey);
		if (destination == null)
		{
			return null;
		}
		else
		{
			double teleportScale = DimensionType.getTeleportationScale(level.dimensionType(), destination.dimensionType());
			// nether 16, overworld 128
			int searchRadius = Math.max((int) (16 * teleportScale), 16);
			WorldBorder worldborder = destination.getWorldBorder();
			BlockPos exitPos = worldborder.clampToBounds((entity.getX() + exitOffset.getX()) * teleportScale, entity.getY() + exitOffset.getY(), (entity.getZ() + exitOffset.getZ()) * teleportScale);
			return getExitPortal(destination, entity, entryPos, exitPos, searchRadius, worldborder, this.getPortalForcer(destination));
		}
	}
	
	@Nullable
	protected static DimensionTransition getExitPortal(ServerLevel level, Entity entity, BlockPos entryPos, BlockPos exitPos, int searchRadius, WorldBorder worldBorder, GelPortalForcer portalForcer)
	{
		Optional<BlockPos> closetPortal = portalForcer.findClosestPortalPosition(exitPos, searchRadius, worldBorder);
		BlockUtil.FoundRectangle rectangle;
		DimensionTransition.PostDimensionTransition postTeleport;
		if (closetPortal.isPresent())
		{
			BlockPos blockpos = closetPortal.get();
			BlockState portalState = level.getBlockState(blockpos);
			rectangle = BlockUtil.getLargestRectangleAround(blockpos, portalState.getValue(AXIS), 21, Direction.Axis.Y, 21, p -> level.getBlockState(p) == portalState);
			postTeleport = DimensionTransition.PLAY_PORTAL_SOUND.then(e -> e.placePortalTicket(blockpos));
		}
		else
		{
			Direction.Axis axis = entity.level().getBlockState(entryPos).getOptionalValue(AXIS).orElse(Direction.Axis.X);
			Optional<BlockUtil.FoundRectangle> createdPortal = portalForcer.createPortal(portalForcer, exitPos, axis);
			if (createdPortal.isEmpty())
			{
				StructureGelMod.LOGGER.error("Unable to create a portal, likely target out of worldborder");
				return null;
			}

			rectangle = createdPortal.get();
			postTeleport = DimensionTransition.PLAY_PORTAL_SOUND.then(DimensionTransition.PLACE_PORTAL_TICKET);
		}

		return getDimensionTransitionFromExit(entity, entryPos, rectangle, level, postTeleport);
	}
	
	protected static DimensionTransition getDimensionTransitionFromExit(Entity entity, BlockPos entryPos, BlockUtil.FoundRectangle rect, ServerLevel level, DimensionTransition.PostDimensionTransition postTeleport)
	{
		BlockState state = entity.level().getBlockState(entryPos);
		Direction.Axis axis;
		Vec3 relativePos;
		if (state.hasProperty(AXIS))
		{
			axis = state.getValue(AXIS);
			BlockUtil.FoundRectangle blockutil$foundrectangle = BlockUtil.getLargestRectangleAround(entryPos, axis, 21, Direction.Axis.Y, 21, p -> entity.level().getBlockState(p) == state);
			relativePos = entity.getRelativePortalPosition(axis, blockutil$foundrectangle);
		}
		else
		{
			axis = Direction.Axis.X;
			relativePos = new Vec3(0.5, 0.0, 0.0);
		}

		return createDimensionTransition(level, rect, axis, relativePos, entity, postTeleport);
	}

	protected static DimensionTransition createDimensionTransition(ServerLevel level, BlockUtil.FoundRectangle rect, Direction.Axis axis, Vec3 relativePos, Entity entity, DimensionTransition.PostDimensionTransition postTeleport)
	{
		BlockPos blockpos = rect.minCorner;
		BlockState blockstate = level.getBlockState(blockpos);
		Direction.Axis direction$axis = blockstate.getOptionalValue(AXIS).orElse(Direction.Axis.X);
		double d0 = (double) rect.axis1Size;
		double d1 = (double) rect.axis2Size;
		EntityDimensions entitydimensions = entity.getDimensions(entity.getPose());
		int i = axis == direction$axis ? 0 : 90;
		double d2 = (double) entitydimensions.width() / 2.0 + (d0 - (double) entitydimensions.width()) * relativePos.x();
		double d3 = (d1 - (double) entitydimensions.height()) * relativePos.y();
		double d4 = 0.5 + relativePos.z();
		boolean flag = direction$axis == Direction.Axis.X;
		Vec3 vec3 = new Vec3((double) blockpos.getX() + (flag ? d2 : d4), (double) blockpos.getY() + d3, (double) blockpos.getZ() + (flag ? d4 : d2));
		Vec3 vec31 = PortalShape.findCollisionFreePosition(vec3, level, entity, entitydimensions);
		return new DimensionTransition(level, vec31, Vec3.ZERO, (float) i, 0.0F, postTeleport);
	}

	@Override
	public Portal.Transition getLocalTransition()
	{
		return Portal.Transition.CONFUSION;
	}

	@Override
	protected BlockState updateShape(BlockState state, Direction updateDir, BlockState updateState, LevelAccessor level, BlockPos pos, BlockPos updatePos)
	{
		Direction.Axis updateAxis = updateDir.getAxis();
		Direction.Axis axis = state.getValue(AXIS);
		boolean flag = axis != updateAxis && updateAxis.isHorizontal();
		return !flag && !updateState.is(this) && !this.getPortalFinder().findAnyShape(level, pos, axis).isComplete() ? Blocks.AIR.defaultBlockState() : super.updateShape(state, updateDir, updateState, level, pos, updatePos);
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
	{
		switch ((Direction.Axis) state.getValue(AXIS))
		{
		case Z:
			return Z_AXIS_AABB;
		case X:
		default:
			return X_AXIS_AABB;
		}
	}

	@Override
	public ItemStack getCloneItemStack(LevelReader level, BlockPos blockPos, BlockState state)
	{
		return ItemStack.EMPTY;
	}

	@Override
	public BlockState rotate(BlockState state, Rotation rotation)
	{
		switch (rotation)
		{
		case COUNTERCLOCKWISE_90:
		case CLOCKWISE_90:
			switch ((Direction.Axis) state.getValue(AXIS))
			{
			case Z:
				return state.setValue(AXIS, Direction.Axis.X);
			case X:
				return state.setValue(AXIS, Direction.Axis.Z);
			default:
				return state;
			}
		default:
			return state;
		}
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
	{
		builder.add(AXIS);
	}
}
