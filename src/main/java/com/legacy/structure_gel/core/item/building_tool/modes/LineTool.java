package com.legacy.structure_gel.core.item.building_tool.modes;

import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.legacy.structure_gel.core.data_components.BlockPalette;
import com.legacy.structure_gel.core.item.building_tool.ActionHistory;
import com.legacy.structure_gel.core.item.building_tool.BuildingToolItem;
import com.legacy.structure_gel.core.item.building_tool.BuildingToolMode;
import com.legacy.structure_gel.core.item.building_tool.BuildingToolMode.ForCorners;
import com.legacy.structure_gel.core.item.building_tool.ToolModeProperty;
import com.legacy.structure_gel.core.item.building_tool.ToolModeProperty.Replace;
import com.legacy.structure_gel.core.util.SGText;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class LineTool extends ForCorners
{
	public LineTool(String name, int modelIndex)
	{
		super(name, modelIndex);
	}

	@Override
	public boolean hasAllCorners()
	{
		return false;
	}

	@Override
	protected void performAction(Level level, Player player, BlockPos clickedPos, ItemStack stack, BlockPos cornerA, BlockPos cornerB)
	{
		if (level.isClientSide)
			return;
		BlockPalette palette = BuildingToolItem.getPalette(stack, level);
		if (!palette.isEmpty())
		{
			boolean causesBlockUpdates = BuildingToolItem.causesBlockUpdates(stack);
			double integrity = BuildingToolItem.getProperty(stack, ToolModeProperty.INTEGRITY);
			Replace replace = BuildingToolItem.getProperty(stack, ToolModeProperty.REPLACE);
			BlockState clickedState = level.getBlockState(clickedPos);
			var action = ActionHistory.newAction(level, causesBlockUpdates);
			RandomSource rand = level.getRandom();
			Set<BlockPos> poses = getLinePositions(cornerA, cornerB);
			int total = 0;
			for (BlockPos pos : poses)
			{
				if (rand.nextFloat() < integrity)
				{
					if (replace.shouldReplace(level, clickedState, pos))
					{
						var opState = palette.getRandom(rand);
						if (opState.isPresent() && this.setBlock(level, pos, opState.get().data(), causesBlockUpdates, action))
							total++;
					}
				}
			}
			ActionHistory.get(player).add(level, action);
			sendPlaceMessage(player, replace, total, palette);
		}
		else
		{
			BuildingToolMode.sendMessage(player, MISSING_STATE_KEY, Style.EMPTY.withColor(ChatFormatting.RED));
		}
	}

	@SuppressWarnings("resource")
	@Override
	public Object[] getDescArgs()
	{
		net.minecraft.client.Options options = net.minecraft.client.Minecraft.getInstance().options;
		var shift = SGText.keybindString(options.keyShift);
		var middleClick = SGText.keybindString(options.keyPickItem);
		var rightClick = SGText.keybindString(options.keyUse);
		var leftClick = SGText.keybindString(options.keyAttack);
		return new Object[] { middleClick, Component.translatable(SELECT_POSITIONS, rightClick, shift, rightClick), leftClick };
	}

	@Override
	public void addProperties(List<ToolModeProperty<?>> properties)
	{
		super.addProperties(properties);
		properties.add(ToolModeProperty.INTEGRITY);
		properties.add(ToolModeProperty.REPLACE);
	}

	@Override
	public boolean hasBlockPalette()
	{
		return true;
	}

	@Override
	public Component getHudMessage(ItemStack stack)
	{
		Optional<BlockPos> p1 = BuildingToolItem.getPos(stack, 0);
		Optional<BlockPos> p2 = BuildingToolItem.getPos(stack, 1);
		if (p1.isPresent() && p2.isPresent())
		{
			DecimalFormat format = new DecimalFormat("0.##");
			Double dist = p1.get().getCenter().distanceTo(p2.get().getCenter()) + 1;
			return Component.translatable(LENGTH_KEY, Component.literal("" + format.format(dist)).withStyle(SGText.VALUE_LABEL_STYLE), Component.literal("" + getLinePositions(p1.get(), p2.get()).size()).withStyle(SGText.VALUE_LABEL_STYLE));
		}
		return null;
	}

	public static Set<BlockPos> getLinePositions(BlockPos cornerA, BlockPos cornerB)
	{
		Set<BlockPos> poses = new HashSet<>();
		double magnitude = Math.sqrt(cornerA.distSqr(cornerB));
		if (magnitude == 0)
		{
			poses.add(cornerA);
			return poses;
		}
		int x = cornerA.getX();
		int y = cornerA.getY();
		int z = cornerA.getZ();
		double dX = (x - cornerB.getX()) / magnitude;
		double dY = (y - cornerB.getY()) / magnitude;
		double dZ = (z - cornerB.getZ()) / magnitude;
		BlockPos pos = cornerA;
		int i = 0;
		while (!pos.equals(cornerB) && i < magnitude + 1)
		{
			pos = cornerA.offset((int) -Math.round(dX * i), (int) -Math.round(dY * i), (int) -Math.round(dZ * i));
			i += 1;
			poses.add(pos);
		}
		return poses;
	}
}
