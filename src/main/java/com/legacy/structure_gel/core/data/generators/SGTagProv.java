package com.legacy.structure_gel.core.data.generators;

import com.legacy.structure_gel.api.data.tags.SGTags;
import com.legacy.structure_gel.core.registry.SGRegistry;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.StructureTagsProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

public class SGTagProv {
   public static class BlockTagProv extends BlockTagsProvider {
      public BlockTagProv(PackOutput output, CompletableFuture<Provider> lookupProvider, @Nullable ExistingFileHelper existingFileHelper) {
         super(output, lookupProvider, "structure_gel", existingFileHelper);
      }

      protected void addTags(Provider prov) {
         this.tag(BlockTags.MINEABLE_WITH_PICKAXE).add((Block)SGRegistry.Blocks.DYNAMIC_SPAWNER.get());
         this.tag(BlockTags.FEATURES_CANNOT_REPLACE).add((Block)SGRegistry.Blocks.DYNAMIC_SPAWNER.get());
         this.tag(BlockTags.LAVA_POOL_STONE_CANNOT_REPLACE).add((Block)SGRegistry.Blocks.DYNAMIC_SPAWNER.get());
         this.tag(BlockTags.DRAGON_IMMUNE).add(SGRegistry.Blocks.DATA_HANDLER.get());
         this.tag(BlockTags.WITHER_IMMUNE).add(SGRegistry.Blocks.DATA_HANDLER.get());
      }
   }

   public static class StructureTagProv extends StructureTagsProvider {
      public StructureTagProv(PackOutput output, CompletableFuture<Provider> lookupProvider, @Nullable ExistingFileHelper existingFileHelper) {
         super(output, lookupProvider, "structure_gel", existingFileHelper);
      }

      protected void addTags(Provider prov) {
         this.tag(SGTags.StructureTags.LAKE_PROOF)
            .add(
               new ResourceKey[]{
                  BuiltinStructures.PILLAGER_OUTPOST,
                  BuiltinStructures.WOODLAND_MANSION,
                  BuiltinStructures.JUNGLE_TEMPLE,
                  BuiltinStructures.DESERT_PYRAMID,
                  BuiltinStructures.IGLOO,
                  BuiltinStructures.STRONGHOLD
               }
            );
      }
   }
}
