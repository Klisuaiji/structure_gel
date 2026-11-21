package com.legacy.structure_gel.core.registry;

import com.legacy.structure_gel.api.block.AxisStructureGelBlock;
import com.legacy.structure_gel.api.block.StructureGelBlock;
import com.legacy.structure_gel.api.block.base.IStructureGel;
import com.legacy.structure_gel.api.data.tags.FilterHolderSet;
import com.legacy.structure_gel.api.data_handler.DataHandlerType;
import com.legacy.structure_gel.api.dynamic_spawner.DynamicSpawnerType;
import com.legacy.structure_gel.api.item.StructureGelItem;
import com.legacy.structure_gel.api.registry.RegistrarHolder;
import com.legacy.structure_gel.api.registry.StructureGelRegistries;
import com.legacy.structure_gel.api.registry.registrar.Registrar;
import com.legacy.structure_gel.api.registry.registrar.RegistrarHandler;
import com.legacy.structure_gel.api.structure.ExtendedJigsawStructure;
import com.legacy.structure_gel.api.structure.GridStructurePlacement;
import com.legacy.structure_gel.api.structure.jigsaw.JigsawCapability;
import com.legacy.structure_gel.api.structure.jigsaw.JigsawCapabilityType;
import com.legacy.structure_gel.api.structure.processor.RandomBlockSwapProcessor;
import com.legacy.structure_gel.api.structure.processor.RandomStateSwapProcessor;
import com.legacy.structure_gel.api.structure.processor.RandomTagSwapProcessor;
import com.legacy.structure_gel.api.structure.processor.RemoveGelStructureProcessor;
import com.legacy.structure_gel.core.block.DataHandlerBlock;
import com.legacy.structure_gel.core.block.DynamicSpawnerBlock;
import com.legacy.structure_gel.core.block_entity.DataHandlerBlockEntity;
import com.legacy.structure_gel.core.block_entity.DynamicSpawnerBlockEntity;
import com.legacy.structure_gel.core.capability.entity.GelEntity;
import com.legacy.structure_gel.core.data_handler.handlers.ArchaeologyHandler;
import com.legacy.structure_gel.core.data_handler.handlers.BlockStateHandler;
import com.legacy.structure_gel.core.data_handler.handlers.ChestHandler;
import com.legacy.structure_gel.core.data_handler.handlers.ConfiguredFeatureHandler;
import com.legacy.structure_gel.core.data_handler.handlers.DynamicSpawnerHandler;
import com.legacy.structure_gel.core.data_handler.handlers.EmptyDataHandler;
import com.legacy.structure_gel.core.data_handler.handlers.EntityHandler;
import com.legacy.structure_gel.core.data_handler.handlers.VanillaSpawnerHandler;
import com.legacy.structure_gel.core.item.BlockPaletteItem;
import com.legacy.structure_gel.core.item.building_tool.BuildingToolItem;
import com.legacy.structure_gel.core.structure.jigsaw.GelSinglePoolElement;
import com.legacy.structure_gel.core.structure.jigsaw.VanillaJigsawStructurePiece;
import com.legacy.structure_gel.core.structure.modifiers.StructureConfigModifier;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.GameMasterBlockItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityType.Builder;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElementType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.world.StructureModifier;
import net.neoforged.neoforge.registries.NeoForgeRegistries.Keys;
import net.neoforged.neoforge.registries.holdersets.HolderSetType;

public class SGRegistry {
   @RegistrarHolder
   public static class AttachmentTypes {
      public static final RegistrarHandler<AttachmentType<?>> HANDLER = RegistrarHandler.getOrCreate(Keys.ATTACHMENT_TYPES, "structure_gel");
      public static final Registrar.Static<AttachmentType<GelEntity>> GEL_ENTITY = HANDLER.createStatic(
         "gel_entity", () -> AttachmentType.builder(GelEntity::new).serialize(GelEntity.Serializer.INSTANCE).copyOnDeath().build()
      );
   }

