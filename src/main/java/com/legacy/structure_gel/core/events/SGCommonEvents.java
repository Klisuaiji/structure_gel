package com.legacy.structure_gel.core.events;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.legacy.structure_gel.api.data.providers.NestedDataProvider;
import com.legacy.structure_gel.api.registry.StructureGelRegistries;
import com.legacy.structure_gel.api.registry.registrar.RegistrarHandler;
import com.legacy.structure_gel.core.StructureGelMod;
import com.legacy.structure_gel.core.commands.StructureGelCommand;
import com.legacy.structure_gel.core.data.generators.SGTagProv;
import com.legacy.structure_gel.core.item.building_tool.ActionHistory;
import com.legacy.structure_gel.core.item.building_tool.BuildingToolItem;
import com.legacy.structure_gel.core.item.building_tool.BuildingToolMode;
import com.legacy.structure_gel.core.item.building_tool.BuildingToolModes;
import com.legacy.structure_gel.core.registry.RegistrarLoader;
import com.legacy.structure_gel.core.registry.SGRegistry;
import com.legacy.structure_gel.core.util.Internal;

import net.minecraft.DetectedVersion;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.metadata.PackMetadataGenerator;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.util.InclusiveRange;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTab.TabVisibility;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.properties.StructureMode;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

@Internal
public class SGCommonEvents
{
	@EventBusSubscriber(modid = StructureGelMod.MODID, )
	protected static class ModBus
	{
		@SubscribeEvent
		protected static void newRegistries(final NewRegistryEvent event)
		{
			event.register(StructureGelRegistries.DATA_HANDLER_TYPE);
			event.register(StructureGelRegistries.LOOT_TABLE_ALIAS);
			event.register(StructureGelRegistries.DYNAMIC_SPAWNER_TYPE);
			event.register(StructureGelRegistries.JIGSAW_TYPE);
			event.register(StructureGelRegistries.GEL_SPREAD_BEHAVIOR);
			event.register(StructureGelRegistries.GEL_SPREAD_RESTRICTION);
		}

		@SubscribeEvent(priority = EventPriority.HIGHEST)
		protected static void onRegistry(final RegisterEvent event)
		{
			// Locate and registrar all RegistrarHandler.
			// Since NeoForge sorts event priority then by mod order, this works. Forge sorts by mod order, then event priority, so it would need to be different.
			RegistrarLoader.loadRegistrars();
		}

		@SubscribeEvent
		protected static void commonSetup(final FMLCommonSetupEvent event)
		{
			BuildingToolModes.init();
			ActionHistory.init();
		}

		@SubscribeEvent
		protected static void onTabLoad(final BuildCreativeModeTabContentsEvent event)
		{
			if (event.getTabKey().equals(CreativeModeTabs.OP_BLOCKS) && event.hasPermissions())
			{
				List<ItemStack> structureBlocks = new ArrayList<>(4);
				for (StructureMode mode : StructureMode.values())
				{
					ItemStack stack = new ItemStack(Blocks.STRUCTURE_BLOCK);
					CompoundTag blockEntityTag = new CompoundTag();
					blockEntityTag.putString("id", BlockEntityType.STRUCTURE_BLOCK.builtInRegistryHolder().key().location().toString());
					blockEntityTag.putString("mode", mode.name());
					stack.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(blockEntityTag));
					structureBlocks.add(stack);
				}
				insertAfter(event, structureBlocks, Items.STRUCTURE_BLOCK);

				event.accept(SGRegistry.Blocks.RED_GEL);
				event.accept(SGRegistry.Blocks.BLUE_GEL);
				event.accept(SGRegistry.Blocks.GREEN_GEL);
				event.accept(SGRegistry.Blocks.CYAN_GEL);
				event.accept(SGRegistry.Blocks.ORANGE_GEL);
				event.accept(SGRegistry.Blocks.YELLOW_GEL);
				event.accept(Items.GUNPOWDER);

				event.accept(SGRegistry.Blocks.DATA_HANDLER);
				event.accept(SGRegistry.Blocks.DYNAMIC_SPAWNER);

				event.accept(SGRegistry.Items.BUILDING_TOOL);
				for (BuildingToolMode mode : BuildingToolModes.REGISTRY)
				{
					if (!mode.isNone())
					{
						ItemStack stack = SGRegistry.Items.BUILDING_TOOL.getDefaultInstance();
						BuildingToolItem.setMode(stack, mode);
						event.accept(stack, CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY);
					}
				}
			}

			if (event.getTabKey().equals(CreativeModeTabs.SPAWN_EGGS))
			{
				insertAfter(event, List.of(SGRegistry.Blocks.DYNAMIC_SPAWNER.asItem().getDefaultInstance()), Items.SPAWNER);
			}
		}

