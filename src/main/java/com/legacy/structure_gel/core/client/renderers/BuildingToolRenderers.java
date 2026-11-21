package com.legacy.structure_gel.core.client.renderers;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.joml.Matrix4f;

import com.legacy.structure_gel.core.SGConfig;
import com.legacy.structure_gel.core.client.SGRenderType;
import com.legacy.structure_gel.core.item.building_tool.BuildingToolItem;
import com.legacy.structure_gel.core.item.building_tool.BuildingToolModes;
import com.legacy.structure_gel.core.item.building_tool.CapturedBlocks;
import com.legacy.structure_gel.core.item.building_tool.ToolModeProperty;
import com.legacy.structure_gel.core.item.building_tool.ToolModeProperty.Replace;
import com.legacy.structure_gel.core.item.building_tool.ToolModeProperty.SGMirror;
import com.legacy.structure_gel.core.item.building_tool.ToolModeProperty.SGRotation;
import com.legacy.structure_gel.core.item.building_tool.ToolModeProperty.Shape;
import com.legacy.structure_gel.core.item.building_tool.modes.CloneTool;
import com.legacy.structure_gel.core.item.building_tool.modes.ExtendTool;
import com.legacy.structure_gel.core.item.building_tool.modes.FloodTool;
import com.legacy.structure_gel.core.item.building_tool.modes.LineTool;
import com.legacy.structure_gel.core.item.building_tool.modes.ReplaceTool;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
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

public class BuildingToolRenderers
{
	public static void init()
	{
	}

