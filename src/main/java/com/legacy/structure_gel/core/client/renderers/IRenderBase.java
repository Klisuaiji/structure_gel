package com.legacy.structure_gel.core.client.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.datafixers.util.Pair;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Matrix4f;

public interface IRenderBase {
   byte DOWN = (byte)(1 << Direction.DOWN.get3DDataValue());
   byte UP = (byte)(1 << Direction.UP.get3DDataValue());
   byte NORTH = (byte)(1 << Direction.NORTH.get3DDataValue());
   byte SOUTH = (byte)(1 << Direction.SOUTH.get3DDataValue());
   byte WEST = (byte)(1 << Direction.WEST.get3DDataValue());
   byte EAST = (byte)(1 << Direction.EAST.get3DDataValue());
   byte ALL = (byte)(DOWN + UP + NORTH + SOUTH + WEST + EAST);
   byte[] DIRECTION_FLAGS = new byte[]{DOWN, UP, NORTH, SOUTH, WEST, EAST};
   Vec3 ONE = new Vec3(1.0, 1.0, 1.0);

   static void makeLineBox(PoseStack poseStack, VertexConsumer vertexCon, BlockPos pos, float r, float g, float b, float a) {
      makeLineBox(poseStack, vertexCon, BoundingBox.fromCorners(pos, pos), 0.0, r, g, b, a);
   }

   static void makeLineBox(PoseStack poseStack, VertexConsumer vertexCon, BoundingBox bounds, float r, float g, float b) {
      makeLineBox(poseStack, vertexCon, bounds, 0.0, r, g, b);
   }

   static void makeLineBox(PoseStack poseStack, VertexConsumer vertexCon, BoundingBox bounds, float r, float g, float b, float a) {
      makeLineBox(poseStack, vertexCon, bounds, 0.0, r, g, b, a);
   }

   static void makeLineBox(PoseStack poseStack, VertexConsumer vertexCon, BoundingBox bounds, double expand, float r, float g, float b) {
      makeLineBox(poseStack, vertexCon, bounds, expand, r, g, b, 1.0F);
   }

   static void makeLineBox(PoseStack poseStack, VertexConsumer vertexCon, BoundingBox bounds, double expand, float r, float g, float b, float a) {
      makeLineBox(
         poseStack,
         vertexCon,
         bounds.minX() - expand,
         bounds.minY() - expand,
         bounds.minZ() - expand,
         bounds.maxX() + 1 + expand,
         bounds.maxY() + 1 + expand,
         bounds.maxZ() + 1 + expand,
         r,
         g,
         b,
         a
      );
   }

   static void makeLineBox(
      PoseStack poseStack, VertexConsumer vertexCon, double nX, double nY, double nZ, double xX, double xY, double xZ, float r, float g, float b, float a
   ) {
      double width = 0.04;
      byte topCulls = (byte)(UP + DOWN);
      byte sideCulls = (byte)(WEST + EAST);
      makeBox(poseStack, vertexCon, nX - width, nY + width, nZ - width, nX + width, xY - width, nZ + width, r, g, b, a, topCulls);
      makeBox(poseStack, vertexCon, nX - width, nY + width, xZ - width, nX + width, xY - width, xZ + width, r, g, b, a, topCulls);
      makeBox(poseStack, vertexCon, xX - width, nY + width, nZ - width, xX + width, xY - width, nZ + width, r, g, b, a, topCulls);
      makeBox(poseStack, vertexCon, xX - width, nY + width, xZ - width, xX + width, xY - width, xZ + width, r, g, b, a, topCulls);
      makeBox(poseStack, vertexCon, nX + width, xY - width, nZ - width, xX - width, xY + width, nZ + width, r, g, b, a, sideCulls);
      makeBox(poseStack, vertexCon, nX + width, xY - width, xZ - width, xX - width, xY + width, xZ + width, r, g, b, a, sideCulls);
      makeBox(poseStack, vertexCon, nX - width, xY - width, nZ - width, nX + width, xY + width, xZ + width, r, g, b, a);
      makeBox(poseStack, vertexCon, xX - width, xY - width, nZ - width, xX + width, xY + width, xZ + width, r, g, b, a);
      makeBox(poseStack, vertexCon, nX + width, nY - width, nZ - width, xX - width, nY + width, nZ + width, r, g, b, a, sideCulls);
      makeBox(poseStack, vertexCon, nX + width, nY - width, xZ - width, xX - width, nY + width, xZ + width, r, g, b, a, sideCulls);
      makeBox(poseStack, vertexCon, nX - width, nY - width, nZ - width, nX + width, nY + width, xZ + width, r, g, b, a);
      makeBox(poseStack, vertexCon, xX - width, nY - width, nZ - width, xX + width, nY + width, xZ + width, r, g, b, a);
   }

