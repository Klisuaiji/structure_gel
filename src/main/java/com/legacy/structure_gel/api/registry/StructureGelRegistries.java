package com.legacy.structure_gel.api.registry;

import com.legacy.structure_gel.api.data_handler.DataHandlerType;
import com.legacy.structure_gel.api.dynamic_spawner.DynamicSpawnerType;
import com.legacy.structure_gel.api.structure.jigsaw.JigsawCapabilityType;
import com.legacy.structure_gel.core.StructureGelMod;
import com.legacy.structure_gel.core.registry.SGRegistry;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.RegistryBuilder;

public class StructureGelRegistries {
   public static final Registry<DataHandlerType<?>> DATA_HANDLER_TYPE = new RegistryBuilder(StructureGelRegistries.Keys.DATA_HANDLER_TYPE).create();
   public static final Registry<ResourceLocation> LOOT_TABLE_ALIAS = new RegistryBuilder(StructureGelRegistries.Keys.LOOT_TABLE_ALIAS).create();
   public static final Registry<DynamicSpawnerType> DYNAMIC_SPAWNER_TYPE = new RegistryBuilder(StructureGelRegistries.Keys.DYNAMIC_SPAWNER_TYPE)
      .defaultKey(SGRegistry.DynamicSpawnerTypes.DEFAULT.getName())
      .create();
   public static final Registry<JigsawCapabilityType<?>> JIGSAW_TYPE = new RegistryBuilder(StructureGelRegistries.Keys.JIGSAW_TYPE)
      .defaultKey(SGRegistry.JigsawTypes.VANILLA.getName())
      .create();

   public static final class Keys {
      public static final ResourceKey<Registry<DataHandlerType<?>>> DATA_HANDLER_TYPE = create("data_handler_type");
      public static final ResourceKey<Registry<ResourceLocation>> LOOT_TABLE_ALIAS = create("loot_table_alias");
      public static final ResourceKey<Registry<DynamicSpawnerType>> DYNAMIC_SPAWNER_TYPE = create("dynamic_spawner_type");
      public static final ResourceKey<Registry<JigsawCapabilityType<?>>> JIGSAW_TYPE = create("jigsaw_type");

      private static <T> ResourceKey<Registry<T>> create(String key) {
         return ResourceKey.createRegistryKey(StructureGelMod.locate(key));
      }
   }
}
