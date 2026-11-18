package com.legacy.structure_gel.core.client.renderers.block_entity;

import com.legacy.structure_gel.core.SGConfig;
import com.legacy.structure_gel.core.client.ClientUtil;
import com.legacy.structure_gel.core.util.SGText;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.block.entity.JigsawBlockEntity;

public class GelJigsawRenderer implements BlockEntityRenderer<JigsawBlockEntity> {
   public GelJigsawRenderer(Context renderContext) {
   }

   public void render(
      JigsawBlockEntity jigsaw, float partialTicks, PoseStack poseStack, MultiBufferSource renderBuffer, int combinedLightIn, int combinedOverlayIn
   ) {
      if (SGConfig.CLIENT.showStructureBlockInfo()) {
         Minecraft mc = Minecraft.getInstance();
         if (jigsaw.getLevel() != null && mc.player != null && ClientUtil.rayTrace(jigsaw.getLevel(), mc.player).getBlockPos().equals(jigsaw.getBlockPos())) {
            MutableComponent mode = Component.literal("Pool: ").setStyle(SGText.VALUE_LABEL_STYLE);
            MutableComponent text = Component.literal(jigsaw.getPool().location().toString());
            ClientUtil.renderName(Component.empty().append(mode).append(text), jigsaw.getLevel(), jigsaw.getBlockPos(), poseStack, renderBuffer, 220);
         }
      }
   }
}
