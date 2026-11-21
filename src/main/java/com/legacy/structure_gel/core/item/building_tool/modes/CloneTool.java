package com.legacy.structure_gel.core.item.building_tool.modes;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import com.legacy.structure_gel.core.item.building_tool.ActionHistory;
import com.legacy.structure_gel.core.item.building_tool.BuildingToolItem;
import com.legacy.structure_gel.core.item.building_tool.BuildingToolMode;
import com.legacy.structure_gel.core.item.building_tool.BuildingToolMode.ForCorners;
import com.legacy.structure_gel.core.item.building_tool.CapturedBlocks;
import com.legacy.structure_gel.core.item.building_tool.ToolModeProperty;
import com.legacy.structure_gel.core.item.building_tool.ToolModeProperty.Replace;
import com.legacy.structure_gel.core.util.SGText;
import com.mojang.datafixers.util.Pair;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;

public class CloneTool extends ForCorners
{
	public CloneTool(String name, int modelIndex)
	{
		super(name, modelIndex);
	}

	@Override
	protected void performAction(Level level, Player player, BlockPos clickedPos, ItemStack stack, BlockPos cornerA, BlockPos cornerB)
	{
		if (!(level instanceof ServerLevel serverLevel))
			return;
		RandomSource rand = level.getRandom();
		boolean causesBlockUpdates = BuildingToolItem.causesBlockUpdates(stack);
		double integrity = BuildingToolItem.getProperty(stack, ToolModeProperty.INTEGRITY);
		Rotation rotation = BuildingToolItem.getProperty(stack, ToolModeProperty.ROTATION).toVanilla(rand);
		Mirror mirror = BuildingToolItem.getProperty(stack, ToolModeProperty.MIRROR).toVanilla(rand);
		boolean cut = BuildingToolItem.getProperty(stack, ToolModeProperty.CUT_FALSE).value();
		Replace replace = BuildingToolItem.getProperty(stack, ToolModeProperty.REPLACE_NOT_CLICKED);
		CapturedBlocks captured = BuildingToolItem.getCapturedBlocks(stack, level, player);
		if (captured == null)
		{
			BuildingToolMode.sendMessage(player, CLIPBOARD_MISSING_KEY, Style.EMPTY.withColor(ChatFormatting.RED));
			return;
		}
		captured = captured.withTransforms(mirror, rotation);

		var action = ActionHistory.newAction(level, causesBlockUpdates);

		if (cut)
		{
			BlockState air = Blocks.AIR.defaultBlockState();
			this.forPosesWithin(cornerA, cornerB, pos -> this.setBlock(level, pos, air, causesBlockUpdates, action));
			this.clearPoses(stack, player);
		}

		BoundingBox destBB = getCloneDestBounds(captured, clickedPos, level.getBlockState(clickedPos), player.position(), player.isShiftKeyDown());

		BlockPos startPos = new BlockPos(destBB.minX(), destBB.minY(), destBB.minZ());
		for (CapturedBlocks.BlockInfo info : captured.getBlockInfos())
		{
			if (rand.nextFloat() < integrity)
			{
				BlockPos placePos = startPos.offset(info.pos());
				BlockState placeState = info.state();
				if (replace.shouldReplace(level, placeState, placePos))
					this.setBlock(level, placePos, old -> info.state(), causesBlockUpdates, info.blockEntityTag().orElse(null), action, captured.getMirror(), captured.getRotation());
			}
		}

		for (CapturedBlocks.EntityInfo info : captured.getEntityInfos())
		{
			Entity entity = info.createEntity(level, startPos, mirror, rotation);
			if (entity != null)
			{
				this.addEntity(level, entity, action);
				if (cut)
					this.removeEntity(serverLevel, info.original(), action);
			}
		}

		ActionHistory.get(player).add(level, action);
		BuildingToolMode.sendMessage(player, CLONE_BLOCKS_KEY);
		BuildingToolMode.playSound(player, SoundEvents.NETHERITE_BLOCK_PLACE);
	}

