package com.legacy.structure_gel.core.asm_hooks;

import com.legacy.structure_gel.api.structure.StructureAccessHelper;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

public class LakeFeatureHooks {
   public static boolean checkForStructures(FeaturePlaceContext<?> context) {
      return !context.level()
         .getLevel()
         .structureManager()
         .startsForStructure(new ChunkPos(context.origin()), s -> StructureAccessHelper.isLakeProof(context.level().registryAccess(), s))
         .isEmpty();
   }
}
