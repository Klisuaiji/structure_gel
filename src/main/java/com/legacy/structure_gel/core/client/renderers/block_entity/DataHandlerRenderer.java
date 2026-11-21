package com.legacy.structure_gel.core.client.renderers.block_entity;

import java.util.List;

import com.legacy.structure_gel.api.data_handler.handlers.DataHandler;
import com.legacy.structure_gel.core.SGConfig;
import com.legacy.structure_gel.core.block.DataHandlerBlock;
import com.legacy.structure_gel.core.block_entity.DataHandlerBlockEntity;
import com.legacy.structure_gel.core.block_entity.DataHandlerBlockEntity.RawHandler;
import com.legacy.structure_gel.core.client.SGRenderType;
import com.legacy.structure_gel.core.client.renderers.IRenderBase;
import com.legacy.structure_gel.core.util.SGText;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class DataHandlerRenderer implements BlockEntityRenderer<DataHandlerBlockEntity>
{
	public DataHandlerRenderer(BlockEntityRendererProvider.Context renderContext)
	{
	}

	@Override
	public void render(DataHandlerBlockEntity dataHandler, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int overlay)
	{
		Minecraft mc = Minecraft.getInstance();
		if (SGConfig.CLIENT.showStructureBlockInfo())
		{
			if (dataHandler.getLevel() != null && mc.player != null && com.legacy.structure_gel.core.client.ClientUtil.rayTrace(dataHandler.getLevel(), mc.player).getBlockPos().equals(dataHandler.getBlockPos()))
			{
				if (dataHandler.getCustomName() == null)
				{
					List<RawHandler> handlers = dataHandler.getHandlers().unwrap();

					MutableComponent mode = SGText.TYPE_LABEL.copy().setStyle(SGText.VALUE_LABEL_STYLE).append(": ");
					MutableComponent text;
					if (handlers.size() == 0)
						text = Component.empty();
					else if (handlers.size() == 1)
						text = Component.literal(handlers.get(0).typeName().toString());
					else
						text = Component.translatable("gui.structure_gel.data_handler.compound");
					com.legacy.structure_gel.core.client.ClientUtil.renderName(Component.empty().append(mode).append(text), dataHandler.getLevel(), dataHandler.getBlockPos(), poseStack, buffer, 220);
				}
				else
				{
					com.legacy.structure_gel.core.client.ClientUtil.renderName(dataHandler.getCustomName(), dataHandler.getLevel(), dataHandler.getBlockPos(), poseStack, buffer, 220);
				}
			}
		}

		Vec3 offset = dataHandler.getOffset();
		if (!Vec3.ZERO.equals(offset))
		{
			BlockState state = dataHandler.getBlockState();
			Vec3 exactPos = Vec3.atBottomCenterOf(BlockPos.ZERO).add(DataHandler.offsetPosition(offset, state.getValue(DataHandlerBlock.FACING), state.getValue(DataHandlerBlock.MIRROR)));

			double r2 = 0.05;
			Vec3 rOffset2 = new Vec3(r2, r2, r2);
			IRenderBase.makeBox(poseStack, buffer.getBuffer(SGRenderType.worldWireframe(false, false)), exactPos.subtract(rOffset2), exactPos.add(rOffset2), 0.5F, 0.7F, 0.9F, 1.0F);

			new IRenderBase.LineBoxBuilder(BlockPos.containing(exactPos)).rgb(0.0F, 0.3F, 0.7F).lineWidth(0.015).build(poseStack, buffer.getBuffer(SGRenderType.worldWireframe(false, false)));
		}
	}

	@Override
	public boolean shouldRenderOffScreen(DataHandlerBlockEntity dataHandler)
	{
		return true;
	}

	@Override
	public AABB getRenderBoundingBox(DataHandlerBlockEntity dataHandler)
	{
		Vec3 offset = dataHandler.getOffset();
		if (Vec3.ZERO.equals(offset))
			return BlockEntityRenderer.super.getRenderBoundingBox(dataHandler);

		double size = Math.max(Math.max(Math.abs(offset.x()), Math.abs(offset.y())), Math.abs(offset.z())) + 3;
		return AABB.ofSize(Vec3.atCenterOf(dataHandler.getBlockPos()), size, size, size);
	}
}