package com.legacy.structure_gel.core.structure.jigsaw;

import javax.annotation.Nullable;
import net.minecraft.world.level.levelgen.structure.StructurePiece;

public interface SinglePoolElementExtension {
   void structure_gel$setPiece(@Nullable StructurePiece var1);

   @Nullable
   StructurePiece structure_gel$getPiece();
}