   static void makeBox(PoseStack poseStack, VertexConsumer vertexCon, AABB bounds, float r, float g, float b, float a) {
      makeBox(poseStack, vertexCon, bounds.minX, bounds.minY, bounds.minZ, bounds.maxX, bounds.maxY, bounds.maxZ, r, g, b, a);
   }

   static void makeBox(PoseStack poseStack, VertexConsumer vertexCon, Vec3 pos, float r, float g, float b, float a) {
      makeBox(poseStack, vertexCon, pos, pos.add(ONE), r, g, b, a);
   }

   static void makeBox(PoseStack poseStack, VertexConsumer vertexCon, Vec3 from, Vec3 to, float r, float g, float b, float a) {
      makeBox(poseStack, vertexCon, from.x(), from.y(), from.z(), to.x(), to.y(), to.z(), r, g, b, a);
   }

   private static int comparePoses(Vec3 renderPos, Vec3 camPos, BlockPos p1, BlockPos p2) {
      return Double.compare(renderPos.add(Vec3.atCenterOf(p2)).distanceToSqr(camPos), renderPos.add(Vec3.atCenterOf(p1)).distanceToSqr(camPos));
   }

   static void highlightVoxelShapes(
      PoseStack poseStack, VertexConsumer vertexCon, Vec3i renderPos, Map<BlockPos, VoxelShape> shapes, float r, float g, float b, float a
   ) {
      highlightVoxelShapes(poseStack, vertexCon, Minecraft.getInstance().gameRenderer.getMainCamera().getPosition(), renderPos, shapes, r, g, b, a);
   }

   static void highlightVoxelShapes(
      PoseStack poseStack, VertexConsumer vertexCon, Vec3 camPos, Vec3i renderPos, Map<BlockPos, VoxelShape> shapes, float r, float g, float b, float a
   ) {
      Vec3 renderVec = Vec3.atCenterOf(renderPos);
      shapes.entrySet()
         .stream()
         .sorted((e1, e2) -> comparePoses(renderVec, camPos, e1.getKey(), e2.getKey()))
         .forEach(
            entry -> {
               BlockPos pos = entry.getKey();
               VoxelShape shape = entry.getValue();
               Direction[] dirs = Direction.values();
               boolean isBlockShape = shape == Shapes.block();
               shape.forAllBoxes(
                  (sx, sy, sz, smx, smy, smz) -> {
                     byte culled = 0;
                     if (isBlockShape) {
                        for (Direction d : dirs) {
                           BlockPos relative = pos.relative(d);
                           if (shapes.get(relative) == Shapes.block()) {
                              culled += DIRECTION_FLAGS[d.get3DDataValue()];
                           }
                        }
                     }

                     if (culled < ALL) {
                        double extend = 0.005;
                        int x = pos.getX();
                        int y = pos.getY();
                        int z = pos.getZ();
                        makeBox(
                           poseStack,
                           vertexCon,
                           x + sx - ((culled & WEST) != 0 ? 0.0 : extend),
                           y + sy - ((culled & DOWN) != 0 ? 0.0 : extend),
                           z + sz - ((culled & NORTH) != 0 ? 0.0 : extend),
                           x + smx + ((culled & EAST) != 0 ? 0.0 : extend),
                           y + smy + ((culled & UP) != 0 ? 0.0 : extend),
                           z + smz + ((culled & SOUTH) != 0 ? 0.0 : extend),
                           r,
                           g,
                           b,
                           a,
                           culled
                        );
                     }
                  }
               );
            }
         );
   }

