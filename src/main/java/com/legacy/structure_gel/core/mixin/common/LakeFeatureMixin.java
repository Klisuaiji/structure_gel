package com.legacy.structure_gel.core.mixin.common;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.legacy.structure_gel.api.structure.StructureAccessHelper;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.LakeFeature;

@SuppressWarnings("deprecation")
@Mixin(LakeFeature.class)
public class LakeFeatureMixin
{
	@Inject(at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/core/BlockPos;below(I)Lnet/minecraft/core/BlockPos;"), method = "place", cancellable = true)
	private void checkForStructures(FeaturePlaceContext<LakeFeature.Configuration> context, CallbackInfoReturnable<Boolean> callback)
	{
		if (!context.level().getLevel().structureManager().startsForStructure(new ChunkPos(context.origin()), s -> StructureAccessHelper.isLakeProof(context.level().registryAccess(), s)).isEmpty())
			callback.setReturnValue(false);
	}
}
