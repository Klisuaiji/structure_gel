package com.legacy.structure_gel.core.mixin.common;

import com.legacy.structure_gel.api.structure.StructureAccessHelper;
import com.legacy.structure_gel.api.structure.base.IPieceBuilderModifier;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import java.util.function.Predicate;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.Structure.GenerationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Structure.class})
public class StructureMixin {
   @Inject(
      at = {@At("HEAD")},
      method = {"generate"},
      cancellable = true
   )
   private void checkDimension(
      RegistryAccess registryAccess,
      ChunkGenerator chunkGen,
      BiomeSource biomeSource,
      RandomState randomState,
      StructureTemplateManager structureTemplateManager,
      long levelSeed,
      ChunkPos chunkPos,
      int refs,
      LevelHeightAccessor levelHeight,
      Predicate<Holder<Biome>> biomeTest,
      CallbackInfoReturnable<StructureStart> callback
   ) {
      boolean validDim = true;
      if (levelHeight instanceof ChunkAccessMixin.Access access && access.getHeightAccessor() instanceof ServerLevel level) {
         validDim = StructureAccessHelper.isValidDimension(
            registryAccess.registryOrThrow(Registries.STRUCTURE).getResourceKey((Structure)this), level.dimension()
         );
      }

      if (!validDim) {
         callback.setReturnValue(StructureStart.INVALID_START);
      }
   }

   @ModifyExpressionValue(
      at = {@At(
         value = "INVOKE",
         target = "net/minecraft/world/level/levelgen/structure/Structure$GenerationStub.getPiecesBuilder()Lnet/minecraft/world/level/levelgen/structure/pieces/StructurePiecesBuilder;"
      )},
      method = {"generate"}
   )
   private StructurePiecesBuilder modifyPieceBuilder(StructurePiecesBuilder pieceBuilder, @Local(ordinal = 0) GenerationContext context) {
      Structure var4 = (Structure)this;
      if (var4 instanceof IPieceBuilderModifier modifier) {
         modifier.modifyPieceBuilder(pieceBuilder, context);
      }

      return pieceBuilder;
   }
}
