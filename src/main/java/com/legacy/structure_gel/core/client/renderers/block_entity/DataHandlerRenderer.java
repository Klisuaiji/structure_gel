package com.legacy.structure_gel.core.client.renderers.block_entity;

import com.legacy.structure_gel.api.data_handler.handlers.DataHandler;
import com.legacy.structure_gel.core.SGConfig;
import com.legacy.structure_gel.core.block.DataHandlerBlock;
import com.legacy.structure_gel.core.block_entity.DataHandlerBlockEntity;
import com.legacy.structure_gel.core.client.ClientUtil;
import com.legacy.structure_gel.core.client.renderers.IRenderBase;
import com.legacy.structure_gel.core.util.SGText;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class DataHandlerRenderer implements BlockEntityRenderer<DataHandlerBlockEntity> {
   public DataHandlerRenderer(Context renderContext) {
   }

   public void render(DataHandlerBlockEntity dataHandler, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int overlay) {
      Minecraft mc = Minecraft.getInstance();
      if (SGConfig.CLIENT.showStructureBlockInfo()
         && dataHandler.getLevel() != null
         && mc.player != null
         && ClientUtil.rayTrace(dataHandler.getLevel(), mc.player).getBlockPos().equals(dataHandler.getBlockPos())) {
         if (dataHandler.getCustomName() == null) {
            List<DataHandlerBlockEntity.RawHandler> handlers = dataHandler.getHandlers().unwrap();
            MutableComponent mode = SGText.TYPE_LABEL.copy().setStyle(SGText.VALUE_LABEL_STYLE).append(": ");
            MutableComponent text;
            if (handlers.size() == 0) {
               text = Component.empty();
            } else if (handlers.size() == 1) {
               text = Component.literal(handlers.get(0).typeName().toString());
            } else {
               text = Component.translatable("gui.structure_gel.data_handler.compound");
            }

            ClientUtil.renderName(Component.empty().append(mode).append(text), dataHandler.getLevel(), dataHandler.getBlockPos(), poseStack, buffer, 220);
         } else {
            ClientUtil.renderName(dataHandler.getCustomName(), dataHandler.getLevel(), dataHandler.getBlockPos(), poseStack, buffer, 220);
         }
      }

      Vec3 offset = dataHandler.getOffset();
      if (!Vec3.ZERO.equals(offset)) {
         BlockState state = dataHandler.getBlockState();
         Vec3 exactPos = Vec3.atBottomCenterOf(BlockPos.ZERO)
            .add(DataHandler.offsetPosition(offset, (Direction)state.getValue(DataHandlerBlock.FACING), (Mirror)state.getValue(DataHandlerBlock.MIRROR)));
         double r2 = 0.05;
         Vec3 rOffset2 = new Vec3(r2, r2, r2);
         IRenderBase.makeBox(
            poseStack, buffer.getBuffer(RenderType.debugSectionQuads()), exactPos.subtract(rOffset2), exactPos.add(rOffset2), 0.5F, 0.7F, 0.9F, 1.0F
         );
         double r1 = 0.51;
         Vec3 rOffset1 = new Vec3(r1, r1, r1);
         Vec3 blockPos = Vec3.atCenterOf(BlockPos.containing(exactPos));
         IRenderBase.makeBox(
            poseStack, buffer.getBuffer(RenderType.debugSectionQuads()), blockPos.subtract(rOffset1), blockPos.add(rOffset1), 0.0F, 0.3F, 0.7F, 0.4F
         );
      }
   }

   public boolean shouldRenderOffScreen(DataHandlerBlockEntity dataHandler) {
      return true;
   }

   public AABB getRenderBoundingBox(DataHandlerBlockEntity dataHandler) {
      Vec3 offset = dataHandler.getOffset();
      if (Vec3.ZERO.equals(offset)) {
         return super.getRenderBoundingBox(dataHandler);
      }

      double size = Math.max(Math.max(Math.abs(offset.x()), Math.abs(offset.y())), Math.abs(offset.z())) + 3.0;
      return AABB.ofSize(Vec3.atCenterOf(dataHandler.getBlockPos()), size, size, size);
   }
}
