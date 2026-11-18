package com.legacy.structure_gel.core.item.building_tool.modes;

import com.legacy.structure_gel.core.item.building_tool.ActionHistory;
import com.legacy.structure_gel.core.item.building_tool.BuildingToolItem;
import com.legacy.structure_gel.core.item.building_tool.BuildingToolMode;
import com.legacy.structure_gel.core.item.building_tool.CapturedBlocks;
import com.legacy.structure_gel.core.item.building_tool.ToolModeProperty;
import com.legacy.structure_gel.core.util.SGText;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;

public class MoveTool extends BuildingToolMode.ForCorners {
   public MoveTool(String name, int modelIndex) {
      super(name, modelIndex);
   }

   @Override
   public boolean targetsSpecificPos() {
      return false;
   }

   @Override
   public int getSpamDelay() {
      return 3;
   }

   @Override
   protected void performAction(Level level, Player player, BlockPos clickedPos, ItemStack stack, BlockPos cornerA, BlockPos cornerB) {
      if (level instanceof ServerLevel serverLevel) {
         boolean causesBlockUpdates = BuildingToolItem.causesBlockUpdates(stack);
         int dist = BuildingToolItem.getProperty(stack, ToolModeProperty.MOVE_DISTANCE);
         boolean cut = BuildingToolItem.getProperty(stack, ToolModeProperty.CUT_TRUE).value();
         ToolModeProperty.Replace replace = BuildingToolItem.getProperty(stack, ToolModeProperty.REPLACE_NOT_CLICKED);
         boolean includeEntities = BuildingToolItem.getProperty(stack, ToolModeProperty.INCLUDE_ENTITIES).value();
         Direction facing = Direction.orderedByNearest(player)[0];
         if (player.isShiftKeyDown()) {
            facing = facing.getOpposite();
         }

         CapturedBlocks captured = new CapturedBlocks(level, cornerA, cornerB, includeEntities);
         ActionHistory.ActionBuilder action = ActionHistory.newAction(level, causesBlockUpdates);
         BlockState air = Blocks.AIR.defaultBlockState();
         if (cut) {
            this.forPosesWithin(cornerA, cornerB, pos -> this.setBlock(level, pos, air, causesBlockUpdates, action));
         }

         int total = 0;
         BlockPos placePos = captured.getWorldPos();

         for (CapturedBlocks.BlockInfo info : captured.getBlockInfos()) {
            BlockPos placeAt = placePos.offset(info.pos()).relative(facing, dist);
            if (replace.shouldReplace(level, air, placeAt)
               && this.setBlock(
                  level,
                  placeAt,
                  old -> info.state(),
                  causesBlockUpdates,
                  info.blockEntityTag().orElse(null),
                  action,
                  captured.getMirror(),
                  captured.getRotation()
               )) {
               total++;
            }
         }

         for (CapturedBlocks.EntityInfo infox : captured.getEntityInfos()) {
            Entity entity = infox.createEntity(level, placePos.relative(facing, dist), Mirror.NONE, Rotation.NONE);
            if (entity != null) {
               this.addEntity(level, entity, action);
               if (cut) {
                  this.removeEntity(serverLevel, infox.original(), action);
               }
            }
         }

         action.changeSelection(this, 0, cornerA, cornerA.relative(facing, dist));
         BuildingToolItem.setPos(stack, 0, cornerA.relative(facing, dist));
         action.changeSelection(this, 1, cornerB, cornerB.relative(facing, dist));
         BuildingToolItem.setPos(stack, 1, cornerB.relative(facing, dist));
         ActionHistory.get(player).add(level, action);
         BuildingToolMode.sendMessage(player, "info.structure_gel.building_tool.message.move_blocks", total, facing.getSerializedName());
         BuildingToolMode.playSound(player, SoundEvents.PISTON_EXTEND);
      }
   }

   @Override
   public Object[] getDescArgs() {
      Options options = Minecraft.getInstance().options;
      String shift = SGText.keybindString(options.keyShift);
      String rightClick = SGText.keybindString(options.keyUse);
      String leftClick = SGText.keybindString(options.keyAttack);
      return new Object[]{
         Component.translatable("item.structure_gel.building_tool.mode.select_corners", new Object[]{rightClick, shift, rightClick, rightClick, shift}),
         leftClick
      };
   }

   @Override
   public void addProperties(List<ToolModeProperty<?>> properties) {
      super.addProperties(properties);
      properties.add(ToolModeProperty.MOVE_DISTANCE);
      properties.add(ToolModeProperty.CUT_TRUE);
      properties.add(ToolModeProperty.REPLACE_NOT_CLICKED);
      properties.add(ToolModeProperty.INCLUDE_ENTITIES);
   }

   @Override
   public boolean hasBlockPalette() {
      return false;
   }
}
