package com.legacy.structure_gel.api.structure.base;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

/**
 * Used by structures to allow for modifying the {@link StructurePiecesBuilder}
 * before generation. Includes a helper method to obtain the piece list from the
 * builder, allowing for removing pieces if need be.
 * 
 * @author Silver_David
 */
public interface IPieceBuilderModifier
{
	/**
	 * Modifies the {@link StructurePiecesBuilder} passed
	 * 
	 * @param pieceBuilder
	 * @param context
	 * @see IPieceBuilderModifier#getPieces(StructurePiecesBuilder)
	 */
	void modifyPieceBuilder(StructurePiecesBuilder pieceBuilder, Structure.GenerationContext context);

	/**
	 * Obtains the Structure Piece list from the builder, ensuring that said list is
	 * mutable in the process
	 * 
	 * @param builder
	 * @return The builder's piece list
	 */
	public static List<StructurePiece> getPieces(StructurePiecesBuilder builder)
	{
		return builder.pieces = new ArrayList<>(builder.pieces);
	}
}
