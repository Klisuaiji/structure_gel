package com.legacy.structure_gel.core.item.building_tool.modes;

import com.legacy.structure_gel.core.item.building_tool.ActionHistory;
import com.legacy.structure_gel.core.item.building_tool.BuildingToolItem;
import com.legacy.structure_gel.core.item.building_tool.BuildingToolMode;
import com.legacy.structure_gel.core.item.building_tool.ToolModeProperty;
import com.legacy.structure_gel.core.util.SGText;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.network.chat.Style;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.util.random.WeightedEntry.Wrapper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class FloodTool extends BuildingToolMode {
   public FloodTool(String name, int modelIndex) {
      super(name, modelIndex);
   }

   @Override
   public void onLeftClick(Level level, Player player, BlockPos clickedPos, ItemStack stack, Direction clickedFace) {
      if (!level.isClientSide) {
         WeightedRandomList<Wrapper<BlockState>> palette = BuildingToolItem.getPalette(stack, level);
         if (!palette.isEmpty()) {
            boolean causesBlockUpdates = BuildingToolItem.causesBlockUpdates(stack);
            boolean extendDown = BuildingToolItem.getProperty(stack, ToolModeProperty.EXTEND_DOWN).value();
            ToolModeProperty.Replace replace = BuildingToolItem.getProperty(stack, ToolModeProperty.REPLACE_NOT_AIR_CLICKED);
            int radius = BuildingToolItem.getProperty(stack, ToolModeProperty.LARGE_RADIUS);
            Set<BlockPos> poses = getFloodPositions(level, clickedPos, clickedFace, replace, radius);
            if (poses.isEmpty()) {
               return;
            }

            ActionHistory.ActionBuilder action = ActionHistory.newAction(level, causesBlockUpdates);
            int dist = extendDown ? 100 : 1;
            BlockState air = Blocks.AIR.defaultBlockState();
            int total = 0;

            for (BlockPos pos : poses) {
               for (MutableBlockPos mutPos = pos.mutable();
                  replace.shouldReplace(level, air, mutPos) && pos.getY() - mutPos.getY() < dist;
                  mutPos.move(Direction.DOWN)
               ) {
                  Optional<Wrapper<BlockState>> opState = palette.getRandom(level.random);
                  if (opState.isPresent() && this.setBlock(level, mutPos.immutable(), (BlockState)opState.get().getData(), causesBlockUpdates, action)) {
                     total++;
                  }
               }
            }

            ActionHistory.get(player).add(level, action);
            sendPlaceMessage(player, replace, total, palette);
         } else {
            BuildingToolMode.sendMessage(player, "info.structure_gel.building_tool.message.missing_state", Style.EMPTY.withColor(ChatFormatting.RED));
         }
      }
   }

   @Override
   public Object[] getDescArgs() {
      Options options = Minecraft.getInstance().options;
      String middleClick = SGText.keybindString(options.keyPickItem);
      String leftClick = SGText.keybindString(options.keyAttack);
      String rightClick = SGText.keybindString(options.keyUse);
      return new Object[]{middleClick, leftClick, rightClick};
   }

   @Override
   public void addProperties(List<ToolModeProperty<?>> properties) {
      super.addProperties(properties);
      properties.add(ToolModeProperty.EXTEND_DOWN);
      properties.add(ToolModeProperty.LARGE_RADIUS);
      properties.add(ToolModeProperty.REPLACE_NOT_AIR_CLICKED);
   }

   @Override
   public boolean hasBlockPalette() {
      return true;
   }

   public static Set<BlockPos> getFloodPositions(Level level, BlockPos clickedPos, Direction clickedFace, ToolModeProperty.Replace replace, int radius) {
      BlockPos startPos = clickedPos.relative(clickedFace);
      if (!replace.shouldReplace(level, level.getBlockState(startPos), startPos)) {
         return Collections.emptySet();
      }

      Set<BlockPos> allPositions = new HashSet<>();
      Set<BlockPos> posesToSearch = new HashSet<>();
      posesToSearch.add(startPos);
      Direction[] dirs = new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};

      while (!posesToSearch.isEmpty()) {
         Set<BlockPos> newPoses = new HashSet<>();

         for (BlockPos pos : posesToSearch) {
            if (pos.distManhattan(startPos) <= radius) {
               allPositions.add(pos);

               for (Direction dir : dirs) {
                  BlockPos offset = pos.relative(dir);
                  if (!allPositions.contains(offset) && replace.shouldReplace(level, level.getBlockState(offset), offset)) {
                     newPoses.add(offset);
                  }
               }
            }
         }

         posesToSearch = newPoses;
      }

      return allPositions;
   }
}
