package com.legacy.structure_gel.core.mixin.common;

import com.legacy.structure_gel.core.structure.jigsaw.GelSinglePoolElement;
import com.legacy.structure_gel.core.structure.jigsaw.SinglePoolElementExtension;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin({SinglePoolElement.class})
public class SinglePoolElementMixin implements SinglePoolElementExtension {
   @Nullable
   private StructurePiece structure_gel$piece = null;

   @Override
   public void structure_gel$setPiece(@Nullable StructurePiece piece) {
      this.structure_gel$piece = piece;
   }

   @Override
   public StructurePiece structure_gel$getPiece() {
      return this.structure_gel$piece;
   }

   @Inject(
      locals = LocalCapture.CAPTURE_FAILSOFT,
      at = {@At(
         value = "INVOKE",
         ordinal = 0,
         target = "Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructureTemplate;processBlockInfos(Lnet/minecraft/world/level/ServerLevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructurePlaceSettings;Ljava/util/List;)Ljava/util/List;"
      )},
      method = {"place"}
   )
   private void dataHandlerHook(
      StructureTemplateManager structureTemplateManager,
      WorldGenLevel level,
      StructureManager structureManager,
      ChunkGenerator chunkGen,
      BlockPos templatePos,
      BlockPos jigsawPos,
      Rotation rotation,
      BoundingBox bounds,
      RandomSource rand,
      boolean isLegacy,
      CallbackInfoReturnable<Boolean> callback,
      StructureTemplate local_template,
      StructurePlaceSettings local_placeSettings
   ) {
      if (this.structure_gel$piece != null) {
         GelSinglePoolElement.processDataHandlers(
            this.structure_gel$piece, local_template, templatePos, jigsawPos, local_placeSettings, level, rotation, bounds, rand
         );
      }
   }
}
