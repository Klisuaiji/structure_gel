package com.legacy.structure_gel.core.mixin.common;

import java.util.function.Predicate;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.legacy.structure_gel.api.structure.StructureAccessHelper;
import com.legacy.structure_gel.api.structure.base.IPieceBuilderModifier;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

@Mixin(Structure.class)
public class StructureMixin
{
	// Prevents structure generation if this structure isn't allowed in the dimension
	@Inject(at = @At(value = "HEAD"), method = "generate", cancellable = true)
	private void checkDimension(Holder<Structure> structure, ResourceKey<Level> level, RegistryAccess registryAccess, ChunkGenerator chunkGen, BiomeSource biomeSource, RandomState randomState, StructureTemplateManager structureTemplateManager, long levelSeed, ChunkPos chunkPos, int refs, LevelHeightAccessor levelHeight, Predicate<Holder<Biome>> biomeTest, CallbackInfoReturnable<StructureStart> callback)
	{
		// So they added the structure and level key as method args in 1.21.4. 
		// I don't need the commented code below this anymore, but they only added 
		// it for JVM logging. So it'll probably vanish randomly in like 4 updates

		//boolean validDim = true;
		//if (levelHeight instanceof ChunkAccessMixin.Access access && access.getHeightAccessor() instanceof ServerLevel level) // ChunkAccess on worldgen, ServerLevel otherwise
		//	validDim = StructureAccessHelper.isValidDimension(registryAccess.lookupOrThrow(Registries.STRUCTURE).getResourceKey((Structure) (Object) this), level.dimension());
		boolean validDim = StructureAccessHelper.isValidDimension(structure.unwrapKey(), level);

		if (!validDim)
			callback.setReturnValue(StructureStart.INVALID_START);
	}

	//@formatter:off
	@ModifyExpressionValue(
			at = @At(
					value = "INVOKE",
					target ="net/minecraft/world/level/levelgen/structure/Structure$GenerationStub.getPiecesBuilder()Lnet/minecraft/world/level/levelgen/structure/pieces/StructurePiecesBuilder;"
					), 
			method = "generate")
	//@formatter:on
	private StructurePiecesBuilder modifyPieceBuilder(StructurePiecesBuilder pieceBuilder, @Local(ordinal = 0) Structure.GenerationContext context)
	{
		if ((Structure) (Object) this instanceof IPieceBuilderModifier modifier)
			modifier.modifyPieceBuilder(pieceBuilder, context);
		return pieceBuilder;
	}
}
