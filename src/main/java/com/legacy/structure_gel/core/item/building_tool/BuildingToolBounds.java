package com.legacy.structure_gel.core.item.building_tool;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.core.Vec3i;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class BuildingToolBounds {
   public static final double CORNER_WIDTH = 0.3;
   public static final double CLICK_RANGE = 10.0;

   public static boolean handleBoundingBoxClick(ItemStack stack, Player player) {
      if (BuildingToolItem.getMode(stack) instanceof BuildingToolMode.ForCorners cornerMode && BuildingToolItem.hasCompleteSelection(stack)) {
         SmartBoundingBox.CornerType selectedCorner = BuildingToolItem.getSelectedCorner(stack);
         BlockPos posA = BuildingToolItem.getPos(stack, 0).get();
         BlockPos posB = BuildingToolItem.getPos(stack, 1).get();
         Vec3 lookVec = player.getLookAngle();
         Vec3 playerPos = player.getEyePosition();
         if (selectedCorner != null) {
            BlockPos pos = placeGrabbedCorner(stack, true, selectedCorner, player, posA, posB);
            BuildingToolItem.setSelectedCorner(stack, null);
            BuildingToolMode.playSound(player, pos, SoundEvents.ITEM_FRAME_ADD_ITEM);
            return true;
         }

         Vec3 cornerWidthVec = new Vec3(0.3, 0.3, 0.3);
         SmartBoundingBox bounds = SmartBoundingBox.fromCorners(posA, posB);

         for (SmartBoundingBox.Corner corner : cornerMode.hasAllCorners() ? bounds.allCorners() : bounds.selectionPointCorners()) {
            Vec3i cornerPos = corner.getPoint();
            Vec3 c = new Vec3(cornerPos.getX(), cornerPos.getY(), cornerPos.getZ());
            AABB aabb = new AABB(c.subtract(cornerWidthVec), c.add(cornerWidthVec));
            Optional<Vec3> clickPos = aabb.clip(playerPos, playerPos.add(lookVec.multiply(10.0, 10.0, 10.0)));
            if (clickPos.isPresent()) {
               boolean moveBounds = player.isShiftKeyDown();
               BuildingToolItem.setMovingBounds(stack, moveBounds);
               BuildingToolItem.setSelectedCorner(stack, corner.type);
               BuildingToolItem.setSelectedCornerDistance(stack, (float)playerPos.distanceTo(clickPos.get()));
               BuildingToolMode.sendMessage(
                  player, moveBounds ? "info.structure_gel.building_tool.message.grabbed_bounds" : "info.structure_gel.building_tool.message.grabbed_corner"
               );
               BuildingToolMode.playSound(player, BlockPos.containing((Position)clickPos.get()), SoundEvents.ITEM_FRAME_REMOVE_ITEM);
               return true;
            }
         }
      }

      return false;
   }

   public static boolean tick(ItemStack stack, Player player) {
      if (BuildingToolItem.getMode(stack) instanceof BuildingToolMode.ForCorners && BuildingToolItem.hasCompleteSelection(stack)) {
         SmartBoundingBox.CornerType selectedCorner = BuildingToolItem.getSelectedCorner(stack);
         if (selectedCorner != null) {
            placeGrabbedCorner(
               stack, false, selectedCorner, player, (Vec3i)BuildingToolItem.getPos(stack, 0).get(), (Vec3i)BuildingToolItem.getPos(stack, 1).get()
            );
            return true;
         }
      }

      return false;
   }

   public static BlockPos placeGrabbedCorner(
      ItemStack stack, boolean clickToRelease, SmartBoundingBox.CornerType selectedCorner, Player player, Vec3i posA, Vec3i posB
   ) {
      float cornerDistance = BuildingToolItem.getSelectedCornerDistance(stack);
      SmartBoundingBox bb = SmartBoundingBox.fromCorners(posA, posB);
      Vec3 lookVec = player.getEyePosition().add(player.getLookAngle().multiply(cornerDistance, cornerDistance, cornerDistance));
      BlockPos newPos = BlockPos.containing(
         Math.round(lookVec.x) + (selectedCorner.xPositive ? -1 : 0),
         Math.round(lookVec.y) + (selectedCorner.yPositive ? -1 : 0),
         Math.round(lookVec.z) + (selectedCorner.zPositive ? -1 : 0)
      );
      boolean movingBounds = BuildingToolItem.isMovingBounds(stack);
      if (movingBounds) {
         bb.moveTo(selectedCorner, newPos);
      } else {
         bb.setCorner(selectedCorner, newPos);
      }

      BuildingToolItem.setPos(stack, 0, bb.getPosA());
      BuildingToolItem.setPos(stack, 1, bb.getPosB());
      if (clickToRelease) {
         BuildingToolMode.sendMessage(
            player, movingBounds ? "info.structure_gel.building_tool.message.placed_bounds" : "info.structure_gel.building_tool.message.placed_corner"
         );
         BuildingToolItem.getMode(stack).onReleaseCorner(stack, player.level(), player);
      }

      return newPos;
   }

   public static void releaseGrabbedCorner(ItemStack stack, Player player) {
      BuildingToolMode mode = BuildingToolItem.getMode(stack);
      SmartBoundingBox.CornerType selectedCorner = BuildingToolItem.getSelectedCorner(stack);
      if (selectedCorner != null && mode instanceof BuildingToolMode.ForCorners && BuildingToolItem.hasCompleteSelection(stack)) {
         placeGrabbedCorner(
            stack, false, selectedCorner, player, (Vec3i)BuildingToolItem.getPos(stack, 0).get(), (Vec3i)BuildingToolItem.getPos(stack, 1).get()
         );
         BuildingToolItem.setSelectedCorner(stack, null);
         mode.onReleaseCorner(stack, player.level(), player);
      }
   }
}
