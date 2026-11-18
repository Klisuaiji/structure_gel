package com.legacy.structure_gel.core.structure.jigsaw;

import com.legacy.structure_gel.api.data_handler.handlers.DataHandler;
import com.legacy.structure_gel.api.structure.GelTemplate;
import com.legacy.structure_gel.api.structure.jigsaw.ExtendedJigsawStructurePiece;
import com.legacy.structure_gel.api.structure.processor.RemoveGelStructureProcessor;
import com.legacy.structure_gel.core.block.DataHandlerBlock;
import com.legacy.structure_gel.core.registry.SGRegistry;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.StructureMode;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElementType;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool.Projection;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;

public class GelSinglePoolElement extends SinglePoolElement {
   public static final Codec<Either<ResourceLocation, StructureTemplate>> POOL_CODEC = Codec.of(
      GelSinglePoolElement::encodePool, ResourceLocation.CODEC.map(Either::left)
   );
   public static final Codec<GelSinglePoolElement> CODEC = RecordCodecBuilder.create(
      instance -> instance.group(
            encodeTemplate(),
            encodeProcessor(),
            projectionCodec(),
            Codec.BOOL.optionalFieldOf("maintainWater", true).forGetter(element -> element.keepLiquids),
            Codec.BOOL.optionalFieldOf("ignoreEntities", false).forGetter(element -> element.ignoreEntities)
         )
         .apply(instance, GelSinglePoolElement::new)
   );
   private boolean keepLiquids = true;
   private boolean ignoreEntities = false;

   private static <T> DataResult<T> encodePool(Either<ResourceLocation, StructureTemplate> locationTemplate, DynamicOps<T> dyn, T data) {
      Optional<ResourceLocation> optional = locationTemplate.left();
      return !optional.isPresent()
         ? DataResult.error(() -> "Can not serialize a runtime pool element")
         : ResourceLocation.CODEC.encode(optional.get(), dyn, data);
   }

   protected static <E extends GelSinglePoolElement> RecordCodecBuilder<E, Holder<StructureProcessorList>> encodeProcessor() {
      return StructureProcessorType.LIST_CODEC.fieldOf("processors").forGetter(piece -> piece.processors);
   }

   protected static <E extends GelSinglePoolElement> RecordCodecBuilder<E, Either<ResourceLocation, StructureTemplate>> encodeTemplate() {
      return POOL_CODEC.fieldOf("location").forGetter(piece -> piece.template);
   }

   public GelSinglePoolElement(
      Either<ResourceLocation, StructureTemplate> location,
      Holder<StructureProcessorList> processors,
      Projection projection,
      boolean keepLiquids,
      boolean ignoreEntities
   ) {
      super(location, processors, projection);
      this.keepLiquids = keepLiquids;
      this.ignoreEntities = ignoreEntities;
   }

   @Deprecated
   public GelSinglePoolElement maintainWater(boolean keepLiquids) {
      return this.setKeepLiquids(keepLiquids);
   }

   public GelSinglePoolElement setKeepLiquids(boolean keepLiquids) {
      this.keepLiquids = keepLiquids;
      return this;
   }

   public GelSinglePoolElement setIgnoreEntities(boolean ignoreEntities) {
      this.ignoreEntities = ignoreEntities;
      return this;
   }

   protected StructurePlaceSettings getSettings(Rotation rotation, BoundingBox boundingBox, boolean isLegacy) {
      StructurePlaceSettings settings = super.getSettings(rotation, boundingBox, isLegacy);
      settings.setKeepLiquids(this.keepLiquids);
      settings.setIgnoreEntities(this.ignoreEntities);
      settings.getProcessors().remove(BlockIgnoreProcessor.STRUCTURE_BLOCK);
      settings.getProcessors().add(0, RemoveGelStructureProcessor.INSTANCE);
      return settings;
   }

