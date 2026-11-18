package com.legacy.structure_gel.core.mixin.client;

import com.legacy.structure_gel.core.SGConfig;
import com.legacy.structure_gel.core.asm_hooks.StructureBlockRendererHooks;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.StructureBlockRenderer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@OnlyIn(Dist.CLIENT)
@Mixin({StructureBlockRenderer.class})
public class StructureBlockRendererMixin {
   @Inject(
      at = {@At("HEAD")},
      method = {"render"}
   )
   private void render(
      StructureBlockEntity structureBlock,
      float partialTicks,
      PoseStack posStack,
      MultiBufferSource buffSource,
      int pPackedLight,
      int pPackedOverlay,
      CallbackInfo callback
   ) {
      StructureBlockRendererHooks.renderName(structureBlock, posStack, buffSource);
   }

   @WrapOperation(
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/level/block/state/BlockState;isAir()Z"
      )},
      method = {"renderInvisibleBlocks"}
   )
   private boolean renderInvisibleAir(BlockState target, Operation<Boolean> original) {
      return (Boolean)original.call(new Object[]{target}) && (Boolean)SGConfig.CLIENT.showAir.get();
   }

   @WrapOperation(
      at = {@At(
         value = "INVOKE",
         target = "net/minecraft/world/level/block/state/BlockState.is(Lnet/minecraft/world/level/block/Block;)Z",
         ordinal = 0
      )},
      method = {"renderInvisibleBlocks"}
   )
   private boolean renderInvisibleVoid(BlockState target, Block block, Operation<Boolean> original) {
      return (Boolean)original.call(new Object[]{target, block}) && (Boolean)SGConfig.CLIENT.showVoid.get();
   }

   @WrapOperation(
      at = {@At(
         value = "INVOKE",
         target = "net/minecraft/world/level/block/state/BlockState.is(Lnet/minecraft/world/level/block/Block;)Z",
         ordinal = 1
      )},
      method = {"renderInvisibleBlocks"}
   )
   private boolean renderInvisibleBarrier(BlockState target, Block block, Operation<Boolean> original) {
      return (Boolean)original.call(new Object[]{target, block}) && (Boolean)SGConfig.CLIENT.showBarrier.get();
   }

   @WrapOperation(
      at = {@At(
         value = "INVOKE",
         target = "net/minecraft/world/level/block/state/BlockState.is(Lnet/minecraft/world/level/block/Block;)Z",
         ordinal = 2
      )},
      method = {"renderInvisibleBlocks"}
   )
   private boolean renderInvisibleLight(BlockState target, Block block, Operation<Boolean> original) {
      return (Boolean)original.call(new Object[]{target, block}) && (Boolean)SGConfig.CLIENT.showLight.get();
   }
}
