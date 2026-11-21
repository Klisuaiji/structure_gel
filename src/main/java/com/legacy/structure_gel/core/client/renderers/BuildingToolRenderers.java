package com.legacy.structure_gel.core.client.renderers;

import com.legacy.structure_gel.core.SGConfig;
import com.legacy.structure_gel.core.item.building_tool.BuildingToolItem;
import com.legacy.structure_gel.core.item.building_tool.BuildingToolModes;
import com.legacy.structure_gel.core.item.building_tool.CapturedBlocks;
import com.legacy.structure_gel.core.item.building_tool.ToolModeProperty;
import com.legacy.structure_gel.core.item.building_tool.modes.CloneTool;
import com.legacy.structure_gel.core.item.building_tool.modes.ExtendTool;
import com.legacy.structure_gel.core.item.building_tool.modes.FloodTool;
import com.legacy.structure_gel.core.item.building_tool.modes.LineTool;
import com.legacy.structure_gel.core.item.building_tool.modes.ReplaceTool;
import com.mojang.blaze3d.platform.GlStateManager.DestFactor;
import com.mojang.blaze3d.platform.GlStateManager.SourceFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexBuffer.Usage;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Matrix4f;

public class BuildingToolRenderers {
   public static void init() {
   }

   static {
      BuildingToolRenderer.RENDERERS
         .put(
            BuildingToolModes.REPLACE,
            () -> new BuildingToolRenderer() {
               @Override
               protected void compileInfo(
                  Minecraft mc,
                  ItemStack stack,
                  PoseStack poseStack,
                  BufferBuilder buffBuilder,
                  double camX,
                  double camY,
                  double camZ,
                  BuildingToolRenderer.RenderInfo renderInfo
               ) {
                  renderInfo.renderPos = this.renderPos;
                  if (this.hitPos != null) {
                     renderInfo.renderPos = this.hitPos;
                     int r = BuildingToolItem.getProperty(stack, ToolModeProperty.MEDIUM_RADIUS);
                     boolean fuzzy = BuildingToolItem.getProperty(stack, ToolModeProperty.FUZZY_TRUE).value();
                     Set<BlockPos> poses = ReplaceTool.getReplacePositions(mc.level, this.hitPos, r, fuzzy)
                        .stream()
                        .map(p -> p.subtract(this.hitPos))
                        .collect(Collectors.toSet());
                     IRenderBase.highlightBlocks(poseStack, buffBuilder, renderInfo.renderPos, poses, 0.75F, 0.0F, 0.75F, 0.3F);
                     IRenderBase.makeLineBox(poseStack, buffBuilder, BoundingBox.fromCorners(Vec3i.ZERO, Vec3i.ZERO), 0.75F, 0.0F, 0.75F, 0.3F);
                  }
               }
            }
         );
      BuildingToolRenderer.RENDERERS
         .put(
            BuildingToolModes.FILL,
            () -> new BuildingToolRenderer.ForCorners() {
               @Override
               protected void compileInfo(
                  Minecraft mc,
                  ItemStack stack,
                  PoseStack poseStack,
                  BufferBuilder buffBuilder,
                  double camX,
                  double camY,
                  double camZ,
                  BuildingToolRenderer.RenderInfo renderInfo
               ) {
                  if (this.pos != null && this.secondPos != null) {
                     ToolModeProperty.Shape shape = BuildingToolItem.getProperty(stack, ToolModeProperty.FILL_SHAPE);
                     if (shape != ToolModeProperty.Shape.CUBE) {
                        Set<BlockPos> poses = new HashSet<>();
                        BoundingBox bounds = BoundingBox.fromCorners(this.pos, this.secondPos);
                        double dx = (bounds.getXSpan() - 1) / 2.0;
                        double dy = (bounds.getYSpan() - 1) / 2.0;
                        double dz = (bounds.getZSpan() - 1) / 2.0;
                        int yInvert = bounds.maxY() - bounds.minY();
                        BlockPos centerPos = bounds.getCenter();

                        for (int x = bounds.minX(); x <= bounds.maxX(); x++) {
                           for (int z = bounds.minZ(); z <= bounds.maxZ(); z++) {
                              for (int y = bounds.minY(); y <= bounds.maxY(); y++) {
                                 BlockPos pos = centerPos.offset(-x, -y, -z);
                                 if (shape.isInside(Vec3.atLowerCornerOf(pos), dx, dy, dz)) {
                                    BlockPos rPos = pos.offset((int)dx, (int)dy, (int)dz);
                                    poses.add(new BlockPos(rPos.getX(), yInvert - rPos.getY(), rPos.getZ()));
                                 }
                              }
                           }
                        }

                        Vec3i shapeRenderPos = new Vec3i(bounds.minX(), bounds.minY(), bounds.minZ());
                        IRenderBase.highlightBlocks(poseStack, buffBuilder, shapeRenderPos, poses, 0.75F, 0.0F, 0.75F, 0.3F);
                     }
                  }

                  super.compileInfo(mc, stack, poseStack, buffBuilder, camX, camY, camZ, renderInfo);
               }

               @Override
               protected float[] getOutlineRGB() {
                  return new float[]{0.75F, 0.0F, 0.75F};
               }
            }
         );
      BuildingToolRenderer.RENDERERS
         .put(
            BuildingToolModes.CLONE,
            () -> new BuildingToolRenderer.ForCorners() {
               @Nullable
               private CapturedBlocks capturedBlocksCache = null;
               @Nullable
               private VertexBuffer destBuffer = null;
               private final Tesselator destTesselator = new Tesselator();
               @Nullable
               private CompletableFuture<BuildingToolRenderer.RenderResult> destFutureRender = null;
               private BuildingToolRenderer.RenderInfo destLastRenderInfo = new BuildingToolRenderer.RenderInfo();

               @Override
               protected float[] getOutlineRGB() {
                  return new float[]{0.0F, 0.85F, 0.0F};
               }

               @Override
               protected void updateData(Minecraft mc, ItemStack stack) {
                  super.updateData(mc, stack);
                  CapturedBlocks captured = BuildingToolItem.getCapturedBlocks(stack, mc.level, mc.player);
                  if (!Objects.equals(captured, this.capturedBlocksCache)) {
                     this.needsCompiled = true;
                     this.selectionChanged = true;
                  }

                  if (this.needsCompiled && (this.selectionChanged || this.itemChanged) && BuildingToolItem.getSelectedCorner(stack) == null) {
                     if (this.pos != null && this.secondPos != null && captured != null) {
                        RandomSource rand = mc.level.random;
                        ToolModeProperty.SGMirror mirror = BuildingToolItem.getProperty(stack, ToolModeProperty.MIRROR);
                        ToolModeProperty.SGRotation rotation = BuildingToolItem.getProperty(stack, ToolModeProperty.ROTATION);
                        CapturedBlocks capturedC = captured.clientCopy()
                           .withTransforms(
                              (mirror == ToolModeProperty.SGMirror.RANDOM ? ToolModeProperty.SGMirror.NONE : mirror).toVanilla(rand),
                              (rotation == ToolModeProperty.SGRotation.RANDOM ? ToolModeProperty.SGRotation.R_0 : rotation).toVanilla(rand)
                           );
                        capturedC.compressForRender(mc.level, this.pos, mc.player);
                        this.capturedBlocksCache = capturedC;
                     } else {
                        this.capturedBlocksCache = null;
                     }
                  }
               }

               @Override
               protected void compileInfo(
                  Minecraft mc,
                  ItemStack stack,
                  PoseStack poseStack,
                  BufferBuilder buffBuilder,
                  double camX,
                  double camY,
                  double camZ,
                  BuildingToolRenderer.RenderInfo renderInfo
               ) {
                  super.compileInfo(mc, stack, poseStack, buffBuilder, camX, camY, camZ, renderInfo);
               }

               protected void compileDest(
                  Minecraft mc,
                  ItemStack stack,
                  PoseStack poseStack,
                  BufferBuilder buffBuilder,
                  double camX,
                  double camY,
                  double camZ,
                  BuildingToolRenderer.RenderInfo renderInfo
               ) {
                  if (this.hitPos != null
                     && this.pos != null
                     && this.secondPos != null
                     && this.capturedBlocksCache != null
                     && BuildingToolItem.getSelectedCorner(stack) == null) {
                     try {
                        BoundingBox destBB = CloneTool.getCloneDestBounds(
                           this.capturedBlocksCache, BlockPos.ZERO, Blocks.AIR.defaultBlockState(), Vec3.ZERO, false
                        );
                        BlockPos worldPos = new BlockPos(destBB.minX(), destBB.minY(), destBB.minZ());
                        Map<BlockPos, VoxelShape> shapes = this.capturedBlocksCache.getShapes(mc.level, worldPos);
                        BoundingBox worldDestBB = CloneTool.getCloneDestBounds(
                           this.capturedBlocksCache,
                           this.hitPos,
                           mc.player.level().getBlockState(this.hitPos),
                           mc.player.position(),
                           mc.player.isShiftKeyDown()
                        );
                        Vec3i cloneRenderPos = new Vec3i(worldDestBB.minX() - (int)camX, worldDestBB.minY() - (int)camY, worldDestBB.minZ() - (int)camZ);
                        IRenderBase.highlightVoxelShapes(poseStack, buffBuilder, Vec3.ZERO, cloneRenderPos, shapes, 0.6F, 0.0F, 0.9F, 0.7F);

                        for (AABB bounds : this.capturedBlocksCache.getEntityBoxes(mc.level, worldPos)) {
                           IRenderBase.makeBox(poseStack, buffBuilder, bounds, 0.8F, 0.8F, 0.8F, 0.7F);
                        }

                        IRenderBase.makeLineBox(poseStack, buffBuilder, destBB, -0.01, 0.75F, 0.0F, 0.75F, 0.3F);
                     } catch (NullPointerException e) {
                        if (this.capturedBlocksCache == null) {
                           return;
                        }

                        throw e;
                     }
                  }
               }

               @Override
               protected BuildingToolRenderer.RenderInfo compile(Minecraft mc, LocalPlayer player, ItemStack stack, double camX, double camY, double camZ) {
                  BuildingToolRenderer.RenderInfo ret = super.compile(mc, player, stack, camX, camY, camZ);
                  if (SGConfig.CLIENT.threadBuildingTool()) {
                     if (this.destFutureRender == null) {
                        this.destLastRenderInfo.isFinished = false;
                        this.destFutureRender = CompletableFuture.supplyAsync(() -> {
                           PoseStack poseStackx = new PoseStack();
                           BufferBuilder buffBuilderx = this.destTesselator.getBuilder();
                           buffBuilderx.begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
                           BuildingToolRenderer.RenderInfo renderInfox = new BuildingToolRenderer.RenderInfo();
                           this.compileDest(mc, stack, poseStackx, buffBuilderx, camX, camY, camZ, renderInfox);
                           return new BuildingToolRenderer.RenderResult(renderInfox, buffBuilderx);
                        }, Util.backgroundExecutor());
                     }

                     if (this.destFutureRender != null && this.destFutureRender.isDone()) {
                        try {
                           BuildingToolRenderer.RenderResult result = this.destFutureRender.get();
                           result.renderInfo().isFinished = true;
                           if (this.destBuffer != null) {
                              this.destBuffer.close();
                           }

                           this.destBuffer = new VertexBuffer(Usage.STATIC);
                           this.destBuffer.bind();
                           this.destBuffer.upload(result.buffBuilder().end());
                           this.destFutureRender = null;
                           VertexBuffer.unbind();
                           this.destLastRenderInfo = result.renderInfo();
                        } catch (Throwable t) {
                           t.printStackTrace();
                        }
                     }
                  } else {
                     PoseStack poseStack = new PoseStack();
                     BufferBuilder buffBuilder = Tesselator.getInstance().getBuilder();
                     buffBuilder.begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
                     BuildingToolRenderer.RenderInfo renderInfo = new BuildingToolRenderer.RenderInfo();
                     this.compileDest(mc, stack, poseStack, buffBuilder, camX, camY, camZ, renderInfo);
                     if (this.destBuffer != null) {
                        this.destBuffer.close();
                     }

                     this.destBuffer = new VertexBuffer(Usage.STATIC);
                     this.destBuffer.bind();
                     this.destBuffer.upload(buffBuilder.end());
                     VertexBuffer.unbind();
                     renderInfo.isFinished = true;
                     this.destLastRenderInfo = renderInfo;
                  }

                  ret.isFinished = ret.isFinished && this.destLastRenderInfo.isFinished;
                  return ret;
               }

               @Override
               protected void render(
                  Minecraft mc, LocalPlayer player, ItemStack stack, PoseStack poseStack, Matrix4f projectionMatrix, double camX, double camY, double camZ
               ) {
                  super.render(mc, player, stack, poseStack, -1);
                  if (this.destBuffer != null && this.hitPos != null && this.capturedBlocksCache != null) {
                     poseStack.pushPose();
                     RenderSystem.setShader(GameRenderer::getPositionColorShader);
                     RenderSystem.enableBlend();
                     RenderSystem.blendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ONE_MINUS_SRC_ALPHA);
                     RenderSystem.enableDepthTest();
                     poseStack.translate(camX, camY, camZ);
                     BoundingBox destBB = CloneTool.getCloneDestBounds(
                        this.capturedBlocksCache, this.hitPos, player.level().getBlockState(this.hitPos), player.position(), player.isShiftKeyDown()
                     );
                     poseStack.translate(destBB.minX() - camX, destBB.minY() - camY, destBB.minZ() - camZ);
                     this.destBuffer.bind();
                     this.destBuffer.drawWithShader(poseStack.last().pose(), projectionMatrix, RenderSystem.getShader());
                     VertexBuffer.unbind();
                     RenderSystem.disableBlend();
                     RenderSystem.disableDepthTest();
                     poseStack.popPose();
                  }
               }

               @Override
               protected void close() {
                  super.close();
                  if (this.destBuffer != null) {
                     this.destBuffer.close();
                  }

                  this.destBuffer = null;
               }
            }
         );
      BuildingToolRenderer.RENDERERS
         .put(
            BuildingToolModes.FLOOD,
            () -> new BuildingToolRenderer() {
               @Override
               protected void compileInfo(
                  Minecraft mc,
                  ItemStack stack,
                  PoseStack poseStack,
                  BufferBuilder buffBuilder,
                  double camX,
                  double camY,
                  double camZ,
                  BuildingToolRenderer.RenderInfo renderInfo
               ) {
                  renderInfo.renderPos = this.renderPos;
                  if (this.hitPos != null && this.hitFace != null && this.hitBlock) {
                     renderInfo.renderPos = this.hitPos;
                     IRenderBase.highlightBlockPos(
                        poseStack,
                        buffBuilder,
                        renderInfo.renderPos,
                        mapPosesTo(
                           FloodTool.getFloodPositions(
                              mc.level,
                              this.hitPos,
                              this.hitFace,
                              BuildingToolItem.getProperty(stack, ToolModeProperty.REPLACE_NOT_AIR_CLICKED),
                              BuildingToolItem.getProperty(stack, ToolModeProperty.LARGE_RADIUS)
                           ),
                           renderInfo.renderPos
                        ),
                        0.75F,
                        0.0F,
                        0.75F,
                        0.3F
                     );
                  }
               }
            }
         );
      BuildingToolRenderer.RENDERERS
         .put(
            BuildingToolModes.EXTEND,
            () -> new BuildingToolRenderer() {
               private Set<BlockPos> extendPoses = new HashSet<>();

               @Override
               protected void updateData(Minecraft mc, ItemStack stack) {
                  super.updateData(mc, stack);
                  if (!this.needsCompiled
                     && this.hitPos != null
                     && this.hitFace != null
                     && this.extendPoses.size()
                        != ExtendTool.getExtendPositions(
                              mc.level,
                              this.hitPos,
                              this.hitFace,
                              BuildingToolItem.getProperty(stack, ToolModeProperty.REPLACE_NOT_AIR_CLICKED),
                              BuildingToolItem.getProperty(stack, ToolModeProperty.LARGE_RADIUS),
                              BuildingToolItem.getProperty(stack, ToolModeProperty.FUZZY_TRUE).value()
                           )
                           .size()) {
                     this.needsCompiled = true;
                  }
               }

               @Override
               protected void compileInfo(
                  Minecraft mc,
                  ItemStack stack,
                  PoseStack poseStack,
                  BufferBuilder buffBuilder,
                  double camX,
                  double camY,
                  double camZ,
                  BuildingToolRenderer.RenderInfo renderInfo
               ) {
                  renderInfo.renderPos = this.renderPos;
                  if (this.hitPos != null && this.hitFace != null && this.hitBlock) {
                     renderInfo.renderPos = this.hitPos;
                     this.extendPoses = ExtendTool.getExtendPositions(
                        mc.level,
                        this.hitPos,
                        this.hitFace,
                        BuildingToolItem.getProperty(stack, ToolModeProperty.REPLACE_NOT_AIR_CLICKED),
                        BuildingToolItem.getProperty(stack, ToolModeProperty.LARGE_RADIUS),
                        BuildingToolItem.getProperty(stack, ToolModeProperty.FUZZY_TRUE).value()
                     );
                     IRenderBase.highlightBlockPos(
                        poseStack, buffBuilder, renderInfo.renderPos, mapPosesTo(this.extendPoses, renderInfo.renderPos), 0.75F, 0.0F, 0.75F, 0.3F
                     );
                  }
               }
            }
         );
      BuildingToolRenderer.RENDERERS.put(BuildingToolModes.MOVE, () -> new BuildingToolRenderer.ForCorners() {
         @Override
         protected float[] getOutlineRGB() {
            return new float[]{0.75F, 0.0F, 0.75F};
         }
      });
      BuildingToolRenderer.RENDERERS
         .put(
            BuildingToolModes.LINE,
            () -> new BuildingToolRenderer.ForCorners() {
               @Override
               protected float[] getOutlineRGB() {
                  return new float[]{0.75F, 0.0F, 0.75F};
               }

               @Override
               protected void compileInfo(
                  Minecraft mc,
                  ItemStack stack,
                  PoseStack poseStack,
                  BufferBuilder buffBuilder,
                  double camX,
                  double camY,
                  double camZ,
                  BuildingToolRenderer.RenderInfo renderInfo
               ) {
                  super.compileInfo(mc, stack, poseStack, buffBuilder, camX, camY, camZ, renderInfo);
                  boolean hasPosA = this.pos != null;
                  boolean hasPosB = this.secondPos != null;
                  if (hasPosA && hasPosB) {
                     float[] rgb = this.getOutlineRGB();
                     float a = 0.3F;
                     IRenderBase.highlightBlockPos(
                        poseStack,
                        buffBuilder,
                        renderInfo.renderPos,
                        mapPosesTo(LineTool.getLinePositions(this.pos, this.secondPos), renderInfo.renderPos),
                        rgb[0],
                        rgb[1],
                        rgb[2],
                        a
                     );
                  }
               }
            }
         );
      BuildingToolRenderer.RENDERERS
         .put(
            BuildingToolModes.SHAPE,
            () -> new BuildingToolRenderer() {
               @Override
               protected void compileInfo(
                  Minecraft mc,
                  ItemStack stack,
                  PoseStack poseStack,
                  BufferBuilder buffBuilder,
                  double camX,
                  double camY,
                  double camZ,
                  BuildingToolRenderer.RenderInfo renderInfo
               ) {
                  renderInfo.renderPos = this.renderPos;
                  if (this.hitPos != null) {
                     int r = BuildingToolItem.getProperty(stack, ToolModeProperty.RADIUS);
                     BoundingBox destBB = BoundingBox.fromCorners(Vec3i.ZERO.offset(-r, -r, -r), Vec3i.ZERO.offset(r, r, r));
                     renderInfo.renderPos = this.hitPos;
                     if (r <= 30) {
                        ToolModeProperty.Replace replace = BuildingToolItem.getProperty(stack, ToolModeProperty.REPLACE);
                        ToolModeProperty.Shape shape = BuildingToolItem.getProperty(stack, ToolModeProperty.SHAPE);
                        Set<BlockPos> poses = new HashSet<>();
                        BlockState clickedState = mc.level.getBlockState(this.hitPos);

                        for (int x = -r; x <= r; x++) {
                           for (int z = -r; z <= r; z++) {
                              for (int y = -r; y <= r; y++) {
                                 BlockPos pos = new BlockPos(x, y, z);
                                 if (shape.isInside(pos.multiply(-1), r) && replace.shouldReplace(mc.level, clickedState, this.hitPos.offset(pos))) {
                                    poses.add(pos);
                                 }
                              }
                           }
                        }

                        IRenderBase.highlightBlocks(poseStack, buffBuilder, renderInfo.renderPos, poses, 0.75F, 0.0F, 0.75F, 0.3F);
                     }

                     IRenderBase.makeLineBox(poseStack, buffBuilder, destBB, 0.75F, 0.0F, 0.75F, 0.3F);
                  }
               }
            }
         );
   }
}
