package com.legacy.structure_gel.core.events;

import com.legacy.blue_skies.compat.*;
import com.legacy.structure_gel.api.data.providers.NestedDataProvider;
import com.legacy.structure_gel.api.registry.StructureGelRegistries;
import com.legacy.structure_gel.api.registry.registrar.RegistrarHandler;
import com.legacy.structure_gel.core.capability.GelCapability;
import com.legacy.structure_gel.core.commands.StructureGelCommand;
import com.legacy.structure_gel.core.data.generators.SGTagProv;
import com.legacy.structure_gel.core.item.building_tool.ActionHistory;
import com.legacy.structure_gel.core.item.building_tool.BuildingToolItem;
import com.legacy.structure_gel.core.item.building_tool.BuildingToolMode;
import com.legacy.structure_gel.core.item.building_tool.BuildingToolModes;
import com.legacy.structure_gel.core.registry.RegistrarLoader;
import com.legacy.structure_gel.core.registry.SGRegistry;
import com.legacy.structure_gel.core.util.Internal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.DetectedVersion;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.metadata.PackMetadataGenerator;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.util.InclusiveRange;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.CreativeModeTab.TabVisibility;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.StructureMode;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.Event.Result;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.common.util.MutableHashedLinkedMap;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

@Internal
public class SGCommonEvents {
   @EventBusSubscriber(modid = "structure_gel", bus = Bus.FORGE)
   protected static class ForgeBus {
      @SubscribeEvent
      protected static void onEntityJoinLevel(EntityJoinLevelEvent event) {
         if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            GelCapability.ifPresent(serverPlayer, gelPlayer -> gelPlayer.sendToClient(serverPlayer));
         }
      }

      @SubscribeEvent
      protected static void registerCommands(RegisterCommandsEvent event) {
         StructureGelCommand.register(event.getDispatcher());
      }

      @SubscribeEvent
      protected static void onRightClickBlock(RightClickBlock event) {
         if (event.getItemStack().is(SGRegistry.Items.BUILDING_TOOL.get())) {
            event.setUseBlock(Result.DENY);
         }
      }
   }

   @EventBusSubscriber(modid = "structure_gel", bus = Bus.MOD)
   protected static class ModBus {
      @SubscribeEvent
      protected static void newRegistries(NewRegistryEvent event) {
         event.register(StructureGelRegistries.DATA_HANDLER_TYPE);
         event.register(StructureGelRegistries.LOOT_TABLE_ALIAS);
         event.register(StructureGelRegistries.DYNAMIC_SPAWNER_TYPE);
         event.register(StructureGelRegistries.JIGSAW_TYPE);
      }

      @SubscribeEvent(priority = EventPriority.HIGHEST)
      protected static void onRegistry(RegisterEvent event) {
         RegistrarLoader.loadRegistrars();
      }

      @SubscribeEvent
      protected static void commonSetup(FMLCommonSetupEvent event) {
         BuildingToolModes.init();
         ActionHistory.init();
      }

      @SubscribeEvent
      protected static void onTabLoad(BuildCreativeModeTabContentsEvent event) {
         if (event.getTabKey().equals(CreativeModeTabs.OP_BLOCKS) && event.hasPermissions()) {
            List<ItemStack> structureBlocks = new ArrayList<>(4);

            for (StructureMode mode : StructureMode.values()) {
               ItemStack stack = new ItemStack(Blocks.STRUCTURE_BLOCK);
               CompoundTag tag = LegacyNbt.getOrCreate(stack);
               CompoundTag blockEntityTag = new CompoundTag();
               blockEntityTag.putString("mode", mode.name());
               tag.put("BlockEntityTag", blockEntityTag);
               structureBlocks.add(stack);
            }

            insertAfter(event.getEntries(), structureBlocks, Items.STRUCTURE_BLOCK);
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

            for (BuildingToolMode mode : BuildingToolModes.REGISTRY) {
               if (!mode.isNone()) {
                  ItemStack stack = SGRegistry.Items.BUILDING_TOOL.getDefaultInstance();
                  BuildingToolItem.setMode(stack, mode);
                  event.accept(stack, TabVisibility.SEARCH_TAB_ONLY);
               }
            }
         }
      }

      private static void insertAfter(MutableHashedLinkedMap<ItemStack, TabVisibility> entries, List<ItemStack> items, ItemLike target) {
         ItemStack currentStack = null;

         for (Entry<ItemStack, TabVisibility> e : entries) {
            if (e.getKey().getItem() == target) {
               currentStack = e.getKey();
               break;
            }
         }

         for (ItemStack item : items) {
            ItemStack var10001 = currentStack;
            currentStack = item;
            entries.putAfter(var10001, item, TabVisibility.PARENT_AND_SEARCH_TABS);
         }
      }

      @SubscribeEvent
      protected static void gatherData(GatherDataEvent event) {
         DataGenerator dataGen = event.getGenerator();
         ExistingFileHelper existingHelper = event.getExistingFileHelper();
         PackOutput packOutput = dataGen.getPackOutput();
         boolean server = event.includeServer();
         DatapackBuiltinEntriesProvider registrarProv = new DatapackBuiltinEntriesProvider(
            packOutput, event.getLookupProvider(), RegistrarHandler.injectRegistries(new RegistrySetBuilder()), Set.of("structure_gel")
         );
         registrarProv = (DatapackBuiltinEntriesProvider)dataGen.addProvider(server, registrarProv);
         dataGen.addProvider(server, new SGTagProv.BlockTagProv(packOutput, registrarProv.getRegistryProvider(), existingHelper));
         dataGen.addProvider(server, new SGTagProv.StructureTagProv(packOutput, registrarProv.getRegistryProvider(), existingHelper));
         dataGen.addProvider(server, packMcmeta(packOutput, "Structure Gel resources"));
      }

      private static final DataProvider packMcmeta(PackOutput output, String description) {
         int serverVersion = DetectedVersion.BUILT_IN.getPackVersion(PackType.SERVER_DATA);
         return NestedDataProvider.of(
            new PackMetadataGenerator(output)
               .add(
                  PackMetadataSection.TYPE,
                  new PackMetadataSection(Component.literal(description), serverVersion, Optional.of(new InclusiveRange(0, Integer.MAX_VALUE)))
               ),
            description
         );
      }
   }
}
