package com.legacy.structure_gel.core.item.building_tool.modes;

import java.util.List;
import java.util.Optional;

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
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedEntry.Wrapper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;

public class FillTool extends BuildingToolMode.ForCorners
{
	public FillTool(String name, int modelIndex)
	{
		super(name, modelIndex);
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
			boolean copyStates = BuildingToolItem.getProperty(stack, ToolModeProperty.COPY_STATES).value();
			double integrity = BuildingToolItem.getProperty(stack, ToolModeProperty.INTEGRITY);
			Shape shape = BuildingToolItem.getProperty(stack, ToolModeProperty.FILL_SHAPE);
			Replace replace = BuildingToolItem.getProperty(stack, ToolModeProperty.REPLACE);
			BlockState clickedState = level.getBlockState(clickedPos);
			var action = ActionHistory.newAction(level, causesBlockUpdates);
			RandomSource rand = level.random;
			BoundingBox bounds = BoundingBox.fromCorners(cornerA, cornerB);
			BlockPos minCorner = new BlockPos(bounds.minX(), bounds.minY(), bounds.minZ());
			BlockPos maxCorner = new BlockPos(bounds.maxX(), bounds.maxY(), bounds.maxZ());
			int xSpan = bounds.getXSpan();
			int ySpan = bounds.getYSpan();
			int zSpan = bounds.getZSpan();
			double dx = (xSpan - 1) / 2.0;
			double dy = (ySpan - 1) / 2.0;
			double dz = (zSpan - 1) / 2.0;
			Vec3 centerPos = Vec3.atLowerCornerOf(bounds.getCenter());
			int total = this.forPosesWithin(minCorner, maxCorner, pos ->
			{
				if (shape.isInside(centerPos.add(-pos.getX(), -pos.getY(), -pos.getZ()), dx, dy, dz) && rand.nextFloat() < integrity)
				{
					Optional<Wrapper<BlockState>> opState = palette.getRandom(level.random);
					if (opState.isPresent())
					{
						BlockState toPlace = opState.get().data();
						if (replace.shouldReplace(level, clickedState, pos))
						{
							return this.setBlock(level, pos, oldState -> copyStates ? IModifyState.mergeStates(toPlace, oldState) : toPlace, causesBlockUpdates, action);
						}
					}
				}
				return false;
			});
			ActionHistory.get(player).add(level, action);
			sendPlaceMessage(player, replace, total, palette);
		}
		else
		{
			BuildingToolMode.sendMessage(player, BuildingToolMode.MISSING_STATE_KEY, Style.EMPTY.withColor(ChatFormatting.RED));
		}
	}

	@Override
	public boolean onDelete(Level level, Player player, ItemStack stack)
	{
		boolean ret = true;
		ItemStack copy = stack.copy();
		var pos0 = BuildingToolItem.getPos(stack, 0);
		var pos1 = BuildingToolItem.getPos(stack, 1);
		if (!pos0.isPresent() || !pos1.isPresent())
			ret = false;
		BuildingToolItem.clearPalette(copy);
		BuildingToolItem.setProperty(copy, ToolModeProperty.REPLACE, ToolModeProperty.Replace.ALL);
		this.onLeftClick(level, player, player.blockPosition(), copy, Direction.UP);
		return ret;
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
		return new Object[] { middleClick, Component.translatable(BuildingToolMode.SELECT_CORNERS, rightClick, shift, rightClick, rightClick, shift), leftClick };
	}

	@Override
	public void addProperties(List<ToolModeProperty<?>> properties)
	{
		super.addProperties(properties);
		properties.add(ToolModeProperty.INTEGRITY);
		properties.add(ToolModeProperty.FILL_SHAPE);
		properties.add(ToolModeProperty.COPY_STATES);
		properties.add(ToolModeProperty.REPLACE);
	}

	@Override
	public boolean hasBlockPalette()
	{
		return true;
	}
}
