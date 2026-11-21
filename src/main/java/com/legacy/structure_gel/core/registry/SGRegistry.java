package com.legacy.structure_gel.core.registry;

import java.util.List;
import java.util.function.Supplier;

import com.legacy.structure_gel.api.block.gel.GelSpreadBehavior;
import com.legacy.structure_gel.api.block.gel.GelSpreadBehavior.FaceAndEdgeSpread;
import com.legacy.structure_gel.api.block.gel.GelSpreadBehavior.FaceSpread;
import com.legacy.structure_gel.api.block.gel.GelSpreadBehavior.InitialAxisSpread;
import com.legacy.structure_gel.api.block.gel.GelSpreadBehaviorType;
import com.legacy.structure_gel.api.block.gel.GelSpreadRestriction;
import com.legacy.structure_gel.api.block.gel.GelSpreadRestriction.DistanceRestriction;
import com.legacy.structure_gel.api.block.gel.GelSpreadRestriction.ItemStackDistance;
import com.legacy.structure_gel.api.block.gel.GelSpreadRestriction.NoSkyRestriction;
import com.legacy.structure_gel.api.block.gel.GelSpreadRestrictionType;
import com.legacy.structure_gel.api.block.gel.StructureGelBlock;
import com.legacy.structure_gel.api.data.tags.FilterHolderSet;
import com.legacy.structure_gel.api.data_handler.DataHandlerType;
import com.legacy.structure_gel.api.dynamic_spawner.DynamicSpawnerType;
import com.legacy.structure_gel.api.item.StructureGelItem;
import com.legacy.structure_gel.api.registry.RegistrarHolder;
import com.legacy.structure_gel.api.registry.StructureGelRegistries;
import com.legacy.structure_gel.api.registry.registrar.Registrar;
import com.legacy.structure_gel.api.registry.registrar.Registrar.BlockRef;
import com.legacy.structure_gel.api.registry.registrar.Registrar.ItemRef;
import com.legacy.structure_gel.api.registry.registrar.Registrar.Static;
import com.legacy.structure_gel.api.registry.registrar.RegistrarHandler;
import com.legacy.structure_gel.api.structure.ExtendedJigsawStructure;
import com.legacy.structure_gel.api.structure.GridStructurePlacement;
import com.legacy.structure_gel.api.structure.jigsaw.JigsawCapability;
import com.legacy.structure_gel.api.structure.jigsaw.JigsawCapabilityType;
import com.legacy.structure_gel.api.structure.processor.RandomBlockSwapProcessor;
import com.legacy.structure_gel.api.structure.processor.RandomStateSwapProcessor;
import com.legacy.structure_gel.api.structure.processor.RandomTagSwapProcessor;
import com.legacy.structure_gel.api.structure.processor.RemoveGelStructureProcessor;
import com.legacy.structure_gel.core.StructureGelMod;
import com.legacy.structure_gel.core.block.DataHandlerBlock;
import com.legacy.structure_gel.core.block.DynamicSpawnerBlock;
import com.legacy.structure_gel.core.block.GelSpreaderBlock;
import com.legacy.structure_gel.core.block_entity.DataHandlerBlockEntity;
import com.legacy.structure_gel.core.block_entity.DynamicSpawnerBlockEntity;
import com.legacy.structure_gel.core.block_entity.GelSpreaderBlockEntity;
import com.legacy.structure_gel.core.data_components.BlockPalette;
import com.legacy.structure_gel.core.data_components.BuildingToolProperties;
import com.legacy.structure_gel.core.data_components.CloneRegion;
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
import com.legacy.structure_gel.core.item.building_tool.SmartBoundingBox;
import com.legacy.structure_gel.core.structure.jigsaw.GelSinglePoolElement;
import com.legacy.structure_gel.core.structure.jigsaw.VanillaJigsawStructurePiece;
import com.legacy.structure_gel.core.structure.modifiers.StructureConfigModifier;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.GameMasterBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElementType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.neoforge.common.world.StructureModifier;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.holdersets.HolderSetType;

public class SGRegistry
{
	// Vanilla Registries