   static void highlightBlocks(PoseStack poseStack, VertexConsumer vertexCon, Vec3i renderPos, Collection<BlockPos> poses, float r, float g, float b, float a) {
      List<Pair<BlockPos, Byte>> toRender = new ArrayList<>();
      Direction[] dirs = Direction.values();

      for (BlockPos pos : poses) {
         byte culled = 0;

         for (Direction d : dirs) {
            if (poses.contains(pos.relative(d))) {
               culled += DIRECTION_FLAGS[d.get3DDataValue()];
            }
         }

         if (culled < ALL) {
            toRender.add(Pair.of(pos, culled));
         }
      }

      Vec3 renderVec = Vec3.atCenterOf(renderPos);
      Vec3 camPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
      toRender.sort((p1, p2) -> comparePoses(renderVec, camPos, (BlockPos)p1.getFirst(), (BlockPos)p2.getFirst()));
      int size = toRender.size();

      for (int i = 0; i < size; i++) {
         Pair<BlockPos, Byte> pair = toRender.get(i);
         BlockPos pos = (BlockPos)pair.getFirst();
         byte culled = (Byte)pair.getSecond();
         double extend = 0.005;
         int x = pos.getX();
         int y = pos.getY();
         int z = pos.getZ();
         makeBox(
            poseStack,
            vertexCon,
            x - ((culled & WEST) != 0 ? 0.0 : extend),
            y - ((culled & DOWN) != 0 ? 0.0 : extend),
            z - ((culled & NORTH) != 0 ? 0.0 : extend),
            x + 1 + ((culled & EAST) != 0 ? 0.0 : extend),
            y + 1 + ((culled & UP) != 0 ? 0.0 : extend),
            z + 1 + ((culled & SOUTH) != 0 ? 0.0 : extend),
            r,
            g,
            b,
            a,
            culled
         );
      }
   }

   static void highlightBlockPos(PoseStack poseStack, VertexConsumer vertexCon, Vec3i renderPos, Collection<BlockPos> poses, float r, float g, float b, float a) {
      double inner = 0.25;
      double outer = 1.0 - inner;
      Vec3 renderVec = Vec3.atCenterOf(renderPos);
      Vec3 camPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
      List<BlockPos> sorted = new ArrayList<>(poses);
      sorted.sort((p1, p2) -> comparePoses(renderVec, camPos, p1, p2));
      int size = sorted.size();

      for (int i = 0; i < size; i++) {
         BlockPos pos = sorted.get(i);
         int x = pos.getX();
         int y = pos.getY();
         int z = pos.getZ();
         makeBox(poseStack, vertexCon, x + inner, y + inner, z + inner, x + outer, y + outer, z + outer, r, g, b, a);
      }
   }

   static void makeBox(
      PoseStack poseStack,
      VertexConsumer vertexCon,
      double minX,
      double minY,
      double minZ,
      double maxX,
      double maxY,
      double maxZ,
      float r,
      float g,
      float b,
      float a,
      byte culled
   ) {
      makeBox(poseStack, vertexCon, (float)minX, (float)minY, (float)minZ, (float)maxX, (float)maxY, (float)maxZ, r, g, b, a, culled);
   }

   static void makeBox(
      PoseStack poseStack,
      VertexConsumer vertexCon,
      double minX,
      double minY,
      double minZ,
      double maxX,
      double maxY,
      double maxZ,
      float r,
      float g,
      float b,
      float a
   ) {
      makeBox(poseStack, vertexCon, (float)minX, (float)minY, (float)minZ, (float)maxX, (float)maxY, (float)maxZ, r, g, b, a, (byte)0);
   }

