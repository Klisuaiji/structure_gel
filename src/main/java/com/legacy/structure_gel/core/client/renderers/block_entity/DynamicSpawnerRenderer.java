package com.legacy.structure_gel.core.client.renderers.block_entity;

import com.legacy.structure_gel.core.block_entity.DynamicSpawnerBlockEntity;
import com.legacy.structure_gel.core.dynamic_spawner.DynamicSpawner;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

public class DynamicSpawnerRenderer implements BlockEntityRenderer<DynamicSpawnerBlockEntity>
{
	public DynamicSpawnerRenderer(BlockEntityRendererProvider.Context context)
	{
	}

	@Override
	public void render(DynamicSpawnerBlockEntity blockEntity, float partialTicks, PoseStack posStack, MultiBufferSource buff, int packedLight, int overlay)
	{
		posStack.pushPose();
		posStack.translate(0.5D, 0.0D, 0.5D);
		DynamicSpawner spawner = blockEntity.getSpawner();
		Entity entity = spawner.getOrCreateDisplayEntity(blockEntity.getLevel(), blockEntity.getBlockPos());
		if (entity != null)
		{
			float scale = 0.53125F;
			float largestLength = Math.max(entity.getBbWidth(), entity.getBbHeight());
			if (largestLength > 1.0D)
			{
				scale /= largestLength;
			}

			posStack.translate(0.0D, 0.4D, 0.0D);
			posStack.mulPose(Axis.YP.rotationDegrees((float) Mth.lerp(partialTicks, spawner.getoSpin(), spawner.getSpin()) * 10.0F));
			posStack.translate(0.0F, -0.2F, 0.0F);
			posStack.mulPose(Axis.XP.rotationDegrees(-30.0F));
			posStack.scale(scale, scale, scale);
			Minecraft.getInstance().getEntityRenderDispatcher().render(entity, 0.0D, 0.0D, 0.0D, 0.0F, posStack, buff, packedLight);
		}

		posStack.popPose();
	}
}