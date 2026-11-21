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

import java.util.Optional;

import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Level;
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
	private void checkDimension(RegistryAccess registryAccess, ChunkGenerator chunkGen, BiomeSource biomeSource, RandomState randomState, StructureTemplateManager structureTemplateManager, long levelSeed, ChunkPos chunkPos, int refs, LevelHeightAccessor levelHeight, Predicate<Holder<Biome>> biomeTest, CallbackInfoReturnable<StructureStart> callback)
	{
		// 1.21.1 Structure#generate has no Holder<Structure>/ResourceKey<Level> args (added in 1.21.4).
		// Recover both from the registry and the height accessor instead.
		Optional<ResourceKey<Structure>> structureKey = registryAccess.registryOrThrow(Registries.STRUCTURE).getResourceKey((Structure) (Object) this);

		ResourceKey<Level> dimension = null;
		if (levelHeight instanceof Level level)
			dimension = level.dimension();
		else if (levelHeight instanceof ChunkAccessMixin.Access access && access.getHeightAccessor() instanceof Level level)
			dimension = level.dimension();

		boolean validDim = dimension != null && StructureAccessHelper.isValidDimension(structureKey, dimension);

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