   static void makeBox(
      PoseStack poseStack,
      VertexConsumer vertexCon,
      float mnX,
      float mnY,
      float mnZ,
      float mxX,
      float mxY,
      float mxZ,
      float r,
      float g,
      float b,
      float a,
      byte culled
   ) {
      Matrix4f matrix4f = poseStack.last().pose();
      float ew = 0.7F;
      float ns = 0.9F;
      float down = 0.5F;
      float dR = r * down;
      float dG = g * down;
      float dB = b * down;
      float ewR = r * ew;
      float ewG = g * ew;
      float ewB = b * ew;
      float nsR = r * ns;
      float nsG = g * ns;
      float nsB = b * ns;
      if ((culled & WEST) == 0) {
         vertexCon.vertex(matrix4f, mnX, mnY, mnZ).color(ewR, ewG, ewB, a).endVertex();
         vertexCon.vertex(matrix4f, mnX, mnY, mxZ).color(ewR, ewG, ewB, a).endVertex();
         vertexCon.vertex(matrix4f, mnX, mxY, mxZ).color(ewR, ewG, ewB, a).endVertex();
         vertexCon.vertex(matrix4f, mnX, mxY, mnZ).color(ewR, ewG, ewB, a).endVertex();
      }

      if ((culled & SOUTH) == 0) {
         vertexCon.vertex(matrix4f, mnX, mnY, mxZ).color(nsR, nsG, nsB, a).endVertex();
         vertexCon.vertex(matrix4f, mxX, mnY, mxZ).color(nsR, nsG, nsB, a).endVertex();
         vertexCon.vertex(matrix4f, mxX, mxY, mxZ).color(nsR, nsG, nsB, a).endVertex();
         vertexCon.vertex(matrix4f, mnX, mxY, mxZ).color(nsR, nsG, nsB, a).endVertex();
      }

      if ((culled & EAST) == 0) {
         vertexCon.vertex(matrix4f, mxX, mnY, mxZ).color(ewR, ewG, ewB, a).endVertex();
         vertexCon.vertex(matrix4f, mxX, mnY, mnZ).color(ewR, ewG, ewB, a).endVertex();
         vertexCon.vertex(matrix4f, mxX, mxY, mnZ).color(ewR, ewG, ewB, a).endVertex();
         vertexCon.vertex(matrix4f, mxX, mxY, mxZ).color(ewR, ewG, ewB, a).endVertex();
      }

      if ((culled & NORTH) == 0) {
         vertexCon.vertex(matrix4f, mxX, mnY, mnZ).color(nsR, nsG, nsB, a).endVertex();
         vertexCon.vertex(matrix4f, mnX, mnY, mnZ).color(nsR, nsG, nsB, a).endVertex();
         vertexCon.vertex(matrix4f, mnX, mxY, mnZ).color(nsR, nsG, nsB, a).endVertex();
         vertexCon.vertex(matrix4f, mxX, mxY, mnZ).color(nsR, nsG, nsB, a).endVertex();
      }

      if ((culled & DOWN) == 0) {
         vertexCon.vertex(matrix4f, mnX, mnY, mnZ).color(dR, dG, dB, a).endVertex();
         vertexCon.vertex(matrix4f, mxX, mnY, mnZ).color(dR, dG, dB, a).endVertex();
         vertexCon.vertex(matrix4f, mxX, mnY, mxZ).color(dR, dG, dB, a).endVertex();
         vertexCon.vertex(matrix4f, mnX, mnY, mxZ).color(dR, dG, dB, a).endVertex();
      }

      if ((culled & UP) == 0) {
         vertexCon.vertex(matrix4f, mnX, mxY, mnZ).color(r, g, b, a).endVertex();
         vertexCon.vertex(matrix4f, mnX, mxY, mxZ).color(r, g, b, a).endVertex();
         vertexCon.vertex(matrix4f, mxX, mxY, mxZ).color(r, g, b, a).endVertex();
         vertexCon.vertex(matrix4f, mxX, mxY, mnZ).color(r, g, b, a).endVertex();
      }
   }
}
