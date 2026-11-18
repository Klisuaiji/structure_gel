package com.legacy.structure_gel.core.mixin.common;

import com.legacy.structure_gel.api.structure.GelTemplateStructurePiece;
import com.legacy.structure_gel.api.structure.base.IModifyState;
import com.legacy.structure_gel.core.capability.misc.StructureSettingsData;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({TemplateStructurePiece.class})
public class TemplateStructurePieceMixin {
   @Inject(
      at = {@At(
         value = "INVOKE",
         ordinal = 0,
         target = "Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructureTemplate;filterBlocks(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructurePlaceSettings;Lnet/minecraft/world/level/block/Block;)Ljava/util/List;"
      )},
      method = {"postProcess"}
   )
   private void dataHandlerHook(
      WorldGenLevel level,
      StructureManager structureMananger,
      ChunkGenerator chunkGen,
      RandomSource rand,
      BoundingBox bounds,
      ChunkPos chunkPos,
      BlockPos pos,
      CallbackInfo callback
   ) {
      TemplateStructurePiece me = (TemplateStructurePiece)this;
      GelTemplateStructurePiece.processDataHandlers(me, me.template(), me.templatePosition(), me.placeSettings(), level, rand, bounds);
   }

   @Inject(
      at = {@At("HEAD")},
      method = {"postProcess"}
   )
   private void modifySettingsHook(
      WorldGenLevel level,
      StructureManager structureMananger,
      ChunkGenerator chunkGen,
      RandomSource rand,
      BoundingBox bounds,
      ChunkPos chunkPos,
      BlockPos pos,
      CallbackInfo callback
   ) {
      TemplateStructurePiece me = (TemplateStructurePiece)this;
      if (me instanceof IModifyState stateModifier) {
         ((StructureSettingsData)me.placeSettings()).structure_gel$setStateModifier(stateModifier);
      }
   }
}
