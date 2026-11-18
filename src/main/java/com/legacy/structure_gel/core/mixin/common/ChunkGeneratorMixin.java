package com.legacy.structure_gel.core.mixin.common;

import com.legacy.structure_gel.api.structure.GridStructurePlacement;
import com.legacy.structure_gel.api.structure.StructureAccessHelper;
import com.mojang.datafixers.util.Pair;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({ChunkGenerator.class})
public class ChunkGeneratorMixin {
   @Inject(
      at = {@At("HEAD")},
      method = {"findNearestMapStructure"},
      cancellable = true
   )
   private void stopIfNotInDimension(
      ServerLevel serverLevel,
      HolderSet<Structure> structures,
      BlockPos origin,
      int radius,
      boolean skipKnownStructures,
      CallbackInfoReturnable<Pair<BlockPos, Holder<Structure>>> callback
   ) {
      ResourceKey<Level> levelKey = serverLevel.dimension();
      if (structures.stream()
         .<Optional>map(Holder::unwrapKey)
         .noneMatch(structure -> StructureAccessHelper.isValidDimension((Optional<? extends ResourceKey<? extends Structure>>)structure, levelKey))) {
         callback.setReturnValue(null);
      }
   }

   @Inject(
      at = {@At("RETURN")},
      method = {"findNearestMapStructure"},
      cancellable = true
   )
   private void locateGelPlacement(
      ServerLevel serverLevel,
      HolderSet<Structure> structures,
      BlockPos origin,
      int radius,
      boolean skipKnownStructures,
      CallbackInfoReturnable<Pair<BlockPos, Holder<Structure>>> callback
   ) {
      Pair<BlockPos, Holder<Structure>> newNearest = GridStructurePlacement.findNearesStructure(
         serverLevel, structures, origin, radius, skipKnownStructures, (ChunkGenerator)this, (Pair<BlockPos, Holder<Structure>>)callback.getReturnValue()
      );
      if (newNearest != null) {
         callback.setReturnValue(newNearest);
      }
   }
}
