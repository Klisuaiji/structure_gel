package com.legacy.structure_gel.api.structure.jigsaw;

import com.legacy.structure_gel.api.registry.StructureGelRegistries;
import com.legacy.structure_gel.api.structure.ExtendedJigsawStructure;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.Structure.GenerationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

public interface JigsawCapability {
   Codec<JigsawCapability> CODEC = StructureGelRegistries.JIGSAW_TYPE.byNameCodec().dispatch(JigsawCapability::getType, JigsawCapabilityType::codec);

   JigsawCapabilityType<?> getType();

   default boolean canPlace(GenerationContext generationContext, BlockPos placementPos, ExtendedJigsawStructure.PlaceContext placeContext) {
      return true;
   }

   default IPieceFactory getPieceFactory() {
      return IPieceFactory.VANILLA;
   }

   default void modifyPieceBuilder(StructurePiecesBuilder pieceBuilder, GenerationContext context) {
   }

   final class Vanilla implements JigsawCapability {
      public static final JigsawCapability.Vanilla INSTANCE = new JigsawCapability.Vanilla();
      public static final Codec<JigsawCapability.Vanilla> CODEC = Codec.unit(INSTANCE);

      @Override
      public JigsawCapabilityType<?> getType() {
         return null;
      }

      @Override
      public String toString() {
         return "Vanilla Capability";
      }
   }
}
