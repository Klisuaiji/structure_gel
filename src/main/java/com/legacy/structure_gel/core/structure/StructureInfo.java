package com.legacy.structure_gel.core.structure;

import java.util.List;
import java.util.stream.Collectors;

import com.legacy.structure_gel.api.structure.IColoredBoundingBox;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;

/**
 * Stores info obtained from a Structure Start to be used with
 * StructureBoundsRenderer
 * 
 * @author Silver_David
 *
 */
public record StructureInfo(ResourceLocation dimension, BoundingBox fullBounds, List<StructureInfo.PieceInfo> pieces)
{

	public StructureInfo(ResourceLocation dimension, StructureStart start)
	{
		this(dimension, start.getBoundingBox(), PieceInfo.toPieceInfos(start.getPieces()));
	}

	public static record PieceInfo(BoundingBox bounds, int color)
	{
		private static List<PieceInfo> toPieceInfos(List<StructurePiece> pieces)
		{
			return pieces.stream().map(piece ->
			{
				BoundingBox bounds = piece.getBoundingBox();
				int color = IColoredBoundingBox.getBoundingBoxColor(piece);
				return new PieceInfo(bounds, color);
			}).collect(Collectors.toList());
		}

		public float[] getColorFloats()
		{
			int c = this.color;
			float r = (c >> 16 & 255) / 255.0F;
			float g = (c >> 8 & 255) / 255.0F;
			float b = (c & 255) / 255.0F;
			return new float[] { r, g, b };
		}
	}
}