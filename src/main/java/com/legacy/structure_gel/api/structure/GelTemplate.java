package com.legacy.structure_gel.api.structure;

import com.legacy.structure_gel.api.structure.base.IModifyState;
import com.legacy.structure_gel.core.capability.misc.StructureSettingsData;
import com.legacy.structure_gel.core.util.Internal;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

@Internal
public class GelTemplate {
   public static boolean placeInWorld(
      StructureTemplate template,
      ServerLevelAccessor levelIn,
      BlockPos pos1,
      BlockPos pos2,
      StructurePlaceSettings placeSettings,
      RandomSource rand,
      int placeFlags,
      IModifyState stateModifier
   ) {
      StructureSettingsData placeData = (StructureSettingsData)placeSettings;
      if (placeData.structure_gel$isNatural()) {
         placeData.structure_gel$setStateModifier(stateModifier);
      }

      return template.placeInWorld(levelIn, pos1, pos2, placeSettings, rand, placeFlags);
   }
}
