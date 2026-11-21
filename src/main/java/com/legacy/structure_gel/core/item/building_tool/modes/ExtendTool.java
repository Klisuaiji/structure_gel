package com.legacy.structure_gel.core.item.building_tool.modes;

import com.legacy.structure_gel.core.item.building_tool.ActionHistory;
import com.legacy.structure_gel.core.item.building_tool.BuildingToolItem;
import com.legacy.structure_gel.core.item.building_tool.BuildingToolMode;
import com.legacy.structure_gel.core.item.building_tool.ToolModeProperty;
import com.legacy.structure_gel.core.util.SGText;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.Direction.Axis;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class ExtendTool extends BuildingToolMode {
   public ExtendTool(String name, int modelIndex) {
      super(name, modelIndex);
   }

   @Override
   public boolean targetsSpecificPos() {
      return false;
   }

   @Override
   public void onLeftClick(Level level, Player player, BlockPos clickedPos, ItemStack stack, Direction clickedFace) {
      if (!level.isClientSide) {
         boolean causesBlockUpdates = BuildingToolItem.causesBlockUpdates(stack);
         ToolModeProperty.Replace replace = BuildingToolItem.getProperty(stack, ToolModeProperty.REPLACE_NOT_AIR_CLICKED);
         int radius = BuildingToolItem.getProperty(stack, ToolModeProperty.LARGE_RADIUS);
         boolean fuzzy = BuildingToolItem.getProperty(stack, ToolModeProperty.FUZZY_TRUE).value();
         Set<BlockPos> poses = getExtendPositions(level, clickedPos, clickedFace, replace, radius, fuzzy);
         if (!poses.isEmpty()) {
            ActionHistory.ActionBuilder action = ActionHistory.newAction(level, causesBlockUpdates);
            Direction originDir = clickedFace.getOpposite();

            for (BlockPos pos : poses) {
               BlockPos fromPos = pos.relative(originDir);
               BlockEntity blockE = level.getBlockEntity(fromPos);
               CompoundTag blockEntityNbt = blockE != null ? blockE.saveWithoutMetadata() : null;
               this.setBlock(level, pos, level.getBlockState(fromPos), causesBlockUpdates, blockEntityNbt, action);
            }

            ActionHistory.get(player).add(level, action);
            BuildingToolMode.sendPlaceMessage(player, ToolModeProperty.Replace.ALL, poses.size(), 1, level.getBlockState(clickedPos));
         }
      }
   }

   @Override
   public Object[] getDescArgs() {
      Options options = Minecraft.getInstance().options;
      String leftClick = SGText.keybindString(options.keyAttack);
      return new Object[]{leftClick};
   }

   @Override
   public void addProperties(List<ToolModeProperty<?>> properties) {
      properties.add(ToolModeProperty.LARGE_RADIUS);
      properties.add(ToolModeProperty.REPLACE_NOT_AIR_CLICKED);
      properties.add(ToolModeProperty.FUZZY_TRUE);
   }

   @Override
   public boolean hasBlockPalette() {
      return false;
   }

   public static Set<BlockPos> getExtendPositions(
      Level level, BlockPos clickedPos, Direction clickedFace, ToolModeProperty.Replace replace, int radius, boolean fuzzy
   ) {
      BlockState clickedState = level.getBlockState(clickedPos);
      if (!clickedState.isAir() && replace.shouldReplace(level, clickedState, clickedPos.relative(clickedFace))) {
         Block clickedBlock = clickedState.getBlock();
         Axis axis = clickedFace.getAxis();
         Set<BlockPos> allPositions = new HashSet<>();
         Set<BlockPos> posesToSearch = new HashSet<>();
         posesToSearch.add(clickedPos);
         List<Vec3i> dirs = new ArrayList<>(9);

         for (int a = -1; a <= 1; a++) {
            for (int b = -1; b <= 1; b++) {
               if (fuzzy || Math.abs(a) != 1 || Math.abs(b) != 1) {
                  Vec3i vec = switch (axis) {
                     case X -> new Vec3i(0, a, b);
                     case Y -> new Vec3i(a, 0, b);
                     case Z -> new Vec3i(a, b, 0);
                     default -> throw new IncompatibleClassChangeError();
                  };
                  dirs.add(vec);
               }
            }
         }

         while (!posesToSearch.isEmpty()) {
            Set<BlockPos> newPoses = new HashSet<>();

            for (BlockPos pos : posesToSearch) {
               if (pos.distManhattan(clickedPos) <= radius) {
                  allPositions.add(pos);

                  for (Vec3i dir : dirs) {
                     BlockPos offset = pos.offset(dir);
                     if (!allPositions.contains(offset)) {
                        BlockState offsetState = level.getBlockState(offset);
                        if (offsetState.is(clickedBlock) && replace.shouldReplace(level, offsetState, offset.relative(clickedFace))) {
                           newPoses.add(offset);
                        }
                     }
                  }
               }
            }

            posesToSearch = newPoses;
         }

         return allPositions.stream().map(p -> p.relative(clickedFace)).collect(Collectors.toSet());
      } else {
         return Collections.emptySet();
      }
   }
}