	@RegistrarHolder
	public static interface BlockTypes
	{
		RegistrarHandler<MapCodec<? extends Block>> HANDLER = RegistrarHandler.getOrCreate(Registries.BLOCK_TYPE, StructureGelMod.MODID);
		Static<MapCodec<GelSpreaderBlock>> GEL_SPREADER = HANDLER.createStatic("gel_spreader", () -> GelSpreaderBlock.CODEC);
		Static<MapCodec<StructureGelBlock>> GEL = HANDLER.createStatic("gel", () -> StructureGelBlock.CODEC);
		Static<MapCodec<DataHandlerBlock>> DATA_HANDLER = HANDLER.createStatic("data_handler", () -> DataHandlerBlock.CODEC);
		Static<MapCodec<DynamicSpawnerBlock>> DYNAMIC_SPAWNER = HANDLER.createStatic("dynamic_spawner", () -> DynamicSpawnerBlock.CODEC);
	}

	@RegistrarHolder
	public static interface Blocks
	{
		RegistrarHandler.BlockHandler HANDLER = RegistrarHandler.getOrCreateBlocks(StructureGelMod.MODID).addHandlerListener(Blocks::register);
		BlockRef<Block> GEL_SPREADER = HANDLER.block("gel_spreader");
		BlockRef<Block> RED_GEL = HANDLER.block("red_gel");
		BlockRef<Block> BLUE_GEL = HANDLER.block("blue_gel");
		BlockRef<Block> GREEN_GEL = HANDLER.block("green_gel");
		BlockRef<Block> CYAN_GEL = HANDLER.block("cyan_gel");
		BlockRef<Block> ORANGE_GEL = HANDLER.block("orange_gel");
		BlockRef<Block> YELLOW_GEL = HANDLER.block("yellow_gel");

		BlockRef<Block> DATA_HANDLER = HANDLER.block("data_handler");
		BlockRef<Block> DYNAMIC_SPAWNER = HANDLER.block("dynamic_spawner");