   @RegistrarHolder
   public static class BlockEntities {
      public static final RegistrarHandler<BlockEntityType<?>> HANDLER = RegistrarHandler.getOrCreate(Registries.BLOCK_ENTITY_TYPE, "structure_gel");
      public static final Registrar.Static<BlockEntityType<DataHandlerBlockEntity>> DATA_HANDLER = HANDLER.createStatic(
         "data_handler", () -> Builder.<DataHandlerBlockEntity>of(DataHandlerBlockEntity::new, new Block[]{SGRegistry.Blocks.DATA_HANDLER.get()}).build(null)
      );
      public static final Registrar.Static<BlockEntityType<DynamicSpawnerBlockEntity>> DYNAMIC_SPAWNER = HANDLER.createStatic(
         "dynamic_spawner", () -> Builder.<DynamicSpawnerBlockEntity>of(DynamicSpawnerBlockEntity::new, new Block[]{(Block)SGRegistry.Blocks.DYNAMIC_SPAWNER.get()}).build(null)
      );
   }

   @RegistrarHolder
   public static class BlockTypes {
      public static final RegistrarHandler<MapCodec<? extends Block>> HANDLER = RegistrarHandler.getOrCreate(Registries.BLOCK_TYPE, "structure_gel");
      public static final Registrar.Static<MapCodec<StructureGelBlock>> GEL = HANDLER.createStatic("gel", () -> StructureGelBlock.CODEC);
      public static final Registrar.Static<MapCodec<AxisStructureGelBlock>> AXIS_GEL = HANDLER.createStatic("axis_gel", () -> AxisStructureGelBlock.CODEC);
      public static final Registrar.Static<MapCodec<DataHandlerBlock>> DATA_HANDLER = HANDLER.createStatic("data_handler", () -> DataHandlerBlock.CODEC);
      public static final Registrar.Static<MapCodec<DynamicSpawnerBlock>> DYNAMIC_SPAWNER = HANDLER.createStatic(
         "dynamic_spawner", () -> DynamicSpawnerBlock.CODEC
      );
   }

