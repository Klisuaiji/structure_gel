package com.legacy.structure_gel.core.client.renderers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.joml.Matrix4f;

import com.legacy.structure_gel.api.util.Positions;
import com.legacy.structure_gel.core.StructureGelMod;
import com.legacy.structure_gel.core.client.SGRenderType;
import com.legacy.structure_gel.core.client.SGShaders;
import com.legacy.structure_gel.core.structure.StructureInfo;
import com.mojang.blaze3d.buffers.BufferUsage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.CompiledShaderProgram;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * Renders structure bounding boxes in the world
 * 
 */
public class StructureBoundsRenderer implements IRenderBase
{
	private static final Map<ResourceLocation, List<StructureBoundsRenderer>> STRUCTURE_INFOS = new HashMap<>();
	private static boolean shouldClear = false;
	private final BlockPos pos;
	private final StructureInfo structureInfo;
	private final VertexBuffer vertexBuffer = new VertexBuffer(BufferUsage.STATIC_WRITE);
	private boolean needsUpdated = true;
	private boolean vertexBufferEmpty = true;

	private StructureBoundsRenderer(StructureInfo structureInfo)
	{
		this.pos = structureInfo.fullBounds().getCenter();
		this.structureInfo = structureInfo;
	}

	public static void render(Minecraft mc, Matrix4f modelViewMatrix, Matrix4f projectionMatrix, double camX, double camY, double camZ)
	{
		if (shouldClear)
		{
			shouldClear = false;
			for (List<StructureBoundsRenderer> rendererList : STRUCTURE_INFOS.values())
			{
				for (StructureBoundsRenderer renderer : rendererList)
				{
					renderer.vertexBuffer.close();
				}
			}
			STRUCTURE_INFOS.clear();
		}

		if (StructureGelMod.proxy.shouldViewBounds())
		{
			ResourceLocation dimensionName = mc.level.dimension().location();
			List<StructureBoundsRenderer> renderers = STRUCTURE_INFOS.get(dimensionName);
			if (renderers != null)
			{
				Vec3i camPos = Positions.vec3i(camX, camY, camZ);
				for (int i = renderers.size() - 1; i > -1; i--)
				{
					var renderer = renderers.get(i);
					if (renderer.pos.closerThan(camPos, 300))
					{
						renderer.renderBounds(mc, modelViewMatrix, projectionMatrix, camX, camY, camZ);
					}
				}
			}
		}
	}

	private void renderBounds(Minecraft mc, Matrix4f modelViewMatrix, Matrix4f projectionMatrix, double camX, double camY, double camZ)
	{
		// Compile
		this.vertexBuffer.bind();
		if (this.needsUpdated /*|| true*/)
		{
			this.needsUpdated = false;
			PoseStack compileStack = new PoseStack();
			MeshData mesh = this.compileBounds(mc, compileStack, Tesselator.getInstance(), camX, camY, camZ);
			if (mesh != null)
			{
				this.vertexBuffer.upload(mesh);
				this.vertexBufferEmpty = false;
			}
			else
			{
				this.vertexBufferEmpty = true;
			}
		}

		// Draw
		if (!this.vertexBufferEmpty)
		{
			BoundingBox bb = this.structureInfo.fullBounds();
			this.drawWithRenderType(SGRenderType.worldWireframe(true), this.vertexBuffer, modelViewMatrix, projectionMatrix,bb.minX() - (float) camX, bb.minY() - (float) camY, bb.minZ() - (float) camZ);
		}
	}
	
	protected void drawWithRenderType(RenderType renderType, VertexBuffer buffer, Matrix4f modelViewMatrix, Matrix4f projectionMatrix, float xOffset, float yOffset, float zOffset)
	{
		renderType.setupRenderState();
		CompiledShaderProgram shader = RenderSystem.getShader();
		if (shader != null)
		{
			if (shader.MODEL_OFFSET != null)
				shader.MODEL_OFFSET.set(xOffset, yOffset, zOffset);
			buffer.bind();

			buffer.drawWithShader(modelViewMatrix, projectionMatrix, shader);

			if (shader.MODEL_OFFSET != null)
				shader.MODEL_OFFSET.set(0.0F, 0.0F, 0.0F);
			VertexBuffer.unbind();
		}
		renderType.clearRenderState();
	}

	@Nullable
	private MeshData compileBounds(Minecraft mc, PoseStack poseStack, Tesselator tesselator, double camX, double camY, double camZ)
	{
		BufferBuilder buffBuilder = tesselator.begin(VertexFormat.Mode.QUADS, SGShaders.WORLD_WIREFRAME.vertexFormat());
		BoundingBox fullBounds = this.structureInfo.fullBounds();
		int minX = fullBounds.minX();
		int minY = fullBounds.minY();
		int minZ = fullBounds.minZ();
		IRenderBase.makeLineBox(poseStack, buffBuilder, BoundingBox.fromCorners(Vec3i.ZERO, fullBounds.getLength()), 0.12, 1.0F, 1.0F, 1.0F);

		for (int i = this.structureInfo.pieces().size() - 1; i > -1; i--)
		{
			StructureInfo.PieceInfo pieceInfo = this.structureInfo.pieces().get(i);
			float[] rgb = pieceInfo.getColorFloats();
			BoundingBox pieceBounds = pieceInfo.bounds();
			IRenderBase.makeLineBox(poseStack, buffBuilder, BoundingBox.fromCorners(Vec3i.ZERO, pieceBounds.getLength()).moved(pieceBounds.minX() - minX, pieceBounds.minY() - minY, pieceBounds.minZ() - minZ), rgb[0], rgb[1], rgb[2]);
		}

		return buffBuilder.build();
	}

	public static void addInfo(StructureInfo structureInfo)
	{
		StructureGelMod.LOGGER.log("Recieved structure info at ({}) in {}", structureInfo.fullBounds().getCenter().toShortString(), structureInfo.dimension());

		STRUCTURE_INFOS.computeIfAbsent(structureInfo.dimension(), type -> new ArrayList<>()).add(new StructureBoundsRenderer(structureInfo));

		// Lag tester
		/*int amount = 20;
		for (int x = 0; x < amount; x++)
		{
			for (int y = 0; y < amount; y++)
			{
				for (int z = 0; z < amount; z++)
				{
					BoundingBox newFull = structureInfo.fullBounds().moved(x, y, z);
					var pieces = structureInfo.pieces();
					int size = pieces.size();
					var newPieces = new ArrayList<StructureInfo.PieceInfo>(size);
					for (int p = 0; p < size; p++)
					{
						var pieceInfo = pieces.get(p);
						newPieces.add(new StructureInfo.PieceInfo(pieceInfo.bounds().moved(x, y, z), pieceInfo.color()));
					}
					STRUCTURE_INFOS.get(structureInfo.dimensionType()).add(new StructureBoundsRenderer(new StructureInfo(structureInfo.dimensionType(), newFull, newPieces)));
				}
			}
		}*/
	}

	public static void clear()
	{
		shouldClear = true;
	}
}
