package com.legacy.structure_gel.core.block;

import javax.annotation.Nullable;

import com.legacy.structure_gel.api.block.gel.StructureGelBlock;
import com.legacy.structure_gel.api.data.tags.SGTags;
import com.legacy.structure_gel.core.SGConfig;
import com.legacy.structure_gel.core.block_entity.GelSpreaderBlockEntity;
import com.legacy.structure_gel.core.registry.SGRegistry;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class GelSpreaderBlock extends BaseEntityBlock
{
	public static final MapCodec<GelSpreaderBlock> CODEC = simpleCodec(GelSpreaderBlock::new);
	public static final EnumProperty<Axis> AXIS = BlockStateProperties.AXIS;
	public static final BooleanProperty DECAY = StructureGelBlock.DECAY;

	public GelSpreaderBlock(Properties properties)
	{
		super(properties);
		this.registerDefaultState(this.stateDefinition.any().setValue(AXIS, Direction.Axis.Y).setValue(DECAY, false));
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec()
	{
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(Builder<Block, BlockState> builder)
	{
		builder.add(AXIS, DECAY);
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random)
	{
		if (state.getValue(DECAY))
			level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context)
	{
		return this.defaultBlockState().setValue(AXIS, context.getNearestLookingDirection().getAxis());
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
	{
		return SGRegistry.BlockEntities.GEL_SPREADER.get().create(pos, state);
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType)
	{
		return level.isClientSide ? null : createTickerHelper(blockEntityType, SGRegistry.BlockEntities.GEL_SPREADER.get(), GelSpreaderBlockEntity::serverTick);
	}

	// -------- COPIES PROPERTIES FROM GEL BLOCKS -----------

	@Override
	public InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit)
	{
		if (player.isCreative() && stack.is(Items.GUNPOWDER))
		{
			StructureGelBlock.scheduleRemoval(level, pos, state);
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.FAIL;
	}

	/**
	 * You can only interact with Gel if you're holding an item that needs to
	 * interact with it, crouching, or in survival mode
	 */
	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
	{
		if (SGConfig.COMMON.advancedGelBehavior())
		{
			if (context == CollisionContext.empty() || (context instanceof EntityCollisionContext eContext && eContext.getEntity() instanceof Player player && (player.isShiftKeyDown() || player.isHolding(stack -> stack.is(SGTags.ItemTags.GEL_INTERACTABLE)) || !player.isCreative())))
				return Shapes.block();
			return Shapes.empty();
		}

		return Shapes.block();
	}

	@Override
	public boolean canBeReplaced(BlockState state, BlockPlaceContext context)
	{
		if (SGConfig.COMMON.advancedGelBehavior())
		{
			Player player = context.getPlayer();
			if (player != null && (player.isShiftKeyDown() || context.getItemInHand().is(SGTags.ItemTags.GEL)))
				return false;
			return true;
		}
		return false;
	}

	@Override
	public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
	{
		return Shapes.empty();
	}

	@Override
	public boolean propagatesSkylightDown(BlockState state)
	{
		return true;
	}

	@OnlyIn(Dist.CLIENT)
	@Override
	public boolean skipRendering(BlockState state, BlockState adjacentState, Direction side)
	{
		return adjacentState.is(SGTags.BlockTags.GEL) || adjacentState.is(this);
	}

	@OnlyIn(Dist.CLIENT)
	@Override
	public float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos)
	{
		return 1.0F;
	}
}
