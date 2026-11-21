package com.legacy.structure_gel.api.structure.jigsaw;

import com.legacy.structure_gel.api.registry.StructureGelRegistries;
import com.legacy.structure_gel.api.structure.ExtendedJigsawStructure;
import com.legacy.structure_gel.api.structure.base.IPieceBuilderModifier;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.Structure.GenerationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

/**
 * This class is used to store additional data within a Jigsaw structure by
 * using {@link ExtendedJigsawStructure}.
 * <p>
 * To create a custom capability, create a class that extends this and can be
 * represented with a {@link Codec}. Then register a supplier to the codec as
 * the {@link JigsawCapabilityType}.
 * 
 * @see JigsawCapability.Vanilla
 * 
 * @author Silver_David
 *
 */
public interface JigsawCapability
{
	public static final Codec<JigsawCapability> CODEC = StructureGelRegistries.JIGSAW_TYPE.byNameCodec().dispatch(JigsawCapability::getType, JigsawCapabilityType::codec);

	/**
	 * @return The {@link JigsawCapabilityType} that represents this.
	 */
	JigsawCapabilityType<?> getType();

	/**
	 * @return True if the jigsaw structure should be allowed to place. False
	 *         otherwise.
	 */
	default boolean canPlace(Structure.GenerationContext generationContext, BlockPos placementPos, ExtendedJigsawStructure.PlaceContext placeContext)
	{
		return true;
	}

	/**
	 * @return A constructor for your structure piece.
	 */
	default IPieceFactory getPieceFactory()
	{
		return IPieceFactory.VANILLA;
	}

	/**
	 * Modifies the piece builder passed in
	 * 
	 * @see IPieceBuilderModifier
	 */
	default void modifyPieceBuilder(StructurePiecesBuilder pieceBuilder, GenerationContext context)
	{

	}

	/**
	 * This is the default implementation of {@link JigsawCapability}.
	 * 
	 * @author Silver_David
	 *
	 */
	public static final class Vanilla implements JigsawCapability
	{
		public static final Vanilla INSTANCE = new Vanilla();
		public static final MapCodec<Vanilla> CODEC = MapCodec.unit(INSTANCE);

		@Override
		public JigsawCapabilityType<?> getType()
		{
			return null;
		}

		@Override
		public String toString()
		{
			return "Vanilla Capability";
		}
	}
}
