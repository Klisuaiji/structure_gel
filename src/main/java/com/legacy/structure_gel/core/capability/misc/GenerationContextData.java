package com.legacy.structure_gel.core.capability.misc;

import com.legacy.structure_gel.api.structure.ExtendedJigsawStructure;
import com.legacy.structure_gel.api.structure.jigsaw.JigsawCapability;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.world.level.levelgen.structure.Structure;

public interface GenerationContextData {
   @Nullable
   Structure structure_gel$getStructure();

   void structure_gel$setStructure(@Nullable Structure var1);

   default Optional<JigsawCapability> structure_gel$getJigsawCapability() {
      return this.structure_gel$getStructure() instanceof ExtendedJigsawStructure extended ? extended.getCapability() : Optional.empty();
   }
}
