package com.legacy.structure_gel.core.item.building_tool.modes;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.legacy.structure_gel.core.data_components.BlockPalette;
import com.legacy.structure_gel.core.item.building_tool.ActionHistory;
import com.legacy.structure_gel.core.item.building_tool.BuildingToolItem;
import com.legacy.structure_gel.core.item.building_tool.BuildingToolMode;
import com.legacy.structure_gel.core.item.building_tool.ToolModeProperty;
import com.legacy.structure_gel.core.item.building_tool.ToolModeProperty.Replace;
import com.legacy.structure_gel.core.util.SGText;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class FloodTool extends BuildingToolMode
{
	public FloodTool(String name, int modelIndex)
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
			boolean extendDown = BuildingToolItem.getProperty(stack, ToolModeProperty.EXTEND_DOWN).value();
			Replace replace = BuildingToolItem.getProperty(stack, ToolModeProperty.REPLACE_NOT_AIR_CLICKED);
			int radius = BuildingToolItem.getProperty(stack, ToolModeProperty.LARGE_RADIUS);
			Set<BlockPos> poses = getFloodPositions(level, clickedPos, clickedFace, replace, radius);
			if (poses.isEmpty())
				return;
			var action = ActionHistory.newAction(level, causesBlockUpdates);
			int dist = extendDown ? 100 : 1;
			BlockState air = Blocks.AIR.defaultBlockState();
			int total = 0;
			for (BlockPos pos : poses)
			{
				for (BlockPos.MutableBlockPos mutPos = pos.mutable(); replace.shouldReplace(level, air, mutPos) && pos.getY() - mutPos.getY() < dist; mutPos.move(Direction.DOWN))
				{
					var opState = palette.getRandom(level.random);
					if (opState.isPresent())
					{
						if (this.setBlock(level, mutPos.immutable(), opState.get().data(), causesBlockUpdates, action))
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
		var middleClick = SGText.keybindString(options.keyPickItem);
		var leftClick = SGText.keybindString(options.keyAttack);
		var rightClick = SGText.keybindString(options.keyUse);
		return new Object[] { middleClick, leftClick, rightClick };
	}

	@Override
	public void addProperties(List<ToolModeProperty<?>> properties)
	{
		super.addProperties(properties);
		properties.add(ToolModeProperty.EXTEND_DOWN);
		properties.add(ToolModeProperty.LARGE_RADIUS);
		properties.add(ToolModeProperty.REPLACE_NOT_AIR_CLICKED);
	}

	@Override
	public boolean hasBlockPalette()
	{
		return true;
	}
	
	public static Set<BlockPos> getFloodPositions(Level level, BlockPos clickedPos, Direction clickedFace, Replace replace, int radius)
	{
		BlockPos startPos = clickedPos.relative(clickedFace);
		if (!replace.shouldReplace(level, level.getBlockState(startPos), startPos))
			return Collections.emptySet();
		Set<BlockPos> allPositions = new HashSet<>();
		Set<BlockPos> posesToSearch = new HashSet<>();
		posesToSearch.add(startPos);
		Direction[] dirs = new Direction[] { Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST };
		while (!posesToSearch.isEmpty())
		{
			Set<BlockPos> newPoses = new HashSet<>();
			for (BlockPos pos : posesToSearch)
			{
				if (pos.distManhattan(startPos) > radius)
					continue;
				allPositions.add(pos);
				for (Direction dir : dirs)
				{
					BlockPos offset = pos.relative(dir);
					if (!allPositions.contains(offset) && replace.shouldReplace(level, level.getBlockState(offset), offset))
					{
						newPoses.add(offset);
					}
				}
			}
			posesToSearch = newPoses;
		}
		return allPositions;
	}
}
