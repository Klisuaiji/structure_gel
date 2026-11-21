package com.legacy.structure_gel.core.item.building_tool.modes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.legacy.structure_gel.api.structure.base.IModifyState;
import com.legacy.structure_gel.core.data_components.BlockPalette;
import com.legacy.structure_gel.core.item.building_tool.ActionHistory;
import com.legacy.structure_gel.core.item.building_tool.BuildingToolItem;
import com.legacy.structure_gel.core.item.building_tool.BuildingToolMode;
import com.legacy.structure_gel.core.item.building_tool.ToolModeProperty;
import com.legacy.structure_gel.core.item.building_tool.ToolModeProperty.Replace;
import com.legacy.structure_gel.core.util.SGText;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedEntry.Wrapper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class ReplaceTool extends BuildingToolMode
{

	public ReplaceTool(String name, int modelIndex)
	{
		super(name, modelIndex);
	}

	@Override
	public void onLeftClick(Level level, Player player, BlockPos clickedPos, ItemStack stack, Direction clickedFace)
	{
		if (level.isClientSide)
			return;
		boolean causesBlockUpdates = BuildingToolItem.causesBlockUpdates(stack);
		double integrity = BuildingToolItem.getProperty(stack, ToolModeProperty.INTEGRITY);
		int radius = BuildingToolItem.getProperty(stack, ToolModeProperty.MEDIUM_RADIUS);
		boolean fuzzy = BuildingToolItem.getProperty(stack, ToolModeProperty.FUZZY_TRUE).value();
		boolean copyStates = BuildingToolItem.getProperty(stack, ToolModeProperty.COPY_STATES).value();
		Set<BlockPos> poses = getReplacePositions(level, clickedPos, radius, fuzzy);
		if (poses.isEmpty())
			return;
		RandomSource rand = level.random;
		var action = ActionHistory.newAction(level, causesBlockUpdates);
		BlockPalette palette = BuildingToolItem.getPalette(stack, level);
		for (BlockPos pos : poses)
		{
			if (rand.nextFloat() < integrity)
			{
				Optional<Wrapper<BlockState>> opState = palette.getRandom(rand);
				if (opState.isPresent())
				{
					BlockState toPlace = opState.get().data();
					this.setBlock(level, pos, oldState -> copyStates ? IModifyState.mergeStates(toPlace, oldState) : toPlace, causesBlockUpdates, action);
				}
			}
		}
		ActionHistory.get(player).add(level, action);
		// Using Replace.AIR to get the "Replaced" message
		sendPlaceMessage(player, Replace.AIR, poses.size(), palette);
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
		properties.add(ToolModeProperty.MEDIUM_RADIUS);
		properties.add(ToolModeProperty.COPY_STATES);
		properties.add(ToolModeProperty.FUZZY_TRUE);
	}

	@Override
	public boolean hasBlockPalette()
	{
		return true;
	}

	public static Set<BlockPos> getReplacePositions(Level level, BlockPos clickedPos, int radius, boolean fuzzy)
	{
		BlockState state = level.getBlockState(clickedPos);
		if (state.isAir())
			return Collections.emptySet();
		Block clickedBlock = state.getBlock();
		Set<BlockPos> allPositions = new HashSet<>();
		Set<BlockPos> posesToSearch = new HashSet<>();
		posesToSearch.add(clickedPos);
		Vec3i[] offsets;
		if (fuzzy)
		{
			int r = 1;
			int d = r * 2 + 1;
			List<Vec3i> o = new ArrayList<>(d * d * d - 1);
			for (int x = -r; x <= r; x++)
				for (int y = -r; y <= r; y++)
					for (int z = -r; z <= r; z++)
						if (!(x == 0 && y == 0 && z == 0))
							o.add(new Vec3i(x, y, z));
			offsets = o.toArray(Vec3i[]::new);
		}
		else
		{
			Direction[] dirs = Direction.values();
			offsets = new Vec3i[dirs.length];
			for (int i = 0; i < dirs.length; i++)
				offsets[i] = dirs[i].getNormal();
		}

		while (!posesToSearch.isEmpty())
		{
			Set<BlockPos> newPoses = new HashSet<>();
			for (BlockPos pos : posesToSearch)
			{
				if (pos.distManhattan(clickedPos) > radius)
					continue;
				allPositions.add(pos);
				for (Vec3i delta : offsets)
				{
					BlockPos offset = pos.offset(delta);
					if (!allPositions.contains(offset) && level.getBlockState(offset).is(clickedBlock))
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
