package com.legacy.structure_gel.api.structure;

import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool.Projection;

/**
 * An interface to be implemented by instances of {@link StructurePiece} to
 * determine their color when using the "/structure_gel viewbounds" command.
 * 
 * @author Silver_David
 *
 */
public interface IColoredBoundingBox
{
	public static final int WHITE = toRgbInt(255, 255, 255);
	public static final int RED = toRgbInt(255, 0, 0);
	public static final int ORANGE = toRgbInt(255, 106, 0);
	public static final int YELLOW = toRgbInt(255, 216, 0);
	public static final int GREEN = toRgbInt(0, 255, 0);
	public static final int BLUE = toRgbInt(0, 0, 255);
	public static final int PURPLE = toRgbInt(110, 7, 255);

	int getBoundingBoxColor();

	public static int getBoundingBoxColor(StructurePiece piece)
	{
		if (piece instanceof IColoredBoundingBox colored)
			return colored.getBoundingBoxColor();
		return piece instanceof PoolElementStructurePiece poolPiece && poolPiece.getElement().getProjection() == Projection.TERRAIN_MATCHING ? BLUE : GREEN;
	}

	/**
	 * Converts the passed rgb values into an int. Color values are 0-255
	 * 
	 * @return The rgb int value
	 */
	public static int toRgbInt(int r, int g, int b)
	{
		int rgb = r;
		rgb = (rgb << 8) | g;
		return (rgb << 8) | b;
	}

	/**
	 * Converts the passed rgb values into an int. Color values are 0.0-1.0
	 * 
	 * @return The rgb int value
	 */
	public static int toRgbInt(float r, float g, float b)
	{
		return toRgbInt((int) (255 * r), (int) (255 * g), (int) (255 * b));
	}
}
