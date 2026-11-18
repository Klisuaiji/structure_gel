package com.legacy.structure_gel.api.structure;

import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool.Projection;

public interface IColoredBoundingBox {
   int WHITE = toRgbInt(255, 255, 255);
   int RED = toRgbInt(255, 0, 0);
   int ORANGE = toRgbInt(255, 106, 0);
   int YELLOW = toRgbInt(255, 216, 0);
   int GREEN = toRgbInt(0, 255, 0);
   int BLUE = toRgbInt(0, 0, 255);
   int PURPLE = toRgbInt(110, 7, 255);

   int getBoundingBoxColor();

   static int getBoundingBoxColor(StructurePiece piece) {
      if (piece instanceof IColoredBoundingBox colored) {
         return colored.getBoundingBoxColor();
      } else {
         return piece instanceof PoolElementStructurePiece poolPiece && poolPiece.getElement().getProjection() == Projection.TERRAIN_MATCHING ? BLUE : GREEN;
      }
   }

   static int toRgbInt(int r, int g, int b) {
      int rgb = r << 8 | g;
      return rgb << 8 | b;
   }

   static int toRgbInt(float r, float g, float b) {
      return toRgbInt((int)(255.0F * r), (int)(255.0F * g), (int)(255.0F * b));
   }
}