   public boolean place(
      StructureTemplateManager templateManager,
      WorldGenLevel level,
      StructureManager structureManager,
      ChunkGenerator chunkGen,
      BlockPos templatePos,
      BlockPos jigsawPos,
      Rotation rotation,
      BoundingBox bounds,
      RandomSource rand,
      boolean isLegacy
   ) {
      return this instanceof SinglePoolElementExtension singlePool
            && singlePool.structure_gel$getPiece() instanceof ExtendedJigsawStructurePiece extendedJigsawPiece
         ? this.place(templateManager, level, structureManager, chunkGen, templatePos, jigsawPos, rotation, bounds, rand, isLegacy, extendedJigsawPiece)
         : super.place(templateManager, level, structureManager, chunkGen, templatePos, jigsawPos, rotation, bounds, rand, isLegacy);
   }

   public boolean place(
      StructureTemplateManager templateManager,
      WorldGenLevel level,
      StructureManager structureManager,
      ChunkGenerator chunkGen,
      BlockPos templatePos,
      BlockPos jigsawPos,
      Rotation rotation,
      BoundingBox bounds,
      RandomSource rand,
      boolean isLegacy,
      ExtendedJigsawStructurePiece extendedJigsawPiece
   ) {
      StructureTemplate template = this.getTemplate(templateManager);
      StructurePlaceSettings placementSettings = this.getSettings(rotation, bounds, isLegacy);
      if (!GelTemplate.placeInWorld(template, level, templatePos, jigsawPos, placementSettings, rand, 18, extendedJigsawPiece::modifyState)) {
         return false;
      } else {
         processDataHandlers(extendedJigsawPiece, template, templatePos, jigsawPos, placementSettings, level, rotation, bounds, rand);

         for (StructureBlockInfo blockInfo : StructureTemplate.processBlockInfos(
            level, templatePos, jigsawPos, placementSettings, this.getDataMarkers(templateManager, templatePos, rotation, false), template
         )) {
            this.handleDataMarker(level, blockInfo, templatePos, rotation, rand, bounds);
            if (blockInfo.nbt() != null
               && level.getBlockState(blockInfo.pos()).is(Blocks.STRUCTURE_BLOCK)
               && StructureMode.valueOf(blockInfo.nbt().getString("mode")) == StructureMode.DATA) {
               extendedJigsawPiece.handleDataMarker(blockInfo.nbt().getString("metadata"), blockInfo.pos(), level, rand, bounds);
            }
         }

         return true;
      }
   }

   public static void processDataHandlers(
      StructurePiece piece,
      StructureTemplate template,
      BlockPos templatePos,
      BlockPos jigsawPos,
      StructurePlaceSettings placeSettings,
      WorldGenLevel level,
      Rotation rotation,
      BoundingBox bounds,
      RandomSource rand
   ) {
      DataHandlerBlock dataHandler = SGRegistry.Blocks.DATA_HANDLER.get();

      for (StructureBlockInfo blockInfo : StructureTemplate.processBlockInfos(
         level,
         templatePos,
         jigsawPos,
         placeSettings,
         template.filterBlocks(templatePos, new StructurePlaceSettings().setRotation(rotation), dataHandler, false),
         template
      )) {
         if (blockInfo.nbt() != null) {
            BlockState state = level.getBlockState(blockInfo.pos());
            if (state.is(dataHandler)) {
               DataHandler.process(state, blockInfo.nbt(), blockInfo.pos(), level, rand, bounds, piece, false);
            }
         }
      }
   }

   public ResourceLocation getLocation() {
      return (ResourceLocation)this.template.left().get();
   }

   private StructureTemplate getTemplate(StructureTemplateManager templateManager) {
      return (StructureTemplate)this.template.map(templateManager::getOrCreate, Function.identity());
   }

   public StructurePoolElementType<?> getType() {
      return SGRegistry.JigsawDeserializers.GEL_SINGLE_POOL_ELEMENT.get();
   }

   public String toString() {
      return "Gel[" + this.template + "]";
   }
}
