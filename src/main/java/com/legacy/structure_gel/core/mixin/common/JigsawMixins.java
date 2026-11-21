package com.legacy.structure_gel.core.mixin.common;

import java.util.Optional;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.legacy.structure_gel.api.structure.ExtendedJigsawStructure;
import com.legacy.structure_gel.api.structure.jigsaw.ExtendedJigsawStructurePiece;
import com.legacy.structure_gel.api.structure.jigsaw.IPieceFactory;
import com.legacy.structure_gel.api.structure.jigsaw.JigsawCapability;
import com.legacy.structure_gel.core.capability.misc.GenerationContextData;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.pools.DimensionPadding;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.pools.alias.PoolAliasLookup;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

public class JigsawMixins
{
	private static final String ADD_PIECES_SIG = "addPieces(Lnet/minecraft/world/level/levelgen/structure/Structure$GenerationContext;Lnet/minecraft/core/Holder;Ljava/util/Optional;ILnet/minecraft/core/BlockPos;ZLjava/util/Optional;ILnet/minecraft/world/level/levelgen/structure/pools/alias/PoolAliasLookup;Lnet/minecraft/world/level/levelgen/structure/pools/DimensionPadding;Lnet/minecraft/world/level/levelgen/structure/templatesystem/LiquidSettings;)Ljava/util/Optional;";

	@Mixin(JigsawStructure.class)
	public static class JigsawStructureMixin
	{
		//@formatter:off
		@Inject(
				at = @At("HEAD"), 
				method = "findGenerationPoint")
		//@formatter:on
		private void findGenerationPoint_addStructureToContext(Structure.GenerationContext context, CallbackInfoReturnable<Optional<Structure.GenerationStub>> callback)
		{
			// Add the structure to the generation context. Jigsaws use this to reference capability in their placement.
			((GenerationContextData) (Object) context).structure_gel$setStructure((JigsawStructure) (Object) this);

			//System.out.println("Added structure to context " + ((GenerationContextData) (Object) context).structure_gel$getStructure().getClass());
		}
	}

	@Mixin(JigsawPlacement.class)
	public static class JigsawPlacementMixin
	{
		//@formatter:off
		@Inject(
				at = @At("HEAD"), 
				method = JigsawMixins.ADD_PIECES_SIG,
				cancellable = true)
		//@formatter:on
		private static void addPieces_cancelOnCapabilityCheck(Structure.GenerationContext context, Holder<StructureTemplatePool> startPool, Optional<ResourceLocation> startJigsawName, int maxDepth, BlockPos pos, boolean useExpansionHack, Optional<Heightmap.Types> projectStartToHeightmap, int maxDistanceFromCenter, PoolAliasLookup poolAlias, DimensionPadding dimensionPadding, LiquidSettings liquidSettings, CallbackInfoReturnable<Optional<Structure.GenerationStub>> callback)
		{
			// ExtendedJigsawStructures should check the capability before placing. If it fails, we return an empty optional
			Optional<JigsawCapability> capability = ((GenerationContextData) (Object) context).structure_gel$getJigsawCapability();
			if (capability.isPresent() && !capability.get().canPlace(context, pos, new ExtendedJigsawStructure.PlaceContext(startPool, startJigsawName, maxDepth, useExpansionHack, projectStartToHeightmap, maxDistanceFromCenter, dimensionPadding, liquidSettings, capability)))
			{
				callback.setReturnValue(Optional.empty());
			}
		}

		//@formatter:off
		@WrapOperation(
				at = @At(
						value = "NEW", 
						target = "net/minecraft/world/level/levelgen/structure/PoolElementStructurePiece"),
				method = ADD_PIECES_SIG
				)
		
		private static PoolElementStructurePiece addPieces_modifyStartPiece(
				// WrapOperation args
				StructureTemplateManager structureTemplateManager, StructurePoolElement poolElement, BlockPos pos, int groundLevelDelta, Rotation rotation, BoundingBox bounds, LiquidSettings liquidSettings, Operation<PoolElementStructurePiece> original, 
				// Method args
				Structure.GenerationContext context)
		//@formatter:on
		{
			// If the structure has JigsawCapability, use that to create the PoolElementStructurePiece
			Optional<JigsawCapability> capability = ((GenerationContextData) (Object) context).structure_gel$getJigsawCapability();
			if (capability.isPresent())
			{
				return capability.get().getPieceFactory().create(new IPieceFactory.Context(structureTemplateManager, poolElement, pos, groundLevelDelta, rotation, bounds, liquidSettings, capability));
			}
			return original.call(structureTemplateManager, poolElement, pos, groundLevelDelta, rotation, bounds, liquidSettings);
		}
	}

	@Mixin(targets = "net/minecraft/world/level/levelgen/structure/pools/JigsawPlacement$Placer" /*JigsawPlacement.Placer.class*/)
	public static class JigsawPlacerMixin
	{
		//@formatter:off
		@WrapOperation(
				at = @At(
						value = "NEW", 
						target = "net/minecraft/world/level/levelgen/structure/PoolElementStructurePiece"),
				method = "tryPlacingChildren"
				)
		
		private PoolElementStructurePiece tryPlacingChildren_modifyPiece(
				// WrapOperation args
				StructureTemplateManager structureTemplateManager, StructurePoolElement poolElement, BlockPos pos, int groundLevelDelta, Rotation rotation, BoundingBox bounds, LiquidSettings liquidSettings, Operation<PoolElementStructurePiece> original, 
				// Method args
				PoolElementStructurePiece poolElementStructurePiece
				)
		//@formatter:on
		{
			// If the structure has JigsawCapability, use that to create the PoolElementStructurePiece
			if (poolElementStructurePiece instanceof ExtendedJigsawStructurePiece extendedPiece)
			{
				Optional<JigsawCapability> capability = extendedPiece.getCapability();
				if (capability.isPresent())
				{
					return capability.get().getPieceFactory().create(new IPieceFactory.Context(structureTemplateManager, poolElement, pos, groundLevelDelta, rotation, bounds, liquidSettings, capability));
				}
			}
			return original.call(structureTemplateManager, poolElement, pos, groundLevelDelta, rotation, bounds, liquidSettings);
		}
	}
}