	@SuppressWarnings("resource")
	@Override
	public Object[] getDescArgs()
	{
		net.minecraft.client.Options options = net.minecraft.client.Minecraft.getInstance().options;
		var shift = SGText.keybindString(options.keyShift);
		var rightClick = SGText.keybindString(options.keyUse);
		var leftClick = SGText.keybindString(options.keyAttack);
		var copy = SGText.keybindString(com.legacy.structure_gel.core.client.ClientProxy.COPY_KEY.get());
		var paste = SGText.keybindString(com.legacy.structure_gel.core.client.ClientProxy.PASTE_KEY.get());
		return new Object[] { Component.translatable(SELECT_CORNERS, rightClick, shift, rightClick, rightClick, shift), leftClick, paste, copy};
	}

	@Override
	public void addProperties(List<ToolModeProperty<?>> properties)
	{
		super.addProperties(properties);
		properties.add(ToolModeProperty.INTEGRITY);
		properties.add(ToolModeProperty.ROTATION);
		properties.add(ToolModeProperty.MIRROR);
		properties.add(ToolModeProperty.CUT_FALSE);
		properties.add(ToolModeProperty.REPLACE_NOT_CLICKED);
		properties.add(ToolModeProperty.INCLUDE_ENTITIES);
	}

	@Override
	public boolean hasBlockPalette()
	{
		return false;
	}

	public static void saveCapturedBlocks(ItemStack stack, Level level, Player player)
	{
		// If both positions are set, save the selected region to the tool
		var pos0 = BuildingToolItem.getPos(stack, 0);
		var pos1 = BuildingToolItem.getPos(stack, 1);
		if (pos0.isPresent() && pos1.isPresent())
		{
			CapturedBlocks captured = new CapturedBlocks(level, pos0.get(), pos1.get(), Mirror.NONE, Rotation.NONE, BuildingToolItem.getProperty(stack, ToolModeProperty.INCLUDE_ENTITIES).value());
			BuildingToolItem.setCapturedBlocks(stack, level, player, Pair.of(BoundingBox.fromCorners(pos0.get(), pos1.get()), captured))	;
		}
	}

	@Override
	public void setPosition(Player player, BlockPos clickedPos, ItemStack stack, boolean message)
	{
		super.setPosition(player, clickedPos, stack, message);
		saveCapturedBlocks(stack, player.level(), player);
	}

	@Override
	public void onSelect(ItemStack stack, Level level, Player player)
	{
		saveCapturedBlocks(stack, level, player);
	}

	@Override
	public void clearPoses(ItemStack stack, Player player)
	{
		super.clearPoses(stack, player);
		BuildingToolItem.setCapturedBlocks(stack, player.level(), player, null);
	}

	@Override
	public boolean requiresSelectionLoaded()
	{
		return false;
	}

	@Override
	public void onReleaseCorner(ItemStack stack, Level level, Player player)
	{
		saveCapturedBlocks(stack, level, player);
	}

	public static BoundingBox getCloneDestBounds(CapturedBlocks captured, BlockPos targetPos, BlockState targetedBlock, Vec3 playerPos, boolean placeOneBlockAbove)
	{
		Vec3i bbLength = captured.getBounds().getLength();
		BlockPos start = targetPos.offset(-bbLength.getX() + bbLength.getX() / 2, 0, -bbLength.getZ() + bbLength.getZ() / 2);
		if (placeOneBlockAbove)
			start = start.above();
		BoundingBox destBB = BoundingBox.fromCorners(start, start.offset(bbLength));
		int x = destBB.minX();
		int z = destBB.minZ();
		int dx = destBB.maxX();
		int dz = destBB.maxZ();

		LinkedList<Vec3i> edgePositions = new LinkedList<>();
		edgePositions.add(new Vec3i(x, 0, z));
		edgePositions.add(new Vec3i(x, 0, dz));
		edgePositions.add(new Vec3i(dx, 0, z));
		edgePositions.add(new Vec3i(dx, 0, dz));
		edgePositions.sort(Comparator.comparingDouble(v -> new Vec3(v.getX() + 0.5, v.getY(), v.getZ() + 0.5).distanceTo(playerPos)));
		Vec3i closestCorner = edgePositions.getFirst();

		Vec3i center = destBB.getCenter();
		return destBB.moved(center.getX() - closestCorner.getX(), 0, center.getZ() - closestCorner.getZ());
	}
}
