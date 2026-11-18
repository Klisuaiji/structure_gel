package com.legacy.structure_gel.api.structure;

import com.legacy.structure_gel.api.data_handler.handlers.DataHandler;
import com.legacy.structure_gel.api.structure.base.IModifyState;
import com.legacy.structure_gel.api.structure.processor.RemoveGelStructureProcessor;
import com.legacy.structure_gel.core.block.DataHandlerBlock;
import com.legacy.structure_gel.core.registry.SGRegistry;
import com.legacy.structure_gel.core.util.Internal;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class GelTemplateStructurePiece extends TemplateStructurePiece implements IModifyState {
   @Deprecated
   private static final Logger LOGGER = LogManager.getLogger();
   public Rotation rotation = Rotation.NONE;
   public Mirror mirror = Mirror.NONE;

   public GelTemplateStructurePiece(
      StructurePieceType structurePieceType,
      int componentType,
      StructureTemplateManager structureManager,
      ResourceLocation templateName,
      StructurePlaceSettings placeSettings,
      BlockPos pos
   ) {
      super(structurePieceType, componentType, structureManager, templateName, templateName.toString(), placeSettings, pos);
   }

   public GelTemplateStructurePiece(
      StructurePieceType structurePieceType, int componentType, StructureTemplateManager structureManager, ResourceLocation templateName, BlockPos pos
   ) {
      this(structurePieceType, componentType, structureManager, templateName, new StructurePlaceSettings(), pos);
   }

   public GelTemplateStructurePiece(
      StructurePieceType structurePieceType,
      CompoundTag nbt,
      StructureTemplateManager structureManager,
      Function<ResourceLocation, StructurePlaceSettings> placeSettingsGetter
   ) {
      super(structurePieceType, nbt, structureManager, placeSettingsGetter);
      this.rotation = getRotation(nbt);
      this.mirror = getMirror(nbt);
   }

   public GelTemplateStructurePiece(StructurePieceType structurePieceType, CompoundTag nbt, StructureTemplateManager structureManager) {
      this(structurePieceType, nbt, structureManager, name -> new StructurePlaceSettings());
   }

   protected StructurePlaceSettings getPlaceSettings(StructureTemplateManager structureManager) {
      return new StructurePlaceSettings();
   }

   protected void setupPlaceSettings(StructureTemplateManager structureManager) {
      StructurePlaceSettings placeSettings = this.getPlaceSettings(structureManager).setRotation(this.rotation).setMirror(this.mirror);
      placeSettings.setRotation(this.rotation);
      placeSettings.setMirror(this.mirror);
      if (this.useGelProcessor() && !placeSettings.getProcessors().contains(RemoveGelStructureProcessor.INSTANCE)) {
         placeSettings.addProcessor(RemoveGelStructureProcessor.INSTANCE);
      }

      this.placeSettings = placeSettings;
      this.boundingBox = this.template.getBoundingBox(this.placeSettings, this.templatePosition);
   }

   protected boolean useGelProcessor() {
      return true;
   }

   public static BlockPos getPos(CompoundTag nbt) {
      return new BlockPos(nbt.getInt("TPX"), nbt.getInt("TPY"), nbt.getInt("TPZ"));
   }

   public static Rotation getRotation(CompoundTag nbt) {
      return Rotation.valueOf(nbt.getString("Rot"));
   }

   public static Mirror getMirror(CompoundTag nbt) {
      return Mirror.valueOf(nbt.getString("Mirror"));
   }

   protected void addAdditionalSaveData(StructurePieceSerializationContext level, CompoundTag nbt) {
      super.addAdditionalSaveData(level, nbt);
      nbt.putString("Rot", this.rotation.name());
      nbt.putString("Mirror", this.mirror.name());
   }

   @Internal
   public void postProcess(
      WorldGenLevel level, StructureManager structureManager, ChunkGenerator chunkGen, RandomSource rand, BoundingBox bounds, ChunkPos chunkPos, BlockPos pos
   ) {
      super.postProcess(level, structureManager, chunkGen, rand, bounds, chunkPos, pos);
   }

   public static void processDataHandlers(
      TemplateStructurePiece piece,
      StructureTemplate template,
      BlockPos templatePos,
      StructurePlaceSettings placeSettings,
      WorldGenLevel level,
      RandomSource rand,
      BoundingBox bounds
   ) {
      DataHandlerBlock dataHandler = SGRegistry.Blocks.DATA_HANDLER.get();

      for (StructureBlockInfo blockInfo : template.filterBlocks(templatePos, placeSettings, dataHandler)) {
         if (blockInfo.nbt() != null) {
            BlockState state = level.getBlockState(blockInfo.pos());
            if (state.is(dataHandler)) {
               DataHandler.process(state, blockInfo.nbt(), blockInfo.pos(), level, rand, bounds, piece, false);
            }
         }
      }
   }

   @Nullable
   @Override
   public BlockState modifyState(ServerLevelAccessor level, RandomSource rand, BlockPos pos, BlockState originalState) {
      return originalState;
   }
}
