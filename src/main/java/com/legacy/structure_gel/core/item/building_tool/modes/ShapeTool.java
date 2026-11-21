package com.legacy.structure_gel.core.item.building_tool.modes;

import java.util.List;

import com.legacy.structure_gel.api.structure.base.IModifyState;
import com.legacy.structure_gel.core.data_components.BlockPalette;
import com.legacy.structure_gel.core.item.building_tool.ActionHistory;
import com.legacy.structure_gel.core.item.building_tool.BuildingToolItem;
import com.legacy.structure_gel.core.item.building_tool.BuildingToolMode;
import com.legacy.structure_gel.core.item.building_tool.ToolModeProperty;
import com.legacy.structure_gel.core.item.building_tool.ToolModeProperty.Replace;
import com.legacy.structure_gel.core.item.building_tool.ToolModeProperty.Shape;
import com.legacy.structure_gel.core.util.SGText;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Style;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class ShapeTool extends BuildingToolMode
{
	public ShapeTool(String name, int modelIndex)
	{
		super(name, modelIndex);
	}

	@Override
	public void onLeftClick(Level level, Player player, BlockPos clickedPos, ItemStack stack, Direction clickedFace)
	{
		if (level.isClientSide)
			return;
		BlockPalette palette = BuildingToolItem.getPalette(stack, level);
		if (!palette.isEmpty())
		{
			boolean causesBlockUpdates = BuildingToolItem.causesBlockUpdates(stack);
			double integrity = BuildingToolItem.getProperty(stack, ToolModeProperty.INTEGRITY);
			int radius = BuildingToolItem.getProperty(stack, ToolModeProperty.RADIUS);
			Shape shape = BuildingToolItem.getProperty(stack, ToolModeProperty.SHAPE);
			Replace replace = BuildingToolItem.getProperty(stack, ToolModeProperty.REPLACE);
			BlockState clickedState = level.getBlockState(clickedPos);
			var action = ActionHistory.newAction(level, causesBlockUpdates);
			RandomSource rand = level.getRandom();
			int total = this.forPosesWithin(clickedPos.offset(-radius, -radius, -radius), clickedPos.offset(radius, radius, radius), pos ->
			{
				if (rand.nextFloat() < integrity && shape.isInside(clickedPos.offset(-pos.getX(), -pos.getY(), -pos.getZ()), radius))
				{
					if (replace.shouldReplace(level, clickedState, pos))
					{
						var opState = palette.getRandom(rand);
						if (opState.isPresent())
							return this.setBlock(level, pos, oldState -> replace == Replace.CLICKED_BLOCK ? IModifyState.mergeStates(opState.get().data(), oldState) : opState.get().data(), causesBlockUpdates, action);
					}
				}
				return false;
			});
			ActionHistory.get(player).add(level, action);
			sendPlaceMessage(player, replace, total, palette);
		}
		else
		{
			BuildingToolMode.sendMessage(player, MISSING_STATE_KEY, Style.EMPTY.withColor(ChatFormatting.RED));
		}
	}

	@Override
	public int getReachDistance(ItemStack stack)
	{
		int radius = BuildingToolItem.getProperty(stack, ToolModeProperty.RADIUS);
		return 2 + radius;
	}

	@SuppressWarnings("resource")
	@Override
	public Object[] getDescArgs()
	{
		net.minecraft.client.Options options = net.minecraft.client.Minecraft.getInstance().options;
		var middleClick = SGText.keybindString(options.keyPickItem);
		var leftClick = SGText.keybindString(options.keyAttack);
		return new Object[] { middleClick, leftClick };
	}

	@Override
	public void addProperties(List<ToolModeProperty<?>> properties)
	{
		super.addProperties(properties);
		properties.add(ToolModeProperty.INTEGRITY);
		properties.add(ToolModeProperty.RADIUS);
		properties.add(ToolModeProperty.SHAPE);
		properties.add(ToolModeProperty.REPLACE);
	}

	@Override
	public boolean hasBlockPalette()
	{
		return true;
	}
}
