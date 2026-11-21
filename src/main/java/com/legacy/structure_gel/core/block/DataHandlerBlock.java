package com.legacy.structure_gel.core.block;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.legacy.structure_gel.core.StructureGelMod;
import com.legacy.structure_gel.core.block_entity.DataHandlerBlockEntity;
import com.legacy.structure_gel.core.block_entity.DataHandlerBlockEntity.RawHandler;
import com.legacy.structure_gel.core.util.SGText;
import com.mojang.serialization.MapCodec;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.GameMasterBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class DataHandlerBlock extends Block implements SimpleWaterloggedBlock, EntityBlock, GameMasterBlock
{
	public static final MapCodec<DataHandlerBlock> CODEC = simpleCodec(DataHandlerBlock::new);

	public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
	public static final EnumProperty<Mirror> MIRROR = EnumProperty.create("mirror", Mirror.class);
	public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
	public static final BooleanProperty CONNECT_TO_BLOCKS = BooleanProperty.create("connect_to_blocks");

	public DataHandlerBlock(Properties properties)
	{
		super(properties);
		this.registerDefaultState(this.stateDefinition.any().setValue(MIRROR, Mirror.NONE).setValue(FACING, Direction.NORTH).setValue(WATERLOGGED, false).setValue(CONNECT_TO_BLOCKS, true));
	}

	@Override
	protected MapCodec<? extends Block> codec()
	{
		return CODEC;
	}

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean copyData)
	{
		ItemStack stack = new ItemStack(this);
		Map<String, String> props = new HashMap<>();
		for (Property prop : List.of(CONNECT_TO_BLOCKS, WATERLOGGED))
		{
			BlockState defaultState = state.getBlock().defaultBlockState();
			var val = state.getValue(prop);
			if (!defaultState.getValue(prop).equals(val))
				props.put(prop.getName(), prop.getName(val));
		}
		if (!props.isEmpty())
			stack.set(DataComponents.BLOCK_STATE, new BlockItemStateProperties(props));

		return stack;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
	{
		return new DataHandlerBlockEntity(pos, state);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult)
	{
		return super.useWithoutItem(state, level, pos, player, hitResult);
	}

	@Override
	public InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult)
	{
		BlockEntity blockEntity = level.getBlockEntity(pos);
		if (blockEntity instanceof DataHandlerBlockEntity dataHandler && player.canUseGameMasterBlocks())
		{
			boolean waterlogged = state.getValue(WATERLOGGED);
			if ((stack.is(Items.WATER_BUCKET) && !waterlogged) || (stack.is(Items.BUCKET) && waterlogged))
			{
				return InteractionResult.PASS;
			}
			if (level.isClientSide)
			{
				StructureGelMod.proxy.openDataHandlerScreen(dataHandler);
			}
			return InteractionResult.SUCCESS;
		}
		else
		{
			return InteractionResult.PASS;
		}
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context)
	{
		FluidState fluidState = context.getLevel().getFluidState(context.getClickedPos());
		return this.defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite()).setValue(WATERLOGGED, fluidState.getType() == Fluids.WATER);
	}

	public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack)
	{
		if (stack.has(DataComponents.CUSTOM_NAME))
		{
			if (level.getBlockEntity(pos) instanceof DataHandlerBlockEntity dataHandler)
			{
				dataHandler.setCustomName(stack.getCustomName());
			}
		}

	}

	@Override
	public FluidState getFluidState(BlockState state)
	{
		return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(true) : Fluids.EMPTY.defaultFluidState();
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess tickAccess, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource rand)
	{
		if (state.getValue(WATERLOGGED))
			tickAccess.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));

		return state;
	}

	@Override
	public VoxelShape getOcclusionShape(BlockState state)
	{
		return state.getValue(WATERLOGGED) ? Shapes.empty() : Shapes.block();
	}

	@Override
	public BlockState rotate(BlockState state, Rotation rotation)
	{
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	public BlockState mirror(BlockState state, Mirror mirror)
	{
		return this.rotate(state, mirror.getRotation(state.getValue(FACING))).setValue(MIRROR, mirror);
	}

	@Override
	protected void createBlockStateDefinition(Builder<Block, BlockState> builder)
	{
		builder.add(MIRROR, FACING, WATERLOGGED, CONNECT_TO_BLOCKS);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lore, TooltipFlag showAdvanced)
	{
		if (net.minecraft.client.gui.screens.Screen.hasShiftDown())
		{
			lore.add(Component.translatable("info.structure_gel.data_handler_description").withStyle(ChatFormatting.GRAY));
		}
		else
		{
			lore.add(SGText.applyKeybindFilter(Component.translatable("info.structure_gel.hold_shift")));

			CustomData data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
			if (data != null)
			{
				CompoundTag beTag = data.copyTag();
				if (beTag.contains(DataHandlerBlockEntity.HANDLERS_KEY, Tag.TAG_LIST))
				{
					lore.add(SGText.NEW_LINE);
					List<RawHandler> handlers = DataHandlerBlockEntity.loadAllHandlers(beTag).unwrap();
					int size = handlers.size();
					for (int i = 0; i < size; i++)
					{
						if (i > 0)
							lore.add(SGText.NEW_LINE);

						RawHandler handler = handlers.get(i);
						ResourceLocation type = handler.typeName();
						if (!type.getPath().isEmpty())
						{
							lore.add(Component.literal(type.toString()).withStyle(ChatFormatting.GRAY));
							LinkedHashMap<String, String> dataEntries = handler.dataEntries();

							if (size > 1)
								lore.add(SGText.BULLET_POINT.copy().withStyle(ChatFormatting.GRAY).append(Component.translatable("info.structure_gel.building_tool.property.weight").withStyle(SGText.VALUE_LABEL_STYLE)).append(Component.literal(": " + handler.getWeight().asInt())));

							for (var entry : dataEntries.entrySet())
							{
								String key = entry.getKey();
								String translationKey = "gui.structure_gel.data_handler." + type + "." + key + ".label";
								MutableComponent propertyLabel = Component.translatable(translationKey);
								if (propertyLabel.getString().equals(translationKey))
									propertyLabel = Component.literal(key);

								String value = entry.getValue();
								if (!value.isEmpty())
									lore.add(SGText.BULLET_POINT.copy().withStyle(ChatFormatting.GRAY).append(propertyLabel.withStyle(SGText.VALUE_LABEL_STYLE)).append(Component.literal(": " + value)));
							}
						}
					}
				}
			}
		}
	}
}
