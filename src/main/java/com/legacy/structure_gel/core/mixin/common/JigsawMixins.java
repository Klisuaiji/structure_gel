package com.legacy.structure_gel.core.mixin.common;

import com.legacy.structure_gel.api.structure.ExtendedJigsawStructure;
import com.legacy.structure_gel.api.structure.jigsaw.ExtendedJigsawStructurePiece;
import com.legacy.structure_gel.api.structure.jigsaw.IPieceFactory;
import com.legacy.structure_gel.api.structure.jigsaw.JigsawCapability;
import com.legacy.structure_gel.core.capability.misc.GenerationContextData;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure.GenerationContext;
import net.minecraft.world.level.levelgen.structure.Structure.GenerationStub;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.pools.alias.PoolAliasLookup;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

public class JigsawMixins {
   private static final String ADD_PIECES_SIG = "addPieces(Lnet/minecraft/world/level/levelgen/structure/Structure$GenerationContext;Lnet/minecraft/core/Holder;Ljava/util/Optional;ILnet/minecraft/core/BlockPos;ZLjava/util/Optional;ILnet/minecraft/world/level/levelgen/structure/pools/alias/PoolAliasLookup;)Ljava/util/Optional;";

   @Mixin({JigsawPlacement.class})
   public static class JigsawPlacementMixin {
      @Inject(
         at = {@At("HEAD")},
         method = {"addPieces(Lnet/minecraft/world/level/levelgen/structure/Structure$GenerationContext;Lnet/minecraft/core/Holder;Ljava/util/Optional;ILnet/minecraft/core/BlockPos;ZLjava/util/Optional;ILnet/minecraft/world/level/levelgen/structure/pools/alias/PoolAliasLookup;)Ljava/util/Optional;"},
         cancellable = true
      )
      private static void addPieces_cancelOnCapabilityCheck(
         GenerationContext context,
         Holder<StructureTemplatePool> startPool,
         Optional<ResourceLocation> startJigsawName,
         int maxDepth,
         BlockPos pos,
         boolean useExpansionHack,
         Optional<Types> projectStartToHeightmap,
         int maxDistanceFromCenter,
         PoolAliasLookup poolAlias,
         CallbackInfoReturnable<Optional<GenerationStub>> callback
      ) {
         Optional<JigsawCapability> capability = ((GenerationContextData)context).structure_gel$getJigsawCapability();
         if (capability.isPresent()
            && !capability.get()
               .canPlace(
                  context,
                  pos,
                  new ExtendedJigsawStructure.PlaceContext(
                     startPool, startJigsawName, maxDepth, useExpansionHack, projectStartToHeightmap, maxDistanceFromCenter, capability
                  )
               )) {
            callback.setReturnValue(Optional.empty());
         }
      }

      @WrapOperation(
         at = {@At(
            value = "NEW",
            target = "net/minecraft/world/level/levelgen/structure/PoolElementStructurePiece"
         )},
         method = {"addPieces(Lnet/minecraft/world/level/levelgen/structure/Structure$GenerationContext;Lnet/minecraft/core/Holder;Ljava/util/Optional;ILnet/minecraft/core/BlockPos;ZLjava/util/Optional;ILnet/minecraft/world/level/levelgen/structure/pools/alias/PoolAliasLookup;)Ljava/util/Optional;"}
      )
      private static PoolElementStructurePiece addPieces_modifyStartPiece(
         StructureTemplateManager structureTemplateManager,
         StructurePoolElement poolElement,
         BlockPos pos,
         int groundLevelDelta,
         Rotation rotation,
         BoundingBox bounds,
         Operation<PoolElementStructurePiece> original,
         GenerationContext context
      ) {
         Optional<JigsawCapability> capability = ((GenerationContextData)context).structure_gel$getJigsawCapability();
         return capability.isPresent()
            ? capability.get()
               .getPieceFactory()
               .create(new IPieceFactory.Context(structureTemplateManager, poolElement, pos, groundLevelDelta, rotation, bounds, capability))
            : (PoolElementStructurePiece)original.call(new Object[]{structureTemplateManager, poolElement, pos, groundLevelDelta, rotation, bounds});
      }
   }

   @Mixin(
      targets = {"net/minecraft/world/level/levelgen/structure/pools/JigsawPlacement$Placer"}
   )
   public static class JigsawPlacerMixin {
      @WrapOperation(
         at = {@At(
            value = "NEW",
            target = "net/minecraft/world/level/levelgen/structure/PoolElementStructurePiece"
         )},
         method = {"tryPlacingChildren"}
      )
      private PoolElementStructurePiece tryPlacingChildren_modifyPiece(
         StructureTemplateManager structureTemplateManager,
         StructurePoolElement poolElement,
         BlockPos pos,
         int groundLevelDelta,
         Rotation rotation,
         BoundingBox bounds,
         Operation<PoolElementStructurePiece> original,
         PoolElementStructurePiece poolElementStructurePiece
      ) {
         if (poolElementStructurePiece instanceof ExtendedJigsawStructurePiece extendedPiece) {
            Optional<JigsawCapability> capability = extendedPiece.getCapability();
            if (capability.isPresent()) {
               return capability.get()
                  .getPieceFactory()
                  .create(new IPieceFactory.Context(structureTemplateManager, poolElement, pos, groundLevelDelta, rotation, bounds, capability));
            }
         }

         return (PoolElementStructurePiece)original.call(new Object[]{structureTemplateManager, poolElement, pos, groundLevelDelta, rotation, bounds});
      }
   }

   @Mixin({JigsawStructure.class})
   public static class JigsawStructureMixin {
      @Inject(
         at = {@At("HEAD")},
         method = {"findGenerationPoint"}
      )
      private void findGenerationPoint_addStructureToContext(GenerationContext context, CallbackInfoReturnable<Optional<GenerationStub>> callback) {
         ((GenerationContextData)context).structure_gel$setStructure((JigsawStructure)this);
      }
   }
}
