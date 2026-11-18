package com.legacy.structure_gel.core.client.renderers;

import com.legacy.structure_gel.api.util.Positions;
import com.legacy.structure_gel.core.StructureGelMod;
import com.legacy.structure_gel.core.structure.StructureInfo;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexBuffer.Usage;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.joml.Matrix4f;

public class StructureBoundsRenderer implements IRenderBase {
   private static final Map<ResourceLocation, List<StructureBoundsRenderer>> STRUCTURE_INFOS = new HashMap<>();
   private static boolean shouldClear = false;
   private final BlockPos pos;
   private final StructureInfo structureInfo;
   @Nullable
   private VertexBuffer lineBuffer;
   private boolean needsUpdated = true;

   private StructureBoundsRenderer(StructureInfo structureInfo) {
      this.pos = structureInfo.fullBounds().getCenter();
      this.structureInfo = structureInfo;
   }

   public static void render(Minecraft mc, PoseStack poseStack, Matrix4f projectionMatrix, double camX, double camY, double camZ) {
      if (shouldClear) {
         shouldClear = false;

         for (List<StructureBoundsRenderer> rendererList : STRUCTURE_INFOS.values()) {
            for (StructureBoundsRenderer renderer : rendererList) {
               if (renderer.lineBuffer != null) {
                  renderer.lineBuffer.close();
               }
            }
         }

         STRUCTURE_INFOS.clear();
      }

      if (StructureGelMod.proxy.shouldViewBounds()) {
         ResourceLocation dimensionName = mc.level.dimension().location();
         List<StructureBoundsRenderer> renderers = STRUCTURE_INFOS.get(dimensionName);
         if (renderers != null) {
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            poseStack.pushPose();
            poseStack.translate(-camX, -camY, -camZ);
            ShaderInstance shaderInstance = RenderSystem.getShader();
            Vec3i camPos = Positions.vec3i(camX, camY, camZ);

            for (int i = renderers.size() - 1; i > -1; i--) {
               StructureBoundsRenderer rendererx = renderers.get(i);
               if (rendererx.pos.closerThan(camPos, 300.0)) {
                  rendererx.renderBounds(mc, poseStack, projectionMatrix, shaderInstance, camX, camY, camZ);
               }
            }

            poseStack.popPose();
         }
      }
   }

   private void renderBounds(Minecraft mc, PoseStack poseStack, Matrix4f projectionMatrix, ShaderInstance shaderInstance, double camX, double camY, double camZ) {
      if (this.needsUpdated) {
         this.needsUpdated = false;
         if (this.lineBuffer != null) {
            this.lineBuffer.close();
         }

         this.lineBuffer = new VertexBuffer(Usage.STATIC);
         BufferBuilder buffBuilder = Tesselator.getInstance().getBuilder();
         buffBuilder.begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
         PoseStack compileStack = new PoseStack();
         this.compileBounds(mc, compileStack, buffBuilder, camX, camY, camZ);
         this.lineBuffer.bind();
         this.lineBuffer.upload(buffBuilder.end());
         VertexBuffer.unbind();
      }

      if (this.lineBuffer != null) {
         poseStack.pushPose();
         BoundingBox bb = this.structureInfo.fullBounds();
         poseStack.translate(camX, camY, camZ);
         poseStack.translate(bb.minX() - camX, bb.minY() - camY, bb.minZ() - camZ);
         this.lineBuffer.bind();
         this.lineBuffer.drawWithShader(poseStack.last().pose(), projectionMatrix, shaderInstance);
         poseStack.popPose();
         VertexBuffer.unbind();
      }
   }

   private void compileBounds(Minecraft mc, PoseStack poseStack, BufferBuilder buffBuilder, double camX, double camY, double camZ) {
      BoundingBox fullBounds = this.structureInfo.fullBounds();
      int minX = fullBounds.minX();
      int minY = fullBounds.minY();
      int minZ = fullBounds.minZ();
      IRenderBase.makeLineBox(poseStack, buffBuilder, BoundingBox.fromCorners(Vec3i.ZERO, fullBounds.getLength()), 0.12, 1.0F, 1.0F, 1.0F);

      for (int i = this.structureInfo.pieces().size() - 1; i > -1; i--) {
         StructureInfo.PieceInfo pieceInfo = this.structureInfo.pieces().get(i);
         float[] rgb = pieceInfo.getColorFloats();
         BoundingBox pieceBounds = pieceInfo.bounds();
         IRenderBase.makeLineBox(
            poseStack,
            buffBuilder,
            BoundingBox.fromCorners(Vec3i.ZERO, pieceBounds.getLength()).moved(pieceBounds.minX() - minX, pieceBounds.minY() - minY, pieceBounds.minZ() - minZ),
            rgb[0],
            rgb[1],
            rgb[2]
         );
      }
   }

   public static void addInfo(StructureInfo structureInfo) {
      StructureGelMod.LOGGER.log("Recieved structure info at ({}) in {}", structureInfo.fullBounds().getCenter().toShortString(), structureInfo.dimension());
      STRUCTURE_INFOS.computeIfAbsent(structureInfo.dimension(), type -> new ArrayList<>()).add(new StructureBoundsRenderer(structureInfo));
   }

   public static void clear() {
      shouldClear = true;
   }
}
