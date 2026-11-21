package com.legacy.structure_gel.api.block.gel;

import java.util.EnumMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import com.legacy.structure_gel.api.data.tags.SGTags;
import com.legacy.structure_gel.core.SGConfig;
import com.legacy.structure_gel.core.StructureGelMod;
import com.legacy.structure_gel.core.registry.SGRegistry;
import com.legacy.structure_gel.core.util.SGText;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class StructureGelBlock extends Block
{
	public static final MapCodec<StructureGelBlock> CODEC = RecordCodecBuilder.mapCodec(instance ->
	{
		//@formatter:off
		return instance.group(
				propertiesCodec(), 
				GelSpreadBehavior.CODEC.fieldOf("spread_behavior").forGetter(g -> g.spreadBehavior),
				GelSpreadRestriction.CODEC.listOf().fieldOf("spread_restrictions").forGetter(g -> g.spreadRestrictions),
				BlockState.CODEC.optionalFieldOf("replacement_state").forGetter(g -> g.replacementState))
				.apply(instance, StructureGelBlock::new);
		//@formatter:on
	});

	public static final EnumMap<Direction, EnumProperty<Neighbor>> NEIGHBORS = Util.make(new EnumMap<>(Direction.class), map ->
	{
		for (Direction dir : Direction.values())
			map.put(dir, EnumProperty.create(dir.getSerializedName(), Neighbor.class));
	});
	public static final BooleanProperty DECAY = BooleanProperty.create("decay");

	protected final GelSpreadBehavior spreadBehavior;
	protected final List<GelSpreadRestriction> spreadRestrictions;
	protected final Optional<BlockState> replacementState;

	public StructureGelBlock(Properties properties, GelSpreadBehavior spreadSides, List<GelSpreadRestriction> spreadRestrictions, Optional<BlockState> replacementState)
	{
		super(properties);
		this.spreadBehavior = spreadSides;
		this.spreadRestrictions = spreadRestrictions;
		this.replacementState = replacementState;

		BlockState defaultState = this.stateDefinition.any().setValue(DECAY, false);
		for (Direction dir : Direction.values())
			defaultState = defaultState.setValue(NEIGHBORS.get(dir), Neighbor.EMPTY);
		this.registerDefaultState(defaultState);
	}

	public StructureGelBlock(Properties properties, GelSpreadBehavior spreadSides, List<GelSpreadRestriction> spreadRestrictions)
	{
		this(properties, spreadSides, spreadRestrictions, Optional.empty());
	}

	@Override
	protected MapCodec<? extends Block> codec()
	{
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(Builder<Block, BlockState> builder)
	{
		for (var val : NEIGHBORS.values())
			builder.add(val);
		builder.add(DECAY);
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess tickAccess, BlockPos pos, Direction updateDir, BlockPos updatePos, BlockState updateState, RandomSource rand)
	{
		EnumProperty<Neighbor> prop = NEIGHBORS.get(updateDir);
		Neighbor oldNeighbor = state.getValue(prop);
		Neighbor newNeighbor = Neighbor.from(updateState);
		if (oldNeighbor == Neighbor.BLOCK && newNeighbor == Neighbor.EMPTY)
		{
			newNeighbor = Neighbor.SPREAD;
			tickAccess.scheduleTick(pos, this, 0);
		}
		return state.setValue(prop, newNeighbor);
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random)
	{
		if (state.getValue(DECAY))
		{
			this.setNeighborDecay(level, pos, state);
			level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
		}
		else
		{
			for (Direction dir : Direction.values())
			{
				EnumProperty<Neighbor> prop = NEIGHBORS.get(dir);
				Neighbor neighbor = state.getValue(prop);
				if (neighbor == Neighbor.SPREAD)
				{
					BlockPos offset = pos.relative(dir);
					if (level.getBlockState(offset).isAir())
						placeGel(level, this, offset);
				}
			}
		}
	}

	public static boolean placeGel(ServerLevel level, Block gel, BlockPos pos)
	{
		return level.setBlock(pos, StructureGelBlock.getStateForPosition(level, gel, pos), Block.UPDATE_ALL);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context)
	{
		if (context.getPlayer().isShiftKeyDown())
		{
			return SGRegistry.Blocks.GEL_SPREADER.get().getStateForPlacement(context);
		}
		return getStateForPosition(context.getLevel(), this, context.getClickedPos());
	}

	public static BlockState getStateForPosition(LevelReader level, Block gel, BlockPos pos)
	{
		BlockState state = gel.defaultBlockState();
		for (Direction dir : Direction.values())
			state = state.setValue(NEIGHBORS.get(dir), Neighbor.from(level.getBlockState(pos.relative(dir))));
		return state;
	}

	@Override
	public InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit)
	{
		if (player.isCreative() && stack.is(Items.GUNPOWDER))
		{
			scheduleRemoval(level, pos, state);
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.FAIL;
	}

	/**
	 * Triggers a chain break reaction if broken in survival mode in case a survival
	 * player finds it
	 */
	@Override
	public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player)
	{
		if (!player.isCreative())
		{
			this.setNeighborDecay(level, pos, state);
		}
		return super.playerWillDestroy(level, pos, state, player);
	}

	public void setNeighborDecay(Level level, BlockPos pos, BlockState state)
	{
		for (Vec3i offset : this.spreadBehavior.getSpreadOffsets(new GelSpreadBehavior.Context(pos, Optional.empty())))
		{
			BlockPos offsetPos = pos.offset(offset);
			BlockState neighbor = level.getBlockState(offsetPos);
			if (neighbor.hasProperty(DECAY) && (neighbor.is(this) || neighbor.is(SGRegistry.Blocks.GEL_SPREADER.get())))
				scheduleRemoval(level, offsetPos, neighbor);
		}
	}

	public static void scheduleRemoval(Level level, BlockPos pos, BlockState state)
	{
		level.setBlock(pos, state.setValue(DECAY, true), Block.UPDATE_NONE);
		level.scheduleTick(pos, state.getBlock(), 1);
	}

	public GelSpreadBehavior getSpreadBehavior()
	{
		return this.spreadBehavior;
	}

	public List<GelSpreadRestriction> getSpreadRestrictions()
	{
		return this.spreadRestrictions;
	}

	public BlockState getReplacementState()
	{
		return this.replacementState.orElse(Blocks.AIR.defaultBlockState());
	}

	@Override
	public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
	{
		return Shapes.empty();
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
	public boolean propagatesSkylightDown(BlockState state)
	{
		return true;
	}

	@OnlyIn(Dist.CLIENT)
	@Override
	public boolean skipRendering(BlockState state, BlockState adjacentState, Direction side)
	{
		return adjacentState.is(this) || adjacentState.is(SGRegistry.Blocks.GEL_SPREADER.get());
	}

	@OnlyIn(Dist.CLIENT)
	@Override
	public float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos)
	{
		return 1.0F;
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, net.minecraft.world.item.TooltipFlag advancedTooltips)
	{
		if (!net.minecraft.client.gui.screens.Screen.hasShiftDown())
		{
			tooltip.add(SGText.applyKeybindFilter(legacyTranslation("hold_shift")));
		}
		else
		{
			// Interactions
			tooltip.add(legacyTranslation("place"));
			tooltip.add(legacyTranslation("gunpowder"));
			tooltip.add(Component.empty());

			// Spread behavior
			tooltip.add(legacyTranslation("spread_behavior").withStyle(ChatFormatting.LIGHT_PURPLE).withStyle(ChatFormatting.UNDERLINE));
			tooltip.add(this.spreadBehavior.getTooltip());

			// Restrictions
			tooltip.add(Component.empty());
			tooltip.add(legacyTranslation("spread_restrictions").withStyle(ChatFormatting.LIGHT_PURPLE).withStyle(ChatFormatting.UNDERLINE));
			for (GelSpreadRestriction r : this.spreadRestrictions)
				tooltip.add(r.getTooltip());
		}
	}

	public static MutableComponent legacyTranslation(String key, Object... args)
	{
		return Component.translatable("info." + StructureGelMod.MODID + "." + key, args).withStyle(ChatFormatting.GRAY);
	}

	public static enum Neighbor implements StringRepresentable
	{
		EMPTY("empty", s -> s.isAir() || s.is(SGTags.BlockTags.GEL)),
		BLOCK("block", s -> true),
		SPREAD("spread", s -> false);

		public static final Codec<Neighbor> CODEC = StringRepresentable.fromValues(Neighbor::values);

		final String name;
		final Predicate<BlockState> blockPredicate;

		Neighbor(String name, Predicate<BlockState> blockPredicate)
		{
			this.name = name;
			this.blockPredicate = blockPredicate;
		}

		@Override
		public String getSerializedName()
		{
			return name;
		}

		public boolean test(BlockState state)
		{
			return blockPredicate.test(state);
		}

		public static Neighbor from(BlockState state)
		{
			for (var n : values())
				if (n.test(state))
					return n;
			return BLOCK;
		}
	}
}
