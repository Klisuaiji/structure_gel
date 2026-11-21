package com.legacy.structure_gel.core.asm_hooks;

import com.legacy.structure_gel.core.SGConfig;
import com.legacy.structure_gel.core.client.ClientUtil;
import com.legacy.structure_gel.core.util.SGText;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.block.state.properties.StructureMode;

public class StructureBlockRendererHooks {
   public static void renderName(StructureBlockEntity structureBlock, PoseStack posStack, MultiBufferSource buffer) {
      if (SGConfig.CLIENT.showStructureBlockInfo()) {
         Minecraft mc = Minecraft.getInstance();
         if (structureBlock.getLevel() != null
            && mc.player != null
            && ClientUtil.rayTrace(structureBlock.getLevel(), mc.player).getBlockPos().equals(structureBlock.getBlockPos())) {
            String name = structureBlock.getMode().getSerializedName();
            MutableComponent mode = Component.translatable("structure_block.mode." + name).setStyle(SGText.VALUE_LABEL_STYLE).append(": ");
            MutableComponent text = Component.literal(
               structureBlock.getMode() == StructureMode.DATA ? structureBlock.getMetaData() : structureBlock.getStructureName()
            );
            ClientUtil.renderName(Component.empty().append(mode).append(text), structureBlock.getLevel(), structureBlock.getBlockPos(), posStack, buffer, 220);
         }
      }
   }
}
