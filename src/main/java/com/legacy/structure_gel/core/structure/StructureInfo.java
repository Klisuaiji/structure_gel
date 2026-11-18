package com.legacy.structure_gel.core.structure;

import com.legacy.structure_gel.api.structure.IColoredBoundingBox;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;

public record StructureInfo(ResourceLocation dimension, BoundingBox fullBounds, List<StructureInfo.PieceInfo> pieces) {
   public StructureInfo(ResourceLocation dimension, StructureStart start) {
      this(dimension, start.getBoundingBox(), StructureInfo.PieceInfo.toPieceInfos(start.getPieces()));
   }

   public record PieceInfo(BoundingBox bounds, int color) {
      private static List<StructureInfo.PieceInfo> toPieceInfos(List<StructurePiece> pieces) {
         return pieces.stream().map(piece -> {
            BoundingBox bounds = piece.getBoundingBox();
            int color = IColoredBoundingBox.getBoundingBoxColor(piece);
            return new StructureInfo.PieceInfo(bounds, color);
         }).collect(Collectors.toList());
      }

      public float[] getColorFloats() {
         int c = this.color;
         float r = (c >> 16 & 0xFF) / 255.0F;
         float g = (c >> 8 & 0xFF) / 255.0F;
         float b = (c & 0xFF) / 255.0F;
         return new float[]{r, g, b};
      }
   }
}
