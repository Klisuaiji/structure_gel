package com.legacy.structure_gel.core.item.building_tool.modes;

import com.legacy.structure_gel.core.item.building_tool.ActionHistory;
import com.legacy.structure_gel.core.item.building_tool.BuildingToolItem;
import com.legacy.structure_gel.core.item.building_tool.BuildingToolMode;
import com.legacy.structure_gel.core.item.building_tool.ToolModeProperty;
import com.legacy.structure_gel.core.util.SGText;
import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.util.random.WeightedEntry.Wrapper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class LineTool extends BuildingToolMode.ForCorners {
   public LineTool(String name, int modelIndex) {
      super(name, modelIndex);
   }

   @Override
   public boolean hasAllCorners() {
      return false;
   }

   @Override
   protected void performAction(Level level, Player player, BlockPos clickedPos, ItemStack stack, BlockPos cornerA, BlockPos cornerB) {
      if (!level.isClientSide) {
         WeightedRandomList<Wrapper<BlockState>> palette = BuildingToolItem.getPalette(stack, level);
         if (!palette.isEmpty()) {
            boolean causesBlockUpdates = BuildingToolItem.causesBlockUpdates(stack);
            double integrity = BuildingToolItem.getProperty(stack, ToolModeProperty.INTEGRITY);
            ToolModeProperty.Replace replace = BuildingToolItem.getProperty(stack, ToolModeProperty.REPLACE);
            BlockState clickedState = level.getBlockState(clickedPos);
            ActionHistory.ActionBuilder action = ActionHistory.newAction(level, causesBlockUpdates);
            RandomSource rand = level.getRandom();
            Set<BlockPos> poses = getLinePositions(cornerA, cornerB);
            int total = 0;

            for (BlockPos pos : poses) {
               if (rand.nextFloat() < integrity && replace.shouldReplace(level, clickedState, pos)) {
                  Optional<Wrapper<BlockState>> opState = palette.getRandom(rand);
                  if (opState.isPresent() && this.setBlock(level, pos, (BlockState)opState.get().getData(), causesBlockUpdates, action)) {
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
      String shift = SGText.keybindString(options.keyShift);
      String middleClick = SGText.keybindString(options.keyPickItem);
      String rightClick = SGText.keybindString(options.keyUse);
      String leftClick = SGText.keybindString(options.keyAttack);
      return new Object[]{
         middleClick, Component.translatable("item.structure_gel.building_tool.mode.select_positions", new Object[]{rightClick, shift, rightClick}), leftClick
      };
   }

   @Override
   public void addProperties(List<ToolModeProperty<?>> properties) {
      super.addProperties(properties);
      properties.add(ToolModeProperty.INTEGRITY);
      properties.add(ToolModeProperty.REPLACE);
   }

   @Override
   public boolean hasBlockPalette() {
      return true;
   }

   @Override
   public Component getHudMessage(ItemStack stack) {
      Optional<BlockPos> p1 = BuildingToolItem.getPos(stack, 0);
      Optional<BlockPos> p2 = BuildingToolItem.getPos(stack, 1);
      if (p1.isPresent() && p2.isPresent()) {
         DecimalFormat format = new DecimalFormat("0.##");
         Double dist = p1.get().getCenter().distanceTo(p2.get().getCenter()) + 1.0;
         return Component.translatable(
            "info.structure_gel.building_tool.message.length",
            new Object[]{
               Component.literal(format.format(dist) + "").withStyle(SGText.VALUE_LABEL_STYLE),
               Component.literal(getLinePositions(p1.get(), p2.get()).size() + "").withStyle(SGText.VALUE_LABEL_STYLE)
            }
         );
      } else {
         return null;
      }
   }

   public static Set<BlockPos> getLinePositions(BlockPos cornerA, BlockPos cornerB) {
      Set<BlockPos> poses = new HashSet<>();
      double magnitude = Math.sqrt(cornerA.distSqr(cornerB));
      if (magnitude == 0.0) {
         poses.add(cornerA);
         return poses;
      }

      int x = cornerA.getX();
      int y = cornerA.getY();
      int z = cornerA.getZ();
      double dX = (x - cornerB.getX()) / magnitude;
      double dY = (y - cornerB.getY()) / magnitude;
      double dZ = (z - cornerB.getZ()) / magnitude;
      BlockPos pos = cornerA;
      int i = 0;

      while (!pos.equals(cornerB) && i < magnitude + 1.0) {
         pos = cornerA.offset((int)(-Math.round(dX * i)), (int)(-Math.round(dY * i)), (int)(-Math.round(dZ * i)));
         i++;
         poses.add(pos);
      }

      return poses;
   }
}
