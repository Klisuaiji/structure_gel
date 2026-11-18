package com.legacy.structure_gel.core.mixin.client;

import com.legacy.structure_gel.core.SGConfig;
import com.legacy.structure_gel.core.registry.SGRegistry;
import java.util.Locale;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Component.Serializer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@OnlyIn(Dist.CLIENT)
@Mixin({Item.class})
public class ItemMixin {
   @Inject(
      at = {@At("HEAD")},
      method = {"getName"},
      cancellable = true
   )
   private void getDisplayName(ItemStack stack, CallbackInfoReturnable<Component> callback) {
      if (SGConfig.CLIENT.showStructureBlockInfo()) {
         Item item = stack.getItem();

         try {
            if (item == Items.STRUCTURE_BLOCK || item == Items.JIGSAW || item == SGRegistry.Blocks.DATA_HANDLER.map(Block::asItem)) {
               if (stack.hasCustomHoverName()) {
                  return;
               }

               CompoundTag blockEntityTag = stack.getTagElement("BlockEntityTag");
               if (blockEntityTag != null) {
                  Component displayName = null;
                  if (item == Items.STRUCTURE_BLOCK) {
                     String mode = blockEntityTag.contains("mode") ? blockEntityTag.getString("mode").toLowerCase(Locale.ENGLISH) : "load";
                     String text = mode.equals("data")
                        ? (blockEntityTag.contains("metadata") ? blockEntityTag.getString("metadata") : "")
                        : (blockEntityTag.contains("name") ? blockEntityTag.getString("name") : "");
                     displayName = Component.translatable("structure_block.mode." + mode)
                        .append(
                           Component.literal(text.isBlank() ? " " + Component.translatable(item.getDescriptionId(stack)).getString() : ": \"" + text + "\"")
                        );
                  } else if (item == Items.JIGSAW) {
                     String text = blockEntityTag.contains("pool") ? blockEntityTag.getString("pool") : "null";
                     displayName = Component.literal("Pool: \"" + text + "\"");
                  } else if (item == SGRegistry.Blocks.DATA_HANDLER.get().asItem() && blockEntityTag.contains("CustomName", 8)) {
                     displayName = Serializer.fromJson(blockEntityTag.getString("CustomName"));
                  }

                  if (displayName != null) {
                     callback.setReturnValue(displayName);
                  }
               }
            }
         } catch (Exception var8) {
         }
      }
   }
}
