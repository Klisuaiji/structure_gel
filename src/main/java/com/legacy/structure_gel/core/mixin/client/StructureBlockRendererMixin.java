package com.legacy.structure_gel.core.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.legacy.structure_gel.core.SGConfig;
import com.legacy.structure_gel.core.client.ClientUtil;
import com.legacy.structure_gel.core.util.SGText;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.StructureBlockRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.StructureMode;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Renders text in the world on top of Structure Blocks, similarly to how it did
 * before 1.13.
 *
 * @author Bailey
 */
@OnlyIn(Dist.CLIENT)
@Mixin(StructureBlockRenderer.class)
public class StructureBlockRendererMixin
{
	// Display the name over the structure block

	@Inject(at = @At("HEAD"), method = "render")
	private void render(StructureBlockEntity structureBlock, float partialTicks, PoseStack posStack, MultiBufferSource buffer, int pPackedLight, int pPackedOverlay, CallbackInfo callback)
	{
		if (!SGConfig.CLIENT.showStructureBlockInfo())
			return;

		Minecraft mc = Minecraft.getInstance();
		if (structureBlock.getLevel() != null && mc.player != null && ClientUtil.rayTrace(structureBlock.getLevel(), mc.player).getBlockPos().equals(structureBlock.getBlockPos()))
		{
			String name = structureBlock.getMode().getSerializedName();
			MutableComponent mode = Component.translatable("structure_block.mode." + name).setStyle(SGText.VALUE_LABEL_STYLE).append(": ");
			MutableComponent text = Component.literal(structureBlock.getMode() == StructureMode.DATA ? structureBlock.getMetaData() : structureBlock.getStructureName());
			ClientUtil.renderName(Component.empty().append(mode).append(text), structureBlock.getLevel(), structureBlock.getBlockPos(), posStack, buffer, 220);
		}
	}

	// Allow the checkboxes in the screen to control what blocks to render or not render

	@WrapOperation(at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;isAir()Z"), method = "renderInvisibleBlocks")
	private boolean renderInvisibleAir(BlockState target, Operation<Boolean> original)
	{
		return original.call(target) && SGConfig.CLIENT.showAir.get();
	}

	@WrapOperation(at = @At(value = "INVOKE", target = "net/minecraft/world/level/block/state/BlockState.is(Lnet/minecraft/world/level/block/Block;)Z", ordinal = 0), method = "renderInvisibleBlocks")
	private boolean renderInvisibleVoid(BlockState target, Block block, Operation<Boolean> original)
	{
		return original.call(target, block) && SGConfig.CLIENT.showVoid.get();
	}

	@WrapOperation(at = @At(value = "INVOKE", target = "net/minecraft/world/level/block/state/BlockState.is(Lnet/minecraft/world/level/block/Block;)Z", ordinal = 1), method = "renderInvisibleBlocks")
	private boolean renderInvisibleBarrier(BlockState target, Block block, Operation<Boolean> original)
	{
		return original.call(target, block) && SGConfig.CLIENT.showBarrier.get();
	}

	@WrapOperation(at = @At(value = "INVOKE", target = "net/minecraft/world/level/block/state/BlockState.is(Lnet/minecraft/world/level/block/Block;)Z", ordinal = 2), method = "renderInvisibleBlocks")
	private boolean renderInvisibleLight(BlockState target, Block block, Operation<Boolean> original)
	{
		return original.call(target, block) && SGConfig.CLIENT.showLight.get();
	}
}
