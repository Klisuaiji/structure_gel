package com.legacy.structure_gel.api.item;

import com.legacy.structure_gel.core.registry.SGRegistry;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.GameMasterBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The item used by structure gel blocks.
 *
 * @author Silver_David
 */
public class StructureGelItem extends GameMasterBlockItem
{
	public StructureGelItem(Block block, Item.Properties properties)
	{
		super(block, properties);
	}

	@Override
	public InteractionResult place(BlockPlaceContext context)
	{
		// Prevent placement outside of creative mode
		if (!context.getPlayer().isCreative())
			return InteractionResult.FAIL;
		else
			return super.place(context);
	}
	
	@Override
	protected boolean updateCustomBlockEntityTag(BlockPos pos, Level level, Player player, ItemStack stack, BlockState state)
	{
		boolean flag = super.updateCustomBlockEntityTag(pos, level, player, stack, state);
		level.getBlockEntity(pos, SGRegistry.BlockEntities.GEL_SPREADER.get()).ifPresent(spreader -> spreader.applyProperties(stack, level.registryAccess()));
		return flag;
	}
}
