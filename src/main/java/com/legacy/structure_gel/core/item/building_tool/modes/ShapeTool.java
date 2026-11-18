package com.legacy.structure_gel.core.item.building_tool.modes;

import com.legacy.structure_gel.api.structure.base.IModifyState;
import com.legacy.structure_gel.core.item.building_tool.ActionHistory;
import com.legacy.structure_gel.core.item.building_tool.BuildingToolItem;
import com.legacy.structure_gel.core.item.building_tool.BuildingToolMode;
import com.legacy.structure_gel.core.item.building_tool.ToolModeProperty;
import com.legacy.structure_gel.core.util.SGText;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Style;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.util.random.WeightedEntry.Wrapper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class ShapeTool extends BuildingToolMode {
   public ShapeTool(String name, int modelIndex) {
      super(name, modelIndex);
   }

   @Override
   public void onLeftClick(Level level, Player player, BlockPos clickedPos, ItemStack stack, Direction clickedFace) {
      if (!level.isClientSide) {
         WeightedRandomList<Wrapper<BlockState>> palette = BuildingToolItem.getPalette(stack, level);
         if (!palette.isEmpty()) {
            boolean causesBlockUpdates = BuildingToolItem.causesBlockUpdates(stack);
            double integrity = BuildingToolItem.getProperty(stack, ToolModeProperty.INTEGRITY);
            int radius = BuildingToolItem.getProperty(stack, ToolModeProperty.RADIUS);
            ToolModeProperty.Shape shape = BuildingToolItem.getProperty(stack, ToolModeProperty.SHAPE);
            ToolModeProperty.Replace replace = BuildingToolItem.getProperty(stack, ToolModeProperty.REPLACE);
            BlockState clickedState = level.getBlockState(clickedPos);
            ActionHistory.ActionBuilder action = ActionHistory.newAction(level, causesBlockUpdates);
            RandomSource rand = level.getRandom();
            int total = this.forPosesWithin(
               clickedPos.offset(-radius, -radius, -radius),
               clickedPos.offset(radius, radius, radius),
               pos -> {
                  if (rand.nextFloat() < integrity
                     && shape.isInside(clickedPos.offset(-pos.getX(), -pos.getY(), -pos.getZ()), radius)
                     && replace.shouldReplace(level, clickedState, pos)) {
                     Optional<Wrapper<BlockState>> opState = palette.getRandom(rand);
                     if (opState.isPresent()) {
                        return this.setBlock(
                           level,
                           pos,
                           oldState -> replace == ToolModeProperty.Replace.CLICKED_BLOCK
                              ? IModifyState.mergeStates((BlockState)opState.get().getData(), oldState)
                              : (BlockState)opState.get().getData(),
                           causesBlockUpdates,
                           action
                        );
                     }
                  }

                  return false;
               }
            );
            ActionHistory.get(player).add(level, action);
            sendPlaceMessage(player, replace, total, palette);
         } else {
            BuildingToolMode.sendMessage(player, "info.structure_gel.building_tool.message.missing_state", Style.EMPTY.withColor(ChatFormatting.RED));
         }
      }
   }

   @Override
   public int getReachDistance(ItemStack stack) {
      int radius = BuildingToolItem.getProperty(stack, ToolModeProperty.RADIUS);
      return 2 + radius;
   }

   @Override
   public Object[] getDescArgs() {
      Options options = Minecraft.getInstance().options;
      String middleClick = SGText.keybindString(options.keyPickItem);
      String leftClick = SGText.keybindString(options.keyAttack);
      return new Object[]{middleClick, leftClick};
   }

   @Override
   public void addProperties(List<ToolModeProperty<?>> properties) {
      super.addProperties(properties);
      properties.add(ToolModeProperty.INTEGRITY);
      properties.add(ToolModeProperty.RADIUS);
      properties.add(ToolModeProperty.SHAPE);
      properties.add(ToolModeProperty.REPLACE);
   }

   @Override
   public boolean hasBlockPalette() {
      return true;
   }
}
