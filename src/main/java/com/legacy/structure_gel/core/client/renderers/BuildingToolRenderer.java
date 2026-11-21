package com.legacy.structure_gel.core.client.renderers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import org.joml.Matrix4f;

import com.legacy.structure_gel.api.util.Positions;
import com.legacy.structure_gel.core.SGConfig;
import com.legacy.structure_gel.core.client.SGRenderType;
import com.legacy.structure_gel.core.item.building_tool.BuildingToolBounds;
import com.legacy.structure_gel.core.item.building_tool.BuildingToolItem;
import com.legacy.structure_gel.core.item.building_tool.BuildingToolMode;
import com.legacy.structure_gel.core.item.building_tool.BuildingToolModes;
import com.legacy.structure_gel.core.item.building_tool.SmartBoundingBox;
import com.legacy.structure_gel.core.item.building_tool.SmartBoundingBox.CornerType;
import com.legacy.structure_gel.core.registry.SGRegistry;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.datafixers.util.Pair;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public abstract class BuildingToolRenderer implements IRenderBase
{
	protected static final Map<BuildingToolMode, Supplier<BuildingToolRenderer>> RENDERERS = new HashMap<>();
	@Nullable
	protected static BuildingToolMode currentMode = null;
	@Nullable
	private static BuildingToolRenderer instance = null;
	private static boolean shouldClear = false;
	@Nullable
	protected BlockPos pos;
	@Nullable
	protected BlockPos hitPos;
	@Nullable
	protected Direction hitFace;
	@Nullable
	protected Vec3 hitVec;
	/**
	 * If a block is being targeted
	 */
	protected boolean hitBlock = false;
	private final VertexBuffer lineBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
	protected boolean needsCompiled = true;
	private boolean lineBufferEmpty = true;
	/**
	 * The position the render is translated to
	 */
	protected Vec3i renderPos = Vec3i.ZERO;

	static
	{
		BuildingToolRenderers.init();
	}

	protected BuildingToolRenderer()
	{
	}

	private static void updateRenderedMode(@Nullable ItemStack stack)
	{
		BuildingToolMode newMode = stack != null && stack.is(SGRegistry.Items.BUILDING_TOOL.get()) ? BuildingToolItem.getMode(stack) : null;
		if (currentMode != newMode || instance == null)
		{
			currentMode = newMode;
			if (instance != null)
				instance.close();
			instance = currentMode == null ? null : RENDERERS.getOrDefault(currentMode, () -> null).get();
		}
	}

	public static void render(Minecraft mc, Matrix4f modelViewMatrix, Matrix4f projectionMatrix, double camX, double camY, double camZ)
	{
		if (shouldClear)
		{
			shouldClear = false;
			if (instance != null)
				instance.close();
			instance = null;
		}

		LocalPlayer player = mc.player;
		if (player != null && player.isCreative())
		{
			Pair<InteractionHand, ItemStack> tool = BuildingToolItem.getBuildingTool(player);
			if (tool == null)
			{
				updateRenderedMode(null);
				return;
			}

			ItemStack toolStack = tool.getSecond();
			updateRenderedMode(toolStack);

			if (instance != null && !toolStack.isEmpty())
			{
				instance.updateData(mc, toolStack);
				Vec3i camPos = Positions.vec3i(camX, camY, camZ);

				if (instance.shouldRender(camPos))
				{
					if (instance.needsCompiled)
					{
						if (instance.compile(mc, player, toolStack, camX, camY, camZ).isFinished)
						{
							instance.needsCompiled = false;
						}
					}
					instance.render(mc, player, toolStack, modelViewMatrix, projectionMatrix, camX, camY, camZ);
				}
			}
		}
	}

	// Each renderer has a tesselator to prevent threading issues
	private final Tesselator tesselator = new Tesselator();
	@Nullable
	private CompletableFuture<RenderResult> futureRender = null;
	// Exists to prevent threading errors. This gets passed from the threaded render to a non-threaded render
	private RenderInfo lastRenderInfo = new RenderInfo();

	protected RenderInfo compile(Minecraft mc, LocalPlayer player, ItemStack stack, double camX, double camY, double camZ)
	{
		if (SGConfig.CLIENT.threadBuildingTool())
		{
			if (futureRender == null)
			{
				lastRenderInfo.isFinished = false;
				futureRender = CompletableFuture.supplyAsync(() ->
				{
					PoseStack poseStack = new PoseStack();
					BufferBuilder buffBuilder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
					RenderInfo renderInfo = new RenderInfo();
					this.compileInfo(mc, stack, poseStack, buffBuilder, camX, camY, camZ, renderInfo);
					MeshData mesh = buffBuilder.build();
					return new RenderResult(renderInfo, mesh);
				}, Util.backgroundExecutor());
			}
			if (futureRender != null && futureRender.isDone())
			{
				try
				{
					this.lineBuffer.bind();
					RenderResult result = futureRender.get();
					result.renderInfo().isFinished = true;
					MeshData mesh = result.mesh;
					if (mesh != null)
					{
						this.lineBuffer.upload(mesh);
						this.lineBufferEmpty = false;
					}
					else
					{
						this.lineBufferEmpty = true;
					}
					futureRender = null;
					VertexBuffer.unbind();
					this.renderPos = result.renderInfo().renderPos;
					lastRenderInfo = result.renderInfo();
				}
				catch (Throwable t)
				{
					t.printStackTrace();
				}
			}
		}
		else
		{
			this.lineBuffer.bind();
			PoseStack poseStack = new PoseStack();
			BufferBuilder buffBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
			RenderInfo renderInfo = new RenderInfo();
			this.compileInfo(mc, stack, poseStack, buffBuilder, camX, camY, camZ, renderInfo);
			MeshData mesh = buffBuilder.build();
			if (mesh != null)
			{
				this.lineBuffer.upload(mesh);
				this.lineBufferEmpty = false;
			}
			else
			{
				this.lineBufferEmpty = true;
			}
			VertexBuffer.unbind();
			this.renderPos = renderInfo.renderPos;
			renderInfo.isFinished = true;
			lastRenderInfo = renderInfo;
		}
		return lastRenderInfo;
	}

	protected static record RenderResult(RenderInfo renderInfo, MeshData mesh)
	{
	}

	protected static class RenderInfo
	{
		boolean isFinished = false;

		Vec3i renderPos = Vec3i.ZERO;
		boolean renderFlagA = true;
	}

	protected void render(Minecraft mc, LocalPlayer player, ItemStack stack, Matrix4f modelViewMatrix, Matrix4f projectionMatrix, double camX, double camY, double camZ)
	{
		if (!this.lineBufferEmpty)
		{
			this.drawWithRenderType(SGRenderType.worldWireframe(true), this.lineBuffer, modelViewMatrix, projectionMatrix, this.renderPos.getX() - (float) camX, this.renderPos.getY() - (float) camY, this.renderPos.getZ() - (float) camZ);
		}
	}

	protected void drawWithRenderType(RenderType renderType, VertexBuffer buffer, Matrix4f modelViewMatrix, Matrix4f projectionMatrix, float xOffset, float yOffset, float zOffset)
	{
		renderType.setupRenderState();
		ShaderInstance shader = RenderSystem.getShader();
		if (shader != null)
		{
			if (shader.getUniform("ModelOffset") != null)
				shader.getUniform("ModelOffset").set(xOffset, yOffset, zOffset);
			buffer.bind();

			buffer.drawWithShader(modelViewMatrix, projectionMatrix, shader);

			if (shader.getUniform("ModelOffset") != null)
				shader.getUniform("ModelOffset").set(0.0F, 0.0F, 0.0F);
			VertexBuffer.unbind();
		}
		renderType.clearRenderState();
	}

	protected static Collection<BlockPos> mapPosesTo(Collection<BlockPos> poses, Vec3i renderPos)
	{
		List<BlockPos> newPoses = new ArrayList<>(poses.size());
		int x = renderPos.getX();
		int y = renderPos.getY();
		int z = renderPos.getZ();
		for (BlockPos pos : poses)
			newPoses.add(pos.offset(-x, -y, -z));
		return newPoses;
	}

	protected boolean shouldRender(Vec3i camPos)
	{
		return true;
	}

	protected void updateData(Minecraft mc, ItemStack stack)
	{
		// Hit pos
		BlockPos hPos;
		Direction hDir;
		boolean hBlock;
		HitResult hitResult = mc.hitResult;
		if (hitResult instanceof BlockHitResult bHitResult)
		{
			hPos = bHitResult.getBlockPos();
			hDir = bHitResult.getDirection();
			hBlock = bHitResult.getType() == HitResult.Type.BLOCK;
		}
		else
		{
			hPos = null;
			hDir = null;
			hBlock = false;
		}

		if (((this.hitPos != null && !this.hitPos.equals(hPos)) || (hPos != null && !hPos.equals(this.hitPos)) || ((this.hitPos == null) != (hPos == null))) || hDir != this.hitFace || hBlock != this.hitBlock)
		{
			this.hitPos = hPos;
			this.hitFace = hDir;
			this.hitBlock = hBlock;
			this.needsCompiled = true;
		}

		// Hit vec
		Vec3 hVec;
		if (hitResult instanceof BlockHitResult bHitResult)
		{
			hVec = bHitResult.getLocation();
		}
		else
		{
			hVec = null;
		}

		if (((this.hitVec != null && !this.hitVec.equals(hVec)) || (hVec != null && !hVec.equals(this.hitVec))))
		{
			this.hitVec = hVec;
			this.needsCompiled = true;
		}
	}

	protected abstract void compileInfo(Minecraft mc, ItemStack stack, PoseStack poseStack, BufferBuilder buffBuilder, double camX, double camY, double camZ, RenderInfo renderInfo);

	protected void close()
	{
		this.lineBuffer.close();
	}

	public static void needsUpdated()
	{
		if (instance != null)
			instance.needsCompiled = true;
	}

	public static void clear()
	{
		shouldClear = true;
	}

	protected abstract static class ForCorners extends BuildingToolRenderer
	{
		protected boolean selectionChanged = false;
		protected boolean itemChanged = false;
		private ItemStack oldStack = ItemStack.EMPTY;
		boolean wasHoldingShift = false;
		@Nullable
		CornerType oldCorner = null;
		@Nullable
		protected BlockPos secondPos;
		private final VertexBuffer targetBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
		private boolean targetBufferEmpty = true;

		@Override
		protected boolean shouldRender(Vec3i camPos)
		{
			return true;
		}

		@Override
		protected void updateData(Minecraft mc, ItemStack stack)
		{
			super.updateData(mc, stack);
			Optional<BlockPos> pos0 = BuildingToolItem.getPos(stack, 0);
			Optional<BlockPos> pos1 = BuildingToolItem.getPos(stack, 1);

			boolean pos0Flag = arePosesDifferent(pos0, this.pos);
			boolean pos1Flag = arePosesDifferent(pos1, this.secondPos);
			CornerType corner = BuildingToolItem.getSelectedCorner(stack);

			boolean isHoldingShift = mc.player.isShiftKeyDown();

			this.pos = pos0.orElse(null);
			this.secondPos = pos1.orElse(null);

			if (pos0Flag || pos1Flag || this.wasHoldingShift != isHoldingShift || this.oldCorner != corner)
				this.needsCompiled = true;

			this.selectionChanged = pos0Flag || pos1Flag || this.oldCorner != corner;
			this.itemChanged = (oldStack == null) != (stack == null) || (oldStack != null && !ItemStack.matches(oldStack, stack));

			this.wasHoldingShift = isHoldingShift;
			this.oldCorner = corner;
			this.oldStack = stack.copy();
		}

		private boolean arePosesDifferent(Optional<BlockPos> opPos, BlockPos compareTo)
		{
			return opPos.isPresent() ? !opPos.get().equals(compareTo) : compareTo != null;
		}

		protected abstract float[] getOutlineRGB();

		@Override
		protected void compileInfo(Minecraft mc, ItemStack stack, PoseStack poseStack, BufferBuilder buffBuilder, double camX, double camY, double camZ, RenderInfo renderInfo)
		{
			boolean hasPosA = this.pos != null;
			boolean hasPosB = this.secondPos != null;
			float selectionPointAlpha = 0.5F;
			Vec3i renderPos = this.renderPos;
			renderInfo.renderFlagA = false;

			if (hasPosA && !hasPosB)
			{
				// Render selection point A only
				renderPos = this.pos;
				IRenderBase.makeLineBox(poseStack, buffBuilder, BoundingBox.fromCorners(Vec3i.ZERO, Vec3i.ZERO), 0.75F, 0.0F, 0.0F, selectionPointAlpha);
			}

			if (!hasPosA && hasPosB)
			{
				// Render selection point B only
				renderPos = this.secondPos;
				IRenderBase.makeLineBox(poseStack, buffBuilder, BoundingBox.fromCorners(Vec3i.ZERO, Vec3i.ZERO), 0.0F, 0.0F, 0.75F, selectionPointAlpha);
			}

			if (hasPosA && hasPosB)
			{
				// Render a fully selected region
				CornerType selectedCorner = BuildingToolItem.getSelectedCorner(stack);
				float[] rgb = getOutlineRGB();
				BoundingBox bb = BoundingBox.fromCorners(this.pos, this.secondPos);
				renderPos = new Vec3i(bb.minX(), bb.minY(), bb.minZ());
				double pointWidth = 0.53;
				Vec3 pointWidthVec = new Vec3(pointWidth, pointWidth, pointWidth);
				Vec3 posA = Vec3.atCenterOf(this.pos.subtract(renderPos));
				Vec3 posB = Vec3.atCenterOf(this.secondPos.subtract(renderPos));

				double cornerWidth = BuildingToolBounds.CORNER_WIDTH;
				Vec3 cornerWidthVec = new Vec3(cornerWidth, cornerWidth, cornerWidth);
				double range = BuildingToolBounds.CLICK_RANGE;
				Vec3 lookVec = mc.player.getLookAngle();
				Vec3 playerPos = mc.player.getEyePosition();
				Vec3 rp = new Vec3(renderPos.getX(), renderPos.getY(), renderPos.getZ());

				// Render green corner box if hovered
				boolean hasAllCorners = this.hasAllCorners();
				SmartBoundingBox bounds = SmartBoundingBox.fromCorners(Positions.blockPos(posA), Positions.blockPos(posB));
				for (SmartBoundingBox.Corner corner : hasAllCorners ? bounds.allCorners() : bounds.selectionPointCorners())
				{
					Vec3i cornerPos = corner.getPoint();
					Vec3 c = new Vec3(cornerPos.getX(), cornerPos.getY(), cornerPos.getZ());
					AABB aabb = new AABB(c.subtract(cornerWidthVec).add(rp), c.add(cornerWidthVec).add(rp));
					if (selectedCorner == corner.type || aabb.clip(playerPos, playerPos.add(lookVec.multiply(range, range, range))).isPresent())
					{
						IRenderBase.makeBox(poseStack, buffBuilder, c.subtract(cornerWidthVec), c.add(cornerWidthVec), 0.0F, 0.70F, 0.30F, 0.5F);
						renderInfo.renderFlagA = true;
						break;
					}
				}

				// Render the selection points
				if (hasAllCorners)
				{
					IRenderBase.makeBox(poseStack, buffBuilder, posA.subtract(pointWidthVec), posA.add(pointWidthVec), 0.75F, 0.0F, 0.0F, selectionPointAlpha);
					IRenderBase.makeBox(poseStack, buffBuilder, posB.subtract(pointWidthVec), posB.add(pointWidthVec), 0.0F, 0.0F, 0.75F, selectionPointAlpha);
				}
				else
				{
					IRenderBase.makeLineBox(poseStack, buffBuilder, BlockPos.containing(posA), 0.75F, 0.0F, 0.0F, selectionPointAlpha);
					IRenderBase.makeLineBox(poseStack, buffBuilder, BlockPos.containing(posB), 0.0F, 0.0F, 0.75F, selectionPointAlpha);
				}

				// Render bounding box if the tool uses a full bounding box
				if (hasAllCorners)
				{
					IRenderBase.makeLineBox(poseStack, buffBuilder, BoundingBox.fromCorners(Vec3i.ZERO, bb.getLength()), rgb[0], rgb[1], rgb[2], 0.3F);
					if (selectedCorner != null)
					{
						double boxWidth = 0.508;
						Vec3 boxWidthVec = new Vec3(boxWidth, boxWidth, boxWidth);
						IRenderBase.makeBox(poseStack, buffBuilder, Vec3.atCenterOf(Vec3i.ZERO).subtract(boxWidthVec), Vec3.atCenterOf(bb.getLength()).add(boxWidthVec), rgb[0], rgb[1], rgb[2], 0.1F);
					}
				}
			}
			renderInfo.renderPos = renderPos;
		}

		protected boolean hasAllCorners()
		{
			return currentMode instanceof BuildingToolMode.ForCorners cornerMode && cornerMode.hasAllCorners();
		}

		protected void compileTarget(Minecraft mc, ItemStack stack, PoseStack poseStack, BufferBuilder buffBuilder, double camX, double camY, double camZ, RenderInfo renderInfo)
		{
			// If clone, hide the targeting box when the area is selected
			if (currentMode == BuildingToolModes.CLONE && this.pos != null && this.secondPos != null)
				return;
			if (renderInfo.renderFlagA)
			{
				return;
			}
			if (this.hitPos != null)
			{
				// Render the target point that would get selected
				boolean posA = !mc.player.isShiftKeyDown();
				float[] color = new float[] { posA ? 0.75F : 0.0F, 0.0F, posA ? 0.0F : 0.75F };
				float selectionPointAlpha = 0.3F;

				if (mc.level.getBlockState(this.hitPos).isAir())
				{
					double min = 0.15;
					double max = 0.85;
					IRenderBase.makeBox(poseStack, buffBuilder, new Vec3(min, min, min), new Vec3(max, max, max), color[0], color[1], color[2], selectionPointAlpha);
				}
				else
				{
					double min = 0.01;
					double max = 0.99;
					IRenderBase.makeLineBox(poseStack, buffBuilder, min, min, min, max, max, max, color[0], color[1], color[2], selectionPointAlpha);
				}
			}
		}

		@Override
		protected RenderInfo compile(Minecraft mc, LocalPlayer player, ItemStack stack, double camX, double camY, double camZ)
		{
			RenderInfo ret = super.compile(mc, player, stack, camX, camY, camZ);

			this.targetBuffer.bind();
			PoseStack poseStack = new PoseStack();
			BufferBuilder buffBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
			this.compileTarget(mc, stack, poseStack, buffBuilder, camX, camY, camZ, ret);
			MeshData mesh = buffBuilder.build();
			if (mesh != null)
			{
				this.targetBuffer.upload(mesh);
				this.targetBufferEmpty = false;
			}
			else
			{
				this.targetBufferEmpty = true;
			}
			VertexBuffer.unbind();
			return ret;
		}

		@Override
		protected void render(Minecraft mc, LocalPlayer player, ItemStack stack, Matrix4f modelViewMatrix, Matrix4f projectionMatrix, double camX, double camY, double camZ)
		{
			Vec3i camPos = mc.gameRenderer.getMainCamera().getBlockPosition();
			int dist = 300;
			if ((this.pos != null && this.pos.closerThan(camPos, dist)) || (this.secondPos != null && this.secondPos.closerThan(camPos, dist)))
				super.render(mc, player, stack, modelViewMatrix, projectionMatrix, camX, camY, camZ);

			if (!this.targetBufferEmpty && this.hitPos != null)
			{
				this.drawWithRenderType(SGRenderType.worldWireframe(true), this.targetBuffer, modelViewMatrix, projectionMatrix, this.hitPos.getX() - (float) camX, this.hitPos.getY() - (float) camY, this.hitPos.getZ() - (float) camZ);
			}
		}

		@Override
		protected void close()
		{
			super.close();
			this.targetBuffer.close();
		}
	}
}
