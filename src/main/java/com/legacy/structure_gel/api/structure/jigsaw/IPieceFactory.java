package com.legacy.structure_gel.api.structure.jigsaw;

import com.legacy.structure_gel.core.structure.jigsaw.VanillaJigsawStructurePiece;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

public interface IPieceFactory {
   IPieceFactory VANILLA = VanillaJigsawStructurePiece::new;

   PoolElementStructurePiece create(IPieceFactory.Context var1);

   record Context(
      StructureTemplateManager structureManager,
      StructurePoolElement poolElement,
      BlockPos pos,
      int groundLevelDelta,
      Rotation rotation,
      BoundingBox bounds,
      Optional<JigsawCapability> jigsawCapability
   ) {
      public <T extends JigsawCapability> Optional<T> jigsawCapability(Class<T> expectedClass) {
         return this.jigsawCapability().map(cap -> expectedClass.isInstance(cap) ? expectedClass.cast(cap) : null);
      }
   }
}