		private static void register(RegistrarHandler.BlockHandler handler)
		{
			Supplier<BlockBehaviour.Properties> gelProps = () -> BlockBehaviour.Properties.of().pushReaction(PushReaction.DESTROY).mapColor(MapColor.NONE).noCollission().strength(0.0F).noLootTable().noOcclusion().sound(SoundType.SLIME_BLOCK).isValidSpawn((state, reader, pos, entity) -> false).isSuffocating((state, reader, pos) -> false).isViewBlocking((state, reader, pos) -> false);
			Supplier<Item.Properties> gelItemProps = () -> new Item.Properties().rarity(Rarity.EPIC);

			handler.registerBlockOnly(GEL_SPREADER, GelSpreaderBlock::new, gelProps);
			handler.registerBlock(RED_GEL, p -> new StructureGelBlock(p, FaceSpread.INSTANCE, List.of(DistanceRestriction.FIFTY)), gelProps, StructureGelItem::new, gelItemProps);
			handler.registerBlock(BLUE_GEL, p -> new StructureGelBlock(p, FaceSpread.INSTANCE, List.of(DistanceRestriction.FIFTY, NoSkyRestriction.INSTANCE)), gelProps, StructureGelItem::new, gelItemProps);
			handler.registerBlock(GREEN_GEL, p -> new StructureGelBlock(p, FaceAndEdgeSpread.INSTANCE, List.of(DistanceRestriction.FIFTY)), gelProps, StructureGelItem::new, gelItemProps);
			handler.registerBlock(CYAN_GEL, p -> new StructureGelBlock(p, FaceAndEdgeSpread.INSTANCE, List.of(DistanceRestriction.FIFTY, NoSkyRestriction.INSTANCE)), gelProps, StructureGelItem::new, gelItemProps);
			handler.registerBlock(ORANGE_GEL, p -> new StructureGelBlock(p, FaceSpread.INSTANCE, List.of(ItemStackDistance.INSTANCE)), gelProps, StructureGelItem::new, gelItemProps);
			handler.registerBlock(YELLOW_GEL, p -> new StructureGelBlock(p, InitialAxisSpread.INSTANCE, List.of(DistanceRestriction.FIFTY)), gelProps, StructureGelItem::new, gelItemProps);

			handler.registerBlock(DATA_HANDLER, DataHandlerBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.STRUCTURE_BLOCK), GameMasterBlockItem::new, () -> new Item.Properties().rarity(Rarity.EPIC));
			handler.registerBlock(DYNAMIC_SPAWNER, DynamicSpawnerBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.SPAWNER).overrideLootTable(net.minecraft.world.level.block.Blocks.SPAWNER.getLootTable()), () -> new Item.Properties().rarity(Rarity.EPIC));
		}
	}

	@RegistrarHolder
	public static interface Items
	{
		RegistrarHandler.ItemHandler HANDLER = RegistrarHandler.getOrCreateItems(StructureGelMod.MODID).addHandlerListener(Items::register);
		ItemRef<BuildingToolItem> BUILDING_TOOL = HANDLER.registerItem("building_tool");
		ItemRef<BlockPaletteItem> BLOCK_PALETTE = HANDLER.registerItem("block_palette");

		private static void register(RegistrarHandler.ItemHandler handler)
		{
			handler.registerItem(BUILDING_TOOL, BuildingToolItem::new, () -> new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));
			handler.registerItem(BLOCK_PALETTE, BlockPaletteItem::new, () -> new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));
		}
	}

	@RegistrarHolder
	public static interface DataComponents
	{
		RegistrarHandler.DataComponentHandler HANDLER = RegistrarHandler.getOrCreateDataComponents(StructureGelMod.MODID);

		Static<DataComponentType<ResourceLocation>> BUILDING_TOOL_MODE = HANDLER.component("bt_mode", builder -> builder.persistent(ResourceLocation.CODEC).networkSynchronized(ResourceLocation.STREAM_CODEC));
		Static<DataComponentType<BlockPos>> BUILDING_TOOL_POS_0 = HANDLER.component("bt_pos_0", builder -> builder.persistent(BlockPos.CODEC).networkSynchronized(BlockPos.STREAM_CODEC).cacheEncoding());
		Static<DataComponentType<BlockPos>> BUILDING_TOOL_POS_1 = HANDLER.component("bt_pos_1", builder -> builder.persistent(BlockPos.CODEC).networkSynchronized(BlockPos.STREAM_CODEC).cacheEncoding());
		List<Static<DataComponentType<BlockPos>>> TOOL_POSES = List.of(BUILDING_TOOL_POS_0, BUILDING_TOOL_POS_1);
		Static<DataComponentType<BuildingToolProperties>> BUILDING_TOOL_PROPERTIES = HANDLER.component("bt_properties", builder -> builder.persistent(BuildingToolProperties.CODEC).networkSynchronized(BuildingToolProperties.STREAM_CODEC).cacheEncoding());
		Static<DataComponentType<Integer>> BUILDING_TOOL_REACH_MODIFIER = HANDLER.component("bt_reach_modifier", builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.INT).cacheEncoding());
		Static<DataComponentType<Boolean>> BUILDING_TOOL_CAUSE_BLOCK_UPDATES = HANDLER.component("bt_cause_block_updates", builder -> builder.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL).cacheEncoding());
		Static<DataComponentType<Float>> BUILDING_TOOL_CORNER_DIST = HANDLER.component("bt_corner_dist", builder -> builder.persistent(Codec.FLOAT).networkSynchronized(ByteBufCodecs.FLOAT).cacheEncoding());
		Static<DataComponentType<Boolean>> BUILDING_TOOL_MOVING_BOUNDS = HANDLER.component("bt_moving_bounds", builder -> builder.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL).cacheEncoding());
		Static<DataComponentType<CloneRegion>> BUILDING_TOOL_CAPTURED_BLOCKS = HANDLER.component("bt_captured_blocks", builder -> builder.persistent(CloneRegion.CODEC).networkSynchronized(CloneRegion.STREAM_CODEC).cacheEncoding());
		Static<DataComponentType<BlockPalette>> BLOCK_PALETTE = HANDLER.component("block_palette", builder -> builder.persistent(BlockPalette.CODEC).networkSynchronized(BlockPalette.STREAM_CODEC));
		Static<DataComponentType<SmartBoundingBox.CornerType>> CORNER_TYPE = HANDLER.component("corner_type", builder -> builder.persistent(SmartBoundingBox.CornerType.CODEC).networkSynchronized(SmartBoundingBox.CornerType.STREAM_CODEC).cacheEncoding());

		Static<DataComponentType<ResourceKey<Structure>>> STRUCTURE = HANDLER.component("structure", builder -> builder.persistent(ResourceKey.codec(Registries.STRUCTURE)).networkSynchronized(ResourceKey.streamCodec(Registries.STRUCTURE)).cacheEncoding());
		Static<DataComponentType<GlobalPos>> STRUCTURE_LOCATION = HANDLER.component("structure_location", builder -> builder.persistent(GlobalPos.CODEC).networkSynchronized(GlobalPos.STREAM_CODEC).cacheEncoding());

	}

	@RegistrarHolder
	public static interface BlockEntities
	{
		RegistrarHandler<BlockEntityType<?>> HANDLER = RegistrarHandler.getOrCreate(Registries.BLOCK_ENTITY_TYPE, StructureGelMod.MODID);
		Static<BlockEntityType<GelSpreaderBlockEntity>> GEL_SPREADER = HANDLER.createStatic("gel_spreader", () -> new BlockEntityType<>(GelSpreaderBlockEntity::new, Blocks.GEL_SPREADER.get()));
		Static<BlockEntityType<DataHandlerBlockEntity>> DATA_HANDLER = HANDLER.createStatic("data_handler", () -> new BlockEntityType<>(DataHandlerBlockEntity::new, Blocks.DATA_HANDLER.get()));
		Static<BlockEntityType<DynamicSpawnerBlockEntity>> DYNAMIC_SPAWNER = HANDLER.createStatic("dynamic_spawner", () -> new BlockEntityType<>(DynamicSpawnerBlockEntity::new, Blocks.DYNAMIC_SPAWNER.get()));
	}

	@RegistrarHolder
	public static interface Processors
	{
		RegistrarHandler<StructureProcessorType<?>> HANDLER = RegistrarHandler.getOrCreate(Registries.STRUCTURE_PROCESSOR, StructureGelMod.MODID);
		Static<StructureProcessorType<RemoveGelStructureProcessor>> REMOVE_FILLER = HANDLER.createStatic("remove_filler", () -> () -> RemoveGelStructureProcessor.CODEC);
		Static<StructureProcessorType<RandomBlockSwapProcessor>> REPLACE_BLOCK = HANDLER.createStatic("replace_block", () -> () -> RandomBlockSwapProcessor.CODEC);
		Static<StructureProcessorType<RandomTagSwapProcessor>> REPLACE_TAG = HANDLER.createStatic("replace_tag", () -> () -> RandomTagSwapProcessor.CODEC);
		Static<StructureProcessorType<RandomStateSwapProcessor>> REPLACE_STATE = HANDLER.createStatic("replace_state", () -> () -> RandomStateSwapProcessor.CODEC);
	}

	@RegistrarHolder
	public static interface JigsawDeserializers
	{
		RegistrarHandler<StructurePoolElementType<?>> HANDLER = RegistrarHandler.getOrCreate(Registries.STRUCTURE_POOL_ELEMENT, StructureGelMod.MODID);
		Static<StructurePoolElementType<GelSinglePoolElement>> GEL_SINGLE_POOL_ELEMENT = HANDLER.createStatic("gel_single_pool_element", () -> () -> GelSinglePoolElement.CODEC);
	}

	@RegistrarHolder
	public static interface StructureTypes
	{
		RegistrarHandler<StructureType<?>> HANDLER = RegistrarHandler.getOrCreate(Registries.STRUCTURE_TYPE, StructureGelMod.MODID);
		Static<StructureType<ExtendedJigsawStructure>> EXTENDED_JIGSAW = HANDLER.createStatic("extended_jigsaw", () -> () -> ExtendedJigsawStructure.CODEC);
	}

	@RegistrarHolder
	public static interface StructurePieceTypes
	{
		RegistrarHandler<StructurePieceType> HANDLER = RegistrarHandler.getOrCreate(Registries.STRUCTURE_PIECE, StructureGelMod.MODID);
		Static<StructurePieceType> VANILLA_JIGSAW = HANDLER.createStatic("gel_jigsaw", () -> VanillaJigsawStructurePiece::new);
	}

	@RegistrarHolder
	public static interface StructurePlacementTypes
	{
		RegistrarHandler<StructurePlacementType<?>> HANDLER = RegistrarHandler.getOrCreate(Registries.STRUCTURE_PLACEMENT, StructureGelMod.MODID);
		Static<StructurePlacementType<GridStructurePlacement>> GRID_PLACEMENT = HANDLER.createStatic("grid_placement", () -> () -> GridStructurePlacement.CODEC);
	}

	// NeoForge Registries

	@RegistrarHolder
	public static interface HolderSetTypes
	{
		RegistrarHandler<HolderSetType> HANDLER = RegistrarHandler.getOrCreate(NeoForgeRegistries.Keys.HOLDER_SET_TYPES, StructureGelMod.MODID);
		Static<HolderSetType> FILTER = HANDLER.createStatic("filter", FilterHolderSet.Type::new);
	}

	@RegistrarHolder
	public static interface StructureModifierSerializers
	{
		RegistrarHandler<MapCodec<? extends StructureModifier>> HANDLER = RegistrarHandler.getOrCreate(NeoForgeRegistries.Keys.STRUCTURE_MODIFIER_SERIALIZERS, StructureGelMod.MODID);
		Static<MapCodec<StructureConfigModifier>> CONFIG_MODIFIER = HANDLER.createStatic("config_modifier", () -> StructureConfigModifier.CODEC);
	}

	@RegistrarHolder
	public static interface StructureModifiers
	{
		RegistrarHandler<StructureModifier> HANDLER = RegistrarHandler.getOrCreate(NeoForgeRegistries.Keys.STRUCTURE_MODIFIERS, StructureGelMod.MODID);
		Registrar.Pointer<StructureConfigModifier> CONFIG_MODIFIER = HANDLER.createPointer("config_modifier", () -> new StructureConfigModifier(null)); // This will contain biome registry in datapack
	}

	// Structure Gel Registries

	@RegistrarHolder
	public static interface DataHandlerTypes
	{
		RegistrarHandler<DataHandlerType<?>> HANDLER = RegistrarHandler.getOrCreate(StructureGelRegistries.Keys.DATA_HANDLER_TYPE, StructureGelMod.MODID);
		Static<DataHandlerType<EmptyDataHandler>> EMPTY = HANDLER.createStatic("empty", () -> new DataHandlerType<>(EmptyDataHandler::new, EmptyDataHandler.PARSER));
		Static<DataHandlerType<ChestHandler>> CHEST = HANDLER.createStatic("chest", () -> new DataHandlerType<>(ChestHandler::new, ChestHandler.PARSER));
		Static<DataHandlerType<EntityHandler>> ENTITY = HANDLER.createStatic("entity", () -> new DataHandlerType<>(EntityHandler::new, EntityHandler.PARSER));
		Static<DataHandlerType<VanillaSpawnerHandler>> SPAWNER = HANDLER.createStatic("spawner", () -> new DataHandlerType<>(VanillaSpawnerHandler::new, VanillaSpawnerHandler.PARSER));
		Static<DataHandlerType<DynamicSpawnerHandler>> DYNAMIC_SPAWNER = HANDLER.createStatic("dynamic_spawner", () -> new DataHandlerType<>(DynamicSpawnerHandler::new, DynamicSpawnerHandler.PARSER));
		Static<DataHandlerType<BlockStateHandler>> BLOCK_STATE = HANDLER.createStatic("block_state", () -> new DataHandlerType<>(BlockStateHandler::new, BlockStateHandler.PARSER));
		Static<DataHandlerType<ConfiguredFeatureHandler>> CONFIGURED_FEATURE = HANDLER.createStatic("configured_feature", () -> new DataHandlerType<>(ConfiguredFeatureHandler::new, ConfiguredFeatureHandler.PARSER));
		Static<DataHandlerType<ArchaeologyHandler>> ARCHAEOLOGY_HANDLER = HANDLER.createStatic("archaeology", () -> new DataHandlerType<>(ArchaeologyHandler::new, ArchaeologyHandler.PARSER));
	}

	@RegistrarHolder
	public static interface LootTableAliases
	{
		RegistrarHandler<ResourceKey<LootTable>> HANDLER = RegistrarHandler.getOrCreate(StructureGelRegistries.Keys.LOOT_TABLE_ALIAS, StructureGelMod.MODID);
		Static<ResourceKey<LootTable>> SIMPLE_DUNGEON = HANDLER.createStatic("dungeon", () -> BuiltInLootTables.SIMPLE_DUNGEON);
	}

	@RegistrarHolder
	public static interface DynamicSpawnerTypes
	{
		RegistrarHandler<DynamicSpawnerType> HANDLER = RegistrarHandler.getOrCreate(StructureGelRegistries.Keys.DYNAMIC_SPAWNER_TYPE, StructureGelMod.MODID);
		Static<DynamicSpawnerType> DEFAULT = HANDLER.createStatic("default", () -> (builder, registry) ->
		{
			// Commentted out by default
			//builder.spawnData(EntityType.ZOMBIE, 2).spawnData(EntityType.SKELETON);
			//builder.maxNearbyEntities(10);
			//builder.spawnCount(12);
		});
	}

	@RegistrarHolder
	public static interface JigsawTypes
	{
		RegistrarHandler<JigsawCapabilityType<?>> HANDLER = RegistrarHandler.getOrCreate(StructureGelRegistries.Keys.JIGSAW_TYPE, StructureGelMod.MODID);
		Static<JigsawCapabilityType<JigsawCapability.Vanilla>> VANILLA = HANDLER.createStatic("vanilla", () -> () -> JigsawCapability.Vanilla.CODEC);
	}

	@RegistrarHolder
	public static interface GelSpreadBehaviors
	{
		RegistrarHandler<GelSpreadBehaviorType> HANDLER = RegistrarHandler.getOrCreate(StructureGelRegistries.Keys.GEL_SPREAD_BEHAVIOR, StructureGelMod.MODID);
		Static<GelSpreadBehaviorType> FACE = HANDLER.createStatic("face", () -> () -> GelSpreadBehavior.FaceSpread.CODEC);
		Static<GelSpreadBehaviorType> EDGE = HANDLER.createStatic("edge", () -> () -> GelSpreadBehavior.EdgeSpread.CODEC);
		Static<GelSpreadBehaviorType> FACE_AND_EDGE = HANDLER.createStatic("face_and_edge", () -> () -> GelSpreadBehavior.FaceAndEdgeSpread.CODEC);
		Static<GelSpreadBehaviorType> INITIAL_AXIS = HANDLER.createStatic("initial_axis", () -> () -> GelSpreadBehavior.InitialAxisSpread.CODEC);
	}

	@RegistrarHolder
	public static interface GelSpreadRestrictions
	{
		RegistrarHandler<GelSpreadRestrictionType> HANDLER = RegistrarHandler.getOrCreate(StructureGelRegistries.Keys.GEL_SPREAD_RESTRICTION, StructureGelMod.MODID);
		Static<GelSpreadRestrictionType> NO_SKY_VISIBILITY = HANDLER.createStatic("no_sky_visibility", () -> () -> GelSpreadRestriction.NoSkyRestriction.CODEC);
		Static<GelSpreadRestrictionType> DISTANCE = HANDLER.createStatic("distance", () -> () -> GelSpreadRestriction.DistanceRestriction.CODEC);
		Static<GelSpreadRestrictionType> ITEM_STACK_COUNT_DISTANCE = HANDLER.createStatic("item_stack_count_distance", () -> () -> GelSpreadRestriction.ItemStackDistance.CODEC);
	}
}
