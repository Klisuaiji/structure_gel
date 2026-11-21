package com.legacy.structure_gel.core.item.building_tool;

import java.util.Optional;

import com.legacy.structure_gel.core.item.building_tool.SmartBoundingBox.CornerType;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class BuildingToolBounds
{
	public static final double CORNER_WIDTH = 0.30;
	public static final double CLICK_RANGE = 10;

	public static boolean handleBoundingBoxClick(ItemStack stack, Player player)
	{
		if (BuildingToolItem.getMode(stack) instanceof BuildingToolMode.ForCorners cornerMode && BuildingToolItem.hasCompleteSelection(stack))
		{
			CornerType selectedCorner = BuildingToolItem.getSelectedCorner(stack);
			BlockPos posA = BuildingToolItem.getPos(stack, 0).get();
			BlockPos posB = BuildingToolItem.getPos(stack, 1).get();
			Vec3 lookVec = player.getLookAngle();
			Vec3 playerPos = player.getEyePosition();

			if (selectedCorner == null)
			{
				Vec3 cornerWidthVec = new Vec3(CORNER_WIDTH, CORNER_WIDTH, CORNER_WIDTH);
				SmartBoundingBox bounds = SmartBoundingBox.fromCorners(posA, posB);
				for (SmartBoundingBox.Corner corner : cornerMode.hasAllCorners() ? bounds.allCorners() : bounds.selectionPointCorners())
				{
					Vec3i cornerPos = corner.getPoint();
					Vec3 c = new Vec3(cornerPos.getX(), cornerPos.getY(), cornerPos.getZ());

					AABB aabb = new AABB(c.subtract(cornerWidthVec), c.add(cornerWidthVec));
					Optional<Vec3> clickPos = aabb.clip(playerPos, playerPos.add(lookVec.multiply(CLICK_RANGE, CLICK_RANGE, CLICK_RANGE)));
					if (clickPos.isPresent())
					{
						boolean moveBounds = player.isShiftKeyDown();
						BuildingToolItem.setMovingBounds(stack, moveBounds);
						// Grab corner
						BuildingToolItem.setSelectedCorner(stack, corner.type);
						BuildingToolItem.setSelectedCornerDistance(stack, (float) playerPos.distanceTo(clickPos.get()));
						BuildingToolMode.sendMessage(player, moveBounds ? BuildingToolMode.GRABBED_BOUNDS_KEY : BuildingToolMode.GRABBED_CORNER_KEY);
						BuildingToolMode.playSound(player, BlockPos.containing(clickPos.get()), SoundEvents.ITEM_FRAME_REMOVE_ITEM);
						return true;
					}
				}
			}
			else
			{
				BlockPos pos = placeGrabbedCorner(stack, true, selectedCorner, player, posA, posB);
				BuildingToolItem.setSelectedCorner(stack, null);
				BuildingToolMode.playSound(player, pos, SoundEvents.ITEM_FRAME_ADD_ITEM);
				return true;
			}
		}

		return false;
	}

	public static boolean tick(ItemStack stack, Player player)
	{
		if (BuildingToolItem.getMode(stack) instanceof BuildingToolMode.ForCorners && BuildingToolItem.hasCompleteSelection(stack))
		{
			CornerType selectedCorner = BuildingToolItem.getSelectedCorner(stack);
			if (selectedCorner != null)
			{
				placeGrabbedCorner(stack, false, selectedCorner, player, BuildingToolItem.getPos(stack, 0).get(), BuildingToolItem.getPos(stack, 1).get());
				return true;
			}
		}
		return false;
	}

	public static BlockPos placeGrabbedCorner(ItemStack stack, boolean clickToRelease, CornerType selectedCorner, Player player, Vec3i posA, Vec3i posB)
	{
		float cornerDistance = BuildingToolItem.getSelectedCornerDistance(stack);
		SmartBoundingBox bb = SmartBoundingBox.fromCorners(posA, posB);
		Vec3 lookVec = player.getEyePosition().add(player.getLookAngle().multiply(cornerDistance, cornerDistance, cornerDistance));
		BlockPos newPos = BlockPos.containing(Math.round(lookVec.x) + (selectedCorner.xPositive ? -1 : 0), Math.round(lookVec.y) + (selectedCorner.yPositive ? -1 : 0), Math.round(lookVec.z) + (selectedCorner.zPositive ? -1 : 0));
		boolean movingBounds = BuildingToolItem.isMovingBounds(stack);
		if (movingBounds)
		{
			bb.moveTo(selectedCorner, newPos);
		}
		else
		{
			bb.setCorner(selectedCorner, newPos);
		}
		BuildingToolItem.setPos(stack, 0, bb.getPosA());
		BuildingToolItem.setPos(stack, 1, bb.getPosB());
		if (clickToRelease)
		{
			BuildingToolMode.sendMessage(player, movingBounds ? BuildingToolMode.PLACED_BOUNDS_KEY : BuildingToolMode.PLACED_CORNER_KEY);
			BuildingToolItem.getMode(stack).onReleaseCorner(stack, player.level(), player);
		}
		return newPos;
	}

	public static void releaseGrabbedCorner(ItemStack stack, Player player)
	{
		var mode = BuildingToolItem.getMode(stack);
		CornerType selectedCorner = BuildingToolItem.getSelectedCorner(stack);
		if (selectedCorner != null && mode instanceof BuildingToolMode.ForCorners && BuildingToolItem.hasCompleteSelection(stack))
		{
			placeGrabbedCorner(stack, false, selectedCorner, player, BuildingToolItem.getPos(stack, 0).get(), BuildingToolItem.getPos(stack, 1).get());
			BuildingToolItem.setSelectedCorner(stack, null);
			mode.onReleaseCorner(stack, player.level(), player);
		}
	}
}
