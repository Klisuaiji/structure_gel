package com.legacy.structure_gel.api.structure.base;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure.GenerationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

public interface IPieceBuilderModifier {
   void modifyPieceBuilder(StructurePiecesBuilder var1, GenerationContext var2);

   static List<StructurePiece> getPieces(StructurePiecesBuilder builder) {
      return builder.pieces = new ArrayList(builder.pieces);
   }
}