	static
	{
		BuildingToolRenderer.RENDERERS.put(BuildingToolModes.REPLACE, () -> new BuildingToolRenderer()
		{
			@Override
			protected void compileInfo(Minecraft mc, ItemStack stack, PoseStack poseStack, BufferBuilder buffBuilder, double camX, double camY, double camZ, RenderInfo renderInfo)
			{
				renderInfo.renderPos = this.renderPos;
				if (this.hitPos != null)
				{
					renderInfo.renderPos = this.hitPos;
					int r = BuildingToolItem.getProperty(stack, ToolModeProperty.MEDIUM_RADIUS);
					boolean fuzzy = BuildingToolItem.getProperty(stack, ToolModeProperty.FUZZY_TRUE).value();
					Set<BlockPos> poses = ReplaceTool.getReplacePositions(mc.level, this.hitPos, r, fuzzy).stream().map(p -> p.subtract(this.hitPos)).collect(Collectors.toSet());

					IRenderBase.highlightBlocks(poseStack, buffBuilder, renderInfo.renderPos, poses, 0.75F, 0.0F, 0.75F, 0.3F);

					IRenderBase.makeLineBox(poseStack, buffBuilder, BoundingBox.fromCorners(Vec3i.ZERO, Vec3i.ZERO), 0.75F, 0.0F, 0.75F, 0.3F);
				}
			}
		});

		BuildingToolRenderer.RENDERERS.put(BuildingToolModes.FILL, () -> new BuildingToolRenderer.ForCorners()
		{
			@Override
			protected void compileInfo(Minecraft mc, ItemStack stack, PoseStack poseStack, BufferBuilder buffBuilder, double camX, double camY, double camZ, RenderInfo renderInfo)
			{
				if (this.pos != null && this.secondPos != null)
				{
					Shape shape = BuildingToolItem.getProperty(stack, ToolModeProperty.FILL_SHAPE);
					if (shape != Shape.CUBE)
					{
						Set<BlockPos> poses = new HashSet<>();
						BoundingBox bounds = BoundingBox.fromCorners(this.pos, this.secondPos);
						double dx = (bounds.getXSpan() - 1) / 2.0;
						double dy = (bounds.getYSpan() - 1) / 2.0;
						double dz = (bounds.getZSpan() - 1) / 2.0;
						int yInvert = bounds.maxY() - bounds.minY();
						BlockPos centerPos = bounds.getCenter();
						for (int x = bounds.minX(); x <= bounds.maxX(); x++)
						{
							for (int z = bounds.minZ(); z <= bounds.maxZ(); z++)
							{
								for (int y = bounds.minY(); y <= bounds.maxY(); y++)
								{
									BlockPos pos = centerPos.offset(-x, -y, -z);
									if (shape.isInside(Vec3.atLowerCornerOf(pos), dx, dy, dz))
									{
										BlockPos rPos = pos.offset((int) dx, (int) dy, (int) dz);
										poses.add(new BlockPos(rPos.getX(), yInvert - rPos.getY(), rPos.getZ()));
									}
								}
							}
						}
						// This is what the render pos should be for a full cube render. I need to do it here because of the rendering order.
						Vec3i shapeRenderPos = new Vec3i(bounds.minX(), bounds.minY(), bounds.minZ());
						IRenderBase.highlightBlocks(poseStack, buffBuilder, shapeRenderPos, poses, 0.75F, 0.0F, 0.75F, 0.3F);
					}
				}

				super.compileInfo(mc, stack, poseStack, buffBuilder, camX, camY, camZ, renderInfo);
			}

			@Override
			protected float[] getOutlineRGB()
			{
				return new float[] { 0.75F, 0.0F, 0.75F };
			}
		});

		BuildingToolRenderer.RENDERERS.put(BuildingToolModes.CLONE, () -> new BuildingToolRenderer.ForCorners()
		{
			@Nullable
			private CapturedBlocks capturedBlocksCache = null;

			@Override
			protected float[] getOutlineRGB()
			{
				return new float[] { 0.0F, 0.85F, 0.0F };
			}

			@Override
			protected void updateData(Minecraft mc, ItemStack stack)
			{
				super.updateData(mc, stack);

				CapturedBlocks captured = BuildingToolItem.getCapturedBlocks(stack, mc.level, mc.player);
				// This is to make sure it updates
				if (!Objects.equals(captured, this.capturedBlocksCache))
				{
					this.needsCompiled = true;
					this.selectionChanged = true;
				}

				if (this.needsCompiled && (this.selectionChanged || this.itemChanged) && BuildingToolItem.getSelectedCorner(stack) == null)
				{
					if (this.pos != null && this.secondPos != null && captured != null)
					{
						RandomSource rand = mc.level.random;
						var mirror = BuildingToolItem.getProperty(stack, ToolModeProperty.MIRROR);
						var rotation = BuildingToolItem.getProperty(stack, ToolModeProperty.ROTATION);
						CapturedBlocks capturedC = captured.clientCopy().withTransforms((mirror == SGMirror.RANDOM ? SGMirror.NONE : mirror).toVanilla(rand), (rotation == SGRotation.RANDOM ? SGRotation.R_0 : rotation).toVanilla(rand));
						capturedC.compressForRender(mc.level, this.pos, mc.player);
						this.capturedBlocksCache = capturedC;
					}
					else
					{
						this.capturedBlocksCache = null;
					}
				}
			}

			@Override
			protected void compileInfo(Minecraft mc, ItemStack stack, PoseStack poseStack, BufferBuilder buffBuilder, double camX, double camY, double camZ, RenderInfo renderInfo)
			{
				super.compileInfo(mc, stack, poseStack, buffBuilder, camX, camY, camZ, renderInfo);
			}

			protected void compileDest(Minecraft mc, ItemStack stack, PoseStack poseStack, BufferBuilder buffBuilder, double camX, double camY, double camZ, RenderInfo renderInfo)
			{
				if (this.hitPos != null && this.pos != null && this.secondPos != null)
				{
					if (this.capturedBlocksCache != null && BuildingToolItem.getSelectedCorner(stack) == null)
					{
						try
						{
							BoundingBox destBB = CloneTool.getCloneDestBounds(this.capturedBlocksCache, BlockPos.ZERO, Blocks.AIR.defaultBlockState(), Vec3.ZERO, false);
							BlockPos worldPos = new BlockPos(destBB.minX(), destBB.minY(), destBB.minZ());
							Map<BlockPos, VoxelShape> shapes = this.capturedBlocksCache.getShapes(mc.level, worldPos);
							BoundingBox worldDestBB = CloneTool.getCloneDestBounds(this.capturedBlocksCache, this.hitPos, mc.player.level().getBlockState(this.hitPos), mc.player.position(), mc.player.isShiftKeyDown());
							Vec3i cloneRenderPos = new Vec3i(worldDestBB.minX() - (int) camX, worldDestBB.minY() - (int) camY, worldDestBB.minZ() - (int) camZ);
							IRenderBase.highlightVoxelShapes(poseStack, buffBuilder, Vec3.ZERO, cloneRenderPos, shapes, 0.60F, 0.0F, 0.90F, 0.7F);

							for (AABB bounds : this.capturedBlocksCache.getEntityBoxes(mc.level, worldPos))
							{
								IRenderBase.makeBox(poseStack, buffBuilder, bounds, 0.8F, 0.8F, 0.8F, 0.7F);
							}

							IRenderBase.makeLineBox(poseStack, buffBuilder, destBB, -0.01D, 0.75F, 0.0F, 0.75F, 0.3F);
						}
						catch (NullPointerException e)
						{
							if (this.capturedBlocksCache == null)
							{
								// Expected behavior. If the selection is cleared before the compile finishes, the thread handling the compile will keep running with a null capturedBlocksCache. This stops it.
							}
							else
							{
								throw e;
							}
						}
					}
				}
			}

			private final VertexBuffer destBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
			private boolean destBufferEmpty = true;
			private final Tesselator destTesselator = new Tesselator();
			@Nullable
			private CompletableFuture<RenderResult> destFutureRender = null;
			private RenderInfo destLastRenderInfo = new RenderInfo();

			@Override
			protected RenderInfo compile(Minecraft mc, LocalPlayer player, ItemStack stack, double camX, double camY, double camZ)
			{
				RenderInfo ret = super.compile(mc, player, stack, camX, camY, camZ);

				if (SGConfig.CLIENT.threadBuildingTool())
				{
					if (destFutureRender == null)
					{
						destLastRenderInfo.isFinished = false;
						destFutureRender = CompletableFuture.supplyAsync(() ->
						{
							PoseStack poseStack = new PoseStack();
							BufferBuilder buffBuilder = destTesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
							RenderInfo renderInfo = new RenderInfo();
							this.compileDest(mc, stack, poseStack, buffBuilder, camX, camY, camZ, renderInfo);
							MeshData mesh = buffBuilder.build();
							return new RenderResult(renderInfo, mesh);
						}, Util.backgroundExecutor());
					}
					if (destFutureRender != null && destFutureRender.isDone())
					{
						try
						{
							this.destBuffer.bind();
							RenderResult result = destFutureRender.get();
							result.renderInfo().isFinished = true;
							MeshData mesh = result.mesh();
							if (mesh != null)
							{
								this.destBuffer.upload(mesh);
								this.destBufferEmpty = false;
							}
							else
							{
								this.destBufferEmpty = true;
							}
							destFutureRender = null;
							VertexBuffer.unbind();
							destLastRenderInfo = result.renderInfo();
						}
						catch (Throwable t)
						{
							t.printStackTrace();
						}
					}
				}
				else
				{
					this.destBuffer.bind();
					PoseStack poseStack = new PoseStack();
					BufferBuilder buffBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
					RenderInfo renderInfo = new RenderInfo();
					this.compileDest(mc, stack, poseStack, buffBuilder, camX, camY, camZ, renderInfo);
					MeshData mesh = buffBuilder.build();
					if (mesh != null)
					{
						this.destBuffer.upload(mesh);
						this.destBufferEmpty = false;
					}
					else
					{
						this.destBufferEmpty = true;
					}
					VertexBuffer.unbind();
					renderInfo.isFinished = true;
					destLastRenderInfo = renderInfo;
				}

				ret.isFinished = ret.isFinished && destLastRenderInfo.isFinished;
				return ret;
			}

			@Override
			protected void render(Minecraft mc, LocalPlayer player, ItemStack stack, Matrix4f modelViewMatrix, Matrix4f projectionMatrix, double camX, double camY, double camZ)
			{
				super.render(mc, player, stack, modelViewMatrix, projectionMatrix, camX, camY, camZ);

				if (!this.destBufferEmpty && this.hitPos != null && this.capturedBlocksCache != null)
				{
					BoundingBox destBB = CloneTool.getCloneDestBounds(this.capturedBlocksCache, this.hitPos, player.level().getBlockState(this.hitPos), player.position(), player.isShiftKeyDown());					
					this.drawWithRenderType(SGRenderType.worldWireframe(true), this.destBuffer, modelViewMatrix, projectionMatrix, destBB.minX() - (float) camX, destBB.minY() - (float) camY, destBB.minZ() - (float) camZ);
				}
			}

			@Override
			protected void close()
			{
				super.close();
				this.destBuffer.close();
			}
		});

		BuildingToolRenderer.RENDERERS.put(BuildingToolModes.FLOOD, () -> new BuildingToolRenderer()
		{

			@Override
			protected void compileInfo(Minecraft mc, ItemStack stack, PoseStack poseStack, BufferBuilder buffBuilder, double camX, double camY, double camZ, RenderInfo renderInfo)
			{
				renderInfo.renderPos = this.renderPos;
				if (this.hitPos != null && this.hitFace != null && this.hitBlock)
				{
					renderInfo.renderPos = this.hitPos;

					IRenderBase.highlightBlockPos(poseStack, buffBuilder, renderInfo.renderPos, mapPosesTo(FloodTool.getFloodPositions(mc.level, this.hitPos, this.hitFace, BuildingToolItem.getProperty(stack, ToolModeProperty.REPLACE_NOT_AIR_CLICKED), BuildingToolItem.getProperty(stack, ToolModeProperty.LARGE_RADIUS)), renderInfo.renderPos), 0.75F, 0.0F, 0.75F, 0.3F);
				}
			}
		});

		BuildingToolRenderer.RENDERERS.put(BuildingToolModes.EXTEND, () -> new BuildingToolRenderer()
		{
			private Set<BlockPos> extendPoses = new HashSet<>();

			@Override
			protected void updateData(Minecraft mc, ItemStack stack)
			{
				super.updateData(mc, stack);
				if (!this.needsCompiled && this.hitPos != null && this.hitFace != null && this.extendPoses.size() != ExtendTool.getExtendPositions(mc.level, this.hitPos, this.hitFace, BuildingToolItem.getProperty(stack, ToolModeProperty.REPLACE_NOT_AIR_CLICKED), BuildingToolItem.getProperty(stack, ToolModeProperty.LARGE_RADIUS), BuildingToolItem.getProperty(stack, ToolModeProperty.FUZZY_TRUE).value()).size())
					this.needsCompiled = true;
			}

			@Override
			protected void compileInfo(Minecraft mc, ItemStack stack, PoseStack poseStack, BufferBuilder buffBuilder, double camX, double camY, double camZ, RenderInfo renderInfo)
			{
				renderInfo.renderPos = this.renderPos;
				if (this.hitPos != null && this.hitFace != null && this.hitBlock)
				{
					renderInfo.renderPos = this.hitPos;
					this.extendPoses = ExtendTool.getExtendPositions(mc.level, this.hitPos, this.hitFace, BuildingToolItem.getProperty(stack, ToolModeProperty.REPLACE_NOT_AIR_CLICKED), BuildingToolItem.getProperty(stack, ToolModeProperty.LARGE_RADIUS), BuildingToolItem.getProperty(stack, ToolModeProperty.FUZZY_TRUE).value());
					IRenderBase.highlightBlockPos(poseStack, buffBuilder, renderInfo.renderPos, mapPosesTo(this.extendPoses, renderInfo.renderPos), 0.75F, 0.0F, 0.75F, 0.3F);
				}
			}
		});

		BuildingToolRenderer.RENDERERS.put(BuildingToolModes.MOVE, () -> new BuildingToolRenderer.ForCorners()
		{
			@Override
			protected float[] getOutlineRGB()
			{
				return new float[] { 0.75F, 0.0F, 0.75F };
			}
		});

		BuildingToolRenderer.RENDERERS.put(BuildingToolModes.LINE, () -> new BuildingToolRenderer.ForCorners()
		{
			@Override
			protected float[] getOutlineRGB()
			{
				return new float[] { 0.75F, 0.0F, 0.75F };
			}

			@Override
			protected void compileInfo(Minecraft mc, ItemStack stack, PoseStack poseStack, BufferBuilder buffBuilder, double camX, double camY, double camZ, RenderInfo renderInfo)
			{
				super.compileInfo(mc, stack, poseStack, buffBuilder, camX, camY, camZ, renderInfo);
				boolean hasPosA = this.pos != null;
				boolean hasPosB = this.secondPos != null;

				if (hasPosA && hasPosB)
				{
					float[] rgb = getOutlineRGB();
					float a = 0.3F;
					IRenderBase.highlightBlockPos(poseStack, buffBuilder, renderInfo.renderPos, mapPosesTo(LineTool.getLinePositions(this.pos, this.secondPos), renderInfo.renderPos), rgb[0], rgb[1], rgb[2], a);
				}
			}
		});

		BuildingToolRenderer.RENDERERS.put(BuildingToolModes.SHAPE, () -> new BuildingToolRenderer()
		{

			@Override
			protected void compileInfo(Minecraft mc, ItemStack stack, PoseStack poseStack, BufferBuilder buffBuilder, double camX, double camY, double camZ, RenderInfo renderInfo)
			{
				renderInfo.renderPos = this.renderPos;
				if (this.hitPos != null)
				{
					int r = BuildingToolItem.getProperty(stack, ToolModeProperty.RADIUS);
					BoundingBox destBB = BoundingBox.fromCorners(Vec3i.ZERO.offset(-r, -r, -r), Vec3i.ZERO.offset(r, r, r));
					renderInfo.renderPos = this.hitPos;

					if (r <= 30)
					{
						Replace replace = BuildingToolItem.getProperty(stack, ToolModeProperty.REPLACE);
						Shape shape = BuildingToolItem.getProperty(stack, ToolModeProperty.SHAPE);

						Set<BlockPos> poses = new HashSet<>();
						BlockState clickedState = mc.level.getBlockState(this.hitPos);
						for (int x = -r; x <= r; x++)
						{
							for (int z = -r; z <= r; z++)
							{
								for (int y = -r; y <= r; y++)
								{
									BlockPos pos = new BlockPos(x, y, z);
									if (shape.isInside(pos.multiply(-1), r) && replace.shouldReplace(mc.level, clickedState, this.hitPos.offset(pos)))
										poses.add(pos);
								}
							}
						}

						IRenderBase.highlightBlocks(poseStack, buffBuilder, renderInfo.renderPos, poses, 0.75F, 0.0F, 0.75F, 0.3F);
					}

					IRenderBase.makeLineBox(poseStack, buffBuilder, destBB, 0.75F, 0.0F, 0.75F, 0.3F);

				}
			}
		});
	}
}
