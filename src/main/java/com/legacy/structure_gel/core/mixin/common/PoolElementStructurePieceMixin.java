package com.legacy.structure_gel.core.mixin.common;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.legacy.structure_gel.core.structure.jigsaw.SinglePoolElementExtension;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;

@Mixin(PoolElementStructurePiece.class)
public class PoolElementStructurePieceMixin
{
	@Inject(at = @At("HEAD"), method = "place")
	private void placeHead(WorldGenLevel level, StructureManager structureManager, ChunkGenerator chunkGenerator, RandomSource random, BoundingBox bounds, BlockPos pos, boolean keepJigsaws, CallbackInfo callback)
	{
		PoolElementStructurePiece me = (PoolElementStructurePiece) (Object) this;
		if (me.getElement() instanceof SinglePoolElementExtension singleElement)
		{
			singleElement.structure_gel$setPiece(me);
		}
	}
}
