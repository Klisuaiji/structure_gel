package com.legacy.structure_gel.api.structure.processor;

import com.legacy.structure_gel.api.block.base.IStructureGel;
import com.legacy.structure_gel.core.registry.SGRegistry;
import com.mojang.serialization.Codec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;

public final class RemoveGelStructureProcessor extends StructureProcessor {
   public static final RemoveGelStructureProcessor INSTANCE = new RemoveGelStructureProcessor();
   public static final Codec<RemoveGelStructureProcessor> CODEC = Codec.unit(() -> INSTANCE);

   private RemoveGelStructureProcessor() {
   }

   @Nullable
   public StructureBlockInfo processBlock(
      LevelReader level, BlockPos pos, BlockPos pos2, StructureBlockInfo existing, StructureBlockInfo placed, StructurePlaceSettings settings
   ) {
      if (placed.state().getBlock() instanceof IStructureGel gel) {
         return new StructureBlockInfo(placed.pos(), gel.getReplacementState(), null);
      } else {
         return placed.state().getBlock() == Blocks.AIR ? null : placed;
      }
   }

   protected StructureProcessorType<?> getType() {
      return SGRegistry.Processors.REMOVE_FILLER.get();
   }
}