   @RegistrarHolder
   public static class Blocks {
      public static final RegistrarHandler.BlockHandler HANDLER = RegistrarHandler.getOrCreateBlocks("structure_gel");
      public static final Registrar.BlockRef<StructureGelBlock> RED_GEL = HANDLER.createStatic(
         "red_gel", () -> new StructureGelBlock(), null, StructureGelItem::new
      );
      public static final Registrar.BlockRef<StructureGelBlock> BLUE_GEL = HANDLER.createStatic(
         "blue_gel", () -> new StructureGelBlock(IStructureGel.Behavior.PHOTOSENSITIVE), null, StructureGelItem::new
      );
      public static final Registrar.BlockRef<StructureGelBlock> GREEN_GEL = HANDLER.createStatic(
         "green_gel", () -> new StructureGelBlock(IStructureGel.Behavior.DIAGONAL_SPREAD), null, StructureGelItem::new
      );
      public static final Registrar.BlockRef<StructureGelBlock> CYAN_GEL = HANDLER.createStatic(
         "cyan_gel", () -> new StructureGelBlock(IStructureGel.Behavior.PHOTOSENSITIVE, IStructureGel.Behavior.DIAGONAL_SPREAD), null, StructureGelItem::new
      );
      public static final Registrar.BlockRef<StructureGelBlock> ORANGE_GEL = HANDLER.createStatic(
         "orange_gel", () -> new StructureGelBlock(IStructureGel.Behavior.DYNAMIC_SPREAD_DIST), null, StructureGelItem::new
      );
      public static final Registrar.BlockRef<StructureGelBlock> YELLOW_GEL = HANDLER.createStatic(
         "yellow_gel", () -> new AxisStructureGelBlock(IStructureGel.Behavior.AXIS_SPREAD), null, StructureGelItem::new
      );
      public static final Registrar.BlockRef<DataHandlerBlock> DATA_HANDLER = HANDLER.createStatic(
         "data_handler",
         () -> new DataHandlerBlock(Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.STRUCTURE_BLOCK)),
         new net.minecraft.world.item.Item.Properties().rarity(Rarity.EPIC),
         GameMasterBlockItem::new
      );
      public static final Registrar.BlockRef<DynamicSpawnerBlock> DYNAMIC_SPAWNER = HANDLER.createStatic(
         "dynamic_spawner",
         () -> new DynamicSpawnerBlock(
            Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.SPAWNER).lootFrom(() -> net.minecraft.world.level.block.Blocks.SPAWNER)
         ),
         new net.minecraft.world.item.Item.Properties().rarity(Rarity.EPIC)
      );
   }

   @RegistrarHolder
   public static class DataHandlerTypes {
      public static final RegistrarHandler<DataHandlerType<?>> HANDLER = RegistrarHandler.getOrCreate(
         StructureGelRegistries.Keys.DATA_HANDLER_TYPE, "structure_gel"
      );
      public static final Registrar.Static<DataHandlerType<EmptyDataHandler>> EMPTY = HANDLER.createStatic(
         "empty", () -> new DataHandlerType<>(EmptyDataHandler::new, EmptyDataHandler::parser)
      );
      public static final Registrar.Static<DataHandlerType<ChestHandler>> CHEST = HANDLER.createStatic(
         "chest", () -> new DataHandlerType<>(ChestHandler::new, ChestHandler::parser)
      );
      public static final Registrar.Static<DataHandlerType<EntityHandler>> ENTITY = HANDLER.createStatic(
         "entity", () -> new DataHandlerType<>(EntityHandler::new, EntityHandler::parser)
      );
      public static final Registrar.Static<DataHandlerType<VanillaSpawnerHandler>> SPAWNER = HANDLER.createStatic(
         "spawner", () -> new DataHandlerType<>(VanillaSpawnerHandler::new, VanillaSpawnerHandler::parser)
      );
      public static final Registrar.Static<DataHandlerType<DynamicSpawnerHandler>> DYNAMIC_SPAWNER = HANDLER.createStatic(
         "dynamic_spawner", () -> new DataHandlerType<>(DynamicSpawnerHandler::new, DynamicSpawnerHandler::parser)
      );
      public static final Registrar.Static<DataHandlerType<BlockStateHandler>> BLOCK_STATE = HANDLER.createStatic(
         "block_state", () -> new DataHandlerType<>(BlockStateHandler::new, BlockStateHandler::parser)
      );
      public static final Registrar.Static<DataHandlerType<ConfiguredFeatureHandler>> CONFIGURED_FEATURE = HANDLER.createStatic(
         "configured_feature", () -> new DataHandlerType<>(ConfiguredFeatureHandler::new, ConfiguredFeatureHandler::parser)
      );
      public static final Registrar.Static<DataHandlerType<ArchaeologyHandler>> ARCHAEOLOGY_HANDLER = HANDLER.createStatic(
         "archaeology", () -> new DataHandlerType<>(ArchaeologyHandler::new, ArchaeologyHandler::parser)
      );
   }

   @RegistrarHolder
   public static class DynamicSpawnerTypes {
      public static final RegistrarHandler<DynamicSpawnerType> HANDLER = RegistrarHandler.getOrCreate(
         StructureGelRegistries.Keys.DYNAMIC_SPAWNER_TYPE, "structure_gel"
      );
      public static final Registrar.Static<DynamicSpawnerType> DEFAULT = HANDLER.createStatic(
         "default", () -> new DynamicSpawnerType((spawner, level, pos) -> {})
      );
   }

   @RegistrarHolder
   public static class HolderSetTypes {
      public static final RegistrarHandler<HolderSetType> HANDLER = RegistrarHandler.getOrCreate(Keys.HOLDER_SET_TYPES, "structure_gel");
      public static final Registrar.Static<HolderSetType> FILTER = HANDLER.createStatic("filter", () -> FilterHolderSet::codec);
   }

   @RegistrarHolder
   public static class Items {
      public static final RegistrarHandler.ItemHandler HANDLER = RegistrarHandler.getOrCreateItems("structure_gel");
      public static final Registrar.ItemRef<BuildingToolItem> BUILDING_TOOL = HANDLER.createStatic(
         "building_tool", () -> new BuildingToolItem(new net.minecraft.world.item.Item.Properties().stacksTo(1).rarity(Rarity.EPIC))
      );
      public static final Registrar.ItemRef<BlockPaletteItem> BLOCK_PALETTE = HANDLER.createStatic(
         "block_palette", () -> new BlockPaletteItem(new net.minecraft.world.item.Item.Properties().stacksTo(1).rarity(Rarity.EPIC))
      );
   }

   @RegistrarHolder
   public static class JigsawDeserializers {
      public static final RegistrarHandler<StructurePoolElementType<?>> HANDLER = RegistrarHandler.getOrCreate(
         Registries.STRUCTURE_POOL_ELEMENT, "structure_gel"
      );
      public static final Registrar.Static<StructurePoolElementType<GelSinglePoolElement>> GEL_SINGLE_POOL_ELEMENT = HANDLER.createStatic(
         "gel_single_pool_element", () -> () -> GelSinglePoolElement.CODEC
      );
   }

   @RegistrarHolder
   public static class JigsawTypes {
      public static final RegistrarHandler<JigsawCapabilityType<?>> HANDLER = RegistrarHandler.getOrCreate(
         StructureGelRegistries.Keys.JIGSAW_TYPE, "structure_gel"
      );
      public static final Registrar.Static<JigsawCapabilityType<JigsawCapability.Vanilla>> VANILLA = HANDLER.createStatic(
         "vanilla", () -> () -> JigsawCapability.Vanilla.CODEC
      );
   }

   @RegistrarHolder
   public static class LootTableAliases {
      public static final RegistrarHandler<ResourceLocation> HANDLER = RegistrarHandler.getOrCreate(
         StructureGelRegistries.Keys.LOOT_TABLE_ALIAS, "structure_gel"
      );
      public static final Registrar.Static<ResourceLocation> SIMPLE_DUNGEON = HANDLER.createStatic("dungeon", () -> BuiltInLootTables.SIMPLE_DUNGEON);
   }

   @RegistrarHolder
   public static class Processors {
      public static final RegistrarHandler<StructureProcessorType<?>> HANDLER = RegistrarHandler.getOrCreate(Registries.STRUCTURE_PROCESSOR, "structure_gel");
      public static final Registrar.Static<StructureProcessorType<RemoveGelStructureProcessor>> REMOVE_FILLER = HANDLER.createStatic(
         "remove_filler", () -> () -> RemoveGelStructureProcessor.CODEC
      );
      public static final Registrar.Static<StructureProcessorType<RandomBlockSwapProcessor>> REPLACE_BLOCK = HANDLER.createStatic(
         "replace_block", () -> () -> RandomBlockSwapProcessor.CODEC
      );
      public static final Registrar.Static<StructureProcessorType<RandomTagSwapProcessor>> REPLACE_TAG = HANDLER.createStatic(
         "replace_tag", () -> () -> RandomTagSwapProcessor.CODEC
      );
      public static final Registrar.Static<StructureProcessorType<RandomStateSwapProcessor>> REPLACE_STATE = HANDLER.createStatic(
         "replace_state", () -> () -> RandomStateSwapProcessor.CODEC
      );
   }

   @RegistrarHolder
   public static class StructureModifierSerializers {
      public static final RegistrarHandler<Codec<? extends StructureModifier>> HANDLER = RegistrarHandler.getOrCreate(
         Keys.STRUCTURE_MODIFIER_SERIALIZERS, "structure_gel"
      );
      public static final Registrar.Static<Codec<StructureConfigModifier>> CONFIG_MODIFIER = HANDLER.createStatic(
         "config_modifier", () -> StructureConfigModifier.CODEC
      );
   }

   @RegistrarHolder
   public static class StructureModifiers {
      public static final RegistrarHandler<StructureModifier> HANDLER = RegistrarHandler.getOrCreate(Keys.STRUCTURE_MODIFIERS, "structure_gel");
      public static final Registrar.Pointer<StructureConfigModifier> CONFIG_MODIFIER = HANDLER.createPointer(
         "config_modifier", () -> new StructureConfigModifier(null)
      );
   }

   @RegistrarHolder
   public static class StructurePieceTypes {
      public static final RegistrarHandler<StructurePieceType> HANDLER = RegistrarHandler.getOrCreate(Registries.STRUCTURE_PIECE, "structure_gel");
      public static final Registrar.Static<StructurePieceType> VANILLA_JIGSAW = HANDLER.createStatic("gel_jigsaw", () -> VanillaJigsawStructurePiece::new);
   }

   @RegistrarHolder
   public static class StructurePlacementTypes {
      public static final RegistrarHandler<StructurePlacementType<?>> HANDLER = RegistrarHandler.getOrCreate(Registries.STRUCTURE_PLACEMENT, "structure_gel");
      public static final Registrar.Static<StructurePlacementType<GridStructurePlacement>> GRID_PLACEMENT = HANDLER.createStatic(
         "grid_placement", () -> () -> GridStructurePlacement.CODEC
      );
   }

   @RegistrarHolder
   public static class StructureTypes {
      public static final RegistrarHandler<StructureType<?>> HANDLER = RegistrarHandler.getOrCreate(Registries.STRUCTURE_TYPE, "structure_gel");
      public static final Registrar.Static<StructureType<ExtendedJigsawStructure>> EXTENDED_JIGSAW = HANDLER.createStatic(
         "extended_jigsaw", () -> () -> ExtendedJigsawStructure.CODEC
      );
   }
}
