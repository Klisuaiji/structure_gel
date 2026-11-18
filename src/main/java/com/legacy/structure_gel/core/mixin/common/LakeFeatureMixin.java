package com.legacy.structure_gel.core.mixin.common;

import com.legacy.structure_gel.core.asm_hooks.LakeFeatureHooks;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.LakeFeature;
import net.minecraft.world.level.levelgen.feature.LakeFeature.Configuration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({LakeFeature.class})
public class LakeFeatureMixin {
   @Inject(
      at = {@At(
         value = "INVOKE_ASSIGN",
         target = "Lnet/minecraft/core/BlockPos;below(I)Lnet/minecraft/core/BlockPos;"
      )},
      method = {"place"},
      cancellable = true
   )
   private void checkForStructures(FeaturePlaceContext<Configuration> context, CallbackInfoReturnable<Boolean> callback) {
      if (LakeFeatureHooks.checkForStructures(context)) {
         callback.setReturnValue(false);
      }
   }
}
