package com.legacy.structure_gel.core.mixin.common;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.legacy.structure_gel.api.structure.GridStructurePlacement;
import com.legacy.structure_gel.api.structure.StructureAccessHelper;
import com.mojang.datafixers.util.Pair;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.Structure;

@Mixin(ChunkGenerator.class)
public class ChunkGeneratorMixin
{
	// Stops a structure from being located if it's in an invalid dimension
	@Inject(at = @At(value = "HEAD"), method = "findNearestMapStructure", cancellable = true)
	private void stopIfNotInDimension(ServerLevel serverLevel, HolderSet<Structure> structures, BlockPos origin, int radius, boolean skipKnownStructures, CallbackInfoReturnable<Pair<BlockPos, Holder<Structure>>> callback)
	{
		var levelKey = serverLevel.dimension();
		if (structures.stream().map(Holder::unwrapKey).noneMatch(structure -> StructureAccessHelper.isValidDimension(structure, levelKey)))
			callback.setReturnValue(null);
	}

	// Allows locating custom structure placements
	@Inject(at = @At(value = "RETURN"), method = "findNearestMapStructure", cancellable = true)
	private void locateGelPlacement(ServerLevel serverLevel, HolderSet<Structure> structures, BlockPos origin, int radius, boolean skipKnownStructures, CallbackInfoReturnable<Pair<BlockPos, Holder<Structure>>> callback)
	{
		var newNearest = GridStructurePlacement.findNearesStructure(serverLevel, structures, origin, radius, skipKnownStructures, (ChunkGenerator) (Object) this, callback.getReturnValue());
		if (newNearest != null)
			callback.setReturnValue(newNearest);
	}
}
