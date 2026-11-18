package com.legacy.structure_gel.core.mixin.common;

import com.legacy.structure_gel.core.capability.misc.GenerationContextData;
import javax.annotation.Nullable;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.Structure.GenerationContext;
import org.spongepowered.asm.mixin.Mixin;

@Mixin({GenerationContext.class})
public class GenerationContextMixin implements GenerationContextData {
   @Nullable
   private Structure structure_gel$structure;

   @Nullable
   @Override
   public Structure structure_gel$getStructure() {
      return this.structure_gel$structure;
   }

   @Override
   public void structure_gel$setStructure(@Nullable Structure structure) {
      this.structure_gel$structure = structure;
   }
}
