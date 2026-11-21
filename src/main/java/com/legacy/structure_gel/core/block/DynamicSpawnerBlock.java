package com.legacy.structure_gel.core.block;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.legacy.structure_gel.core.StructureGelMod;
import com.legacy.structure_gel.core.block_entity.DynamicSpawnerBlockEntity;
import com.legacy.structure_gel.core.block_entity.DynamicSpawnerBlockEntity.ExpRange;
import com.legacy.structure_gel.core.registry.SGRegistry;
import com.legacy.structure_gel.core.util.SGText;
import com.mojang.serialization.MapCodec;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class DynamicSpawnerBlock extends BaseEntityBlock
{
	public static final MapCodec<DynamicSpawnerBlock> CODEC = simpleCodec(DynamicSpawnerBlock::new);

	public DynamicSpawnerBlock(Properties properties)
	{
		super(properties);
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec()
	{
		return CODEC;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
	{
		return new DynamicSpawnerBlockEntity(pos, state);
	}

	@Override
	public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult)
	{	
		BlockEntity blockEntity = level.getBlockEntity(pos);
		if (blockEntity instanceof DynamicSpawnerBlockEntity spawner && player.canUseGameMasterBlocks())
		{
			if (level.isClientSide)
			{
				StructureGelMod.proxy.openDynamicSpawnerScreen(spawner);
			}
			return InteractionResult.SUCCESS;
		}
		else
		{
			return InteractionResult.PASS;
		}
	}

	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type)
	{
		return createTickerHelper(type, SGRegistry.BlockEntities.DYNAMIC_SPAWNER.get(), level.isClientSide ? DynamicSpawnerBlockEntity::clientTick : DynamicSpawnerBlockEntity::serverTick);
	}

	@Override
	public int getExpDrop(BlockState state, LevelAccessor level, BlockPos pos, @Nullable BlockEntity blockEntity, @Nullable Entity breaker, ItemStack tool)
	{
		if (level.getBlockEntity(pos) instanceof DynamicSpawnerBlockEntity spawner)
		{
			ExpRange range = spawner.getExpRange();
			int min = range.min;
			int max = range.max;

			int r = (max - min) / 2 + 1;
			RandomSource rand = level.getRandom();
			return min + rand.nextInt(r) + rand.nextInt(r);
		}
		return Blocks.SPAWNER.getExpDrop(Blocks.SPAWNER.defaultBlockState(), level, pos, blockEntity, breaker, tool);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lore, TooltipFlag showAdvanced)
	{
		if (net.minecraft.client.gui.screens.Screen.hasShiftDown())
		{
			lore.add(Component.translatable("info.structure_gel.dynamic_spawner_description").withStyle(ChatFormatting.GRAY));
		}
		else
		{
			lore.add(SGText.applyKeybindFilter(Component.translatable("info.structure_gel.hold_shift")));

			CustomData beData = stack.get(DataComponents.BLOCK_ENTITY_DATA);
			if (beData != null)
			{
				lore.add(SGText.NEW_LINE);
				CompoundTag beTag = beData.copyTag();
				String typeName = beTag.getString(DynamicSpawnerBlockEntity.SPAWNER_ID_KEY);
				if (!typeName.isBlank())
					lore.add(SGText.BULLET_POINT.copy().withStyle(ChatFormatting.GRAY).append(SGText.TYPE_LABEL.copy().withStyle(SGText.VALUE_LABEL_STYLE)).append(Component.literal(": " + typeName)));

				if (beTag.contains(DynamicSpawnerBlockEntity.EXP_RANGE_KEY, Tag.TAG_COMPOUND))
				{
					ExpRange range = ExpRange.load(beTag.getCompound(DynamicSpawnerBlockEntity.EXP_RANGE_KEY));
					lore.add(SGText.BULLET_POINT.copy().withStyle(ChatFormatting.GRAY).append(Component.translatable("gui.structure_gel.dynamic_spawner.exp_range").withStyle(SGText.VALUE_LABEL_STYLE)).append(Component.literal(": " + range.min + " - " + range.max)));
				}
			}
		}
	}
}
