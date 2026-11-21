package com.legacy.structure_gel.api.data.tags;

import com.legacy.structure_gel.core.StructureGelMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.levelgen.structure.Structure;

public interface SGTags {
   private static <T> TagKey<T> create(ResourceKey<Registry<T>> registryKey, String name) {
      return TagKey.create(registryKey, StructureGelMod.locate(name));
   }

   interface StructureTags {
      TagKey<Structure> LAKE_PROOF = create("lake_proof");

      static TagKey<Structure> create(String name) {
         return SGTags.create(Registries.STRUCTURE, name);
      }
   }
}
