package com.legacy.structure_gel.api.item;

import com.google.common.collect.UnmodifiableIterator;
import com.legacy.structure_gel.api.block.StructureGelBlock;
import com.legacy.structure_gel.api.block.base.IStructureGel;
import com.legacy.structure_gel.core.util.SGText;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.GameMasterBlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class StructureGelItem extends GameMasterBlockItem {
   public StructureGelItem(StructureGelBlock structureGelBlock, @Nullable Properties properties) {
      super(
         structureGelBlock,
         properties == null
            ? new Properties().stacksTo(structureGelBlock.behaviors.contains(IStructureGel.Behavior.DYNAMIC_SPREAD_DIST) ? 50 : 64).rarity(Rarity.EPIC)
            : properties
      );
   }

   public StructureGelItem(StructureGelBlock structureGelBlock) {
      this(structureGelBlock, null);
   }

   public InteractionResult place(BlockPlaceContext context) {
      return !context.getPlayer().isCreative() ? InteractionResult.FAIL : super.place(context);
   }

   @OnlyIn(Dist.CLIENT)
   public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag advancedTooltips) {
      if (!Screen.hasShiftDown()) {
         tooltip.add(SGText.applyKeybindFilter(Component.translatable("info.structure_gel.hold_shift")));
      } else if (this.getBlock() instanceof StructureGelBlock structureGel) {
         tooltip.add(Component.translatable("info.structure_gel.place").withStyle(ChatFormatting.GRAY));
         tooltip.add(Component.translatable("info.structure_gel.gunpowder").withStyle(ChatFormatting.GRAY));
         tooltip.add(Component.literal(""));
         if (structureGel.behaviors.isEmpty()) {
            tooltip.add(Component.translatable(IStructureGel.Behavior.DEFAULT.getTranslation()).withStyle(ChatFormatting.GRAY));
         }

         UnmodifiableIterator var8 = structureGel.behaviors.iterator();

         while (var8.hasNext()) {
            IStructureGel.Behavior behavior = (IStructureGel.Behavior)var8.next();
            tooltip.add(Component.translatable(behavior.getTranslation()).withStyle(ChatFormatting.GRAY));
         }
      } else {
         tooltip.add(Component.translatable("info.structure_gel.unknown_behavior").withStyle(ChatFormatting.RED));
      }
   }
}