		private static void insertAfter(BuildCreativeModeTabContentsEvent event, List<ItemStack> items, ItemLike target)
		{
			ItemStack currentStack = null;

			for (ItemStack e : event.getParentEntries())
			{
				if (e.getItem() == target)
				{
					currentStack = e;
					break;
				}
			}

			for (var item : items)
				event.insertAfter(currentStack, currentStack = item, TabVisibility.PARENT_AND_SEARCH_TABS);
		}

		@SubscribeEvent
		protected static void gatherData(final GatherDataEvent event)
		{
			DataGenerator dataGen = event.getGenerator();
			PackOutput packOutput = dataGen.getPackOutput();
			boolean server = true;

			DatapackBuiltinEntriesProvider registrarProv = new DatapackBuiltinEntriesProvider(packOutput, event.getLookupProvider(), RegistrarHandler.injectRegistries(new RegistrySetBuilder()), Set.of(StructureGelMod.MODID));
			registrarProv = dataGen.addProvider(server, registrarProv);

			// Server
			var blockTags = new SGTagProv.BlockTagProv(packOutput, registrarProv.getRegistryProvider(), event.getExistingFileHelper());
			dataGen.addProvider(server, blockTags);
			dataGen.addProvider(server, new SGTagProv.ItemTagProv(packOutput, registrarProv.getRegistryProvider(), blockTags.contentsGetter(), event.getExistingFileHelper()));
			dataGen.addProvider(server, new SGTagProv.StructureTagProv(packOutput, registrarProv.getRegistryProvider(), event.getExistingFileHelper()));

			dataGen.addProvider(server, packMcmeta(packOutput, "Structure Gel resources"));
		}

		private static final DataProvider packMcmeta(PackOutput output, String description)
		{
			int serverVersion = DetectedVersion.BUILT_IN.getPackVersion(PackType.SERVER_DATA);
			return NestedDataProvider.of(new PackMetadataGenerator(output).add(PackMetadataSection.TYPE, new PackMetadataSection(Component.literal(description), serverVersion, Optional.of(new InclusiveRange<>(0, Integer.MAX_VALUE)))), description);
		}
	}

	@EventBusSubscriber(modid = StructureGelMod.MODID, )
	protected static class ForgeBus
	{
		@SubscribeEvent
		protected static void registerCommands(final RegisterCommandsEvent event)
		{
			StructureGelCommand.register(event.getDispatcher());
		}

		@SubscribeEvent
		protected static void onRightClickBlock(final PlayerInteractEvent.RightClickBlock event)
		{
			if (event.getItemStack().is(SGRegistry.Items.BUILDING_TOOL.get()))
			{
				event.setUseBlock(TriState.FALSE);
			}
		}

		/*@SubscribeEvent
		protected static void onTemplateLoad(final LoadStructureTemplateEvent event)
		{
			if (event.getId().toString().contains("/houses/"))
			{
				List<Block> options = List.of(Blocks.RED_STAINED_GLASS, Blocks.ORANGE_STAINED_GLASS, Blocks.YELLOW_STAINED_GLASS, Blocks.GREEN_STAINED_GLASS, Blocks.BLUE_STAINED_GLASS, Blocks.PURPLE_STAINED_GLASS);
				BlockState state = options.get(new Random().nextInt(options.size())).defaultBlockState();
				for (var palette : event.getPalettes())
				{
					BoundingBox area = BoundingBox.fromCorners(BlockPos.ZERO, event.getSize());
					for (int x = area.minX(); x < area.maxX(); x++)
					{
						for (int z = area.minZ(); z < area.maxZ(); z++)
						{
							for (int y = area.minY(); y < area.maxY(); y++)
							{
								BlockPos pos = new BlockPos(x, y, z);
								if (event.hasBlock(palette, pos, info -> !info.state().isAir() && !info.state().is(Blocks.JIGSAW)))
									event.setBlock(palette, pos, state, null);
							}
						}
					}
				}
			}
		
			if (event.getId().getPath().equals(("pillager_outpost/base_plate")))
			{
				// EAST_UP is positive X
				event.setJigsaw(Blocks.JIGSAW.defaultBlockState().setValue(JigsawBlock.ORIENTATION, FrontAndTop.EAST_UP), event.moveInBounds(new BlockPos(1000, 0, event.getSize().getZ() / 2)), jigsaw ->
				{
					jigsaw.setPool(ResourceKey.create(Registries.TEMPLATE_POOL, new ResourceLocation("village/plains/streets")));
					jigsaw.setName(new ResourceLocation("minecraft:street"));
					jigsaw.setTarget(new ResourceLocation("minecraft:street"));
					jigsaw.setFinalState(Blocks.SNOW.builtInRegistryHolder().key().location().toString());
				});
			}
		}*/
	}
}
