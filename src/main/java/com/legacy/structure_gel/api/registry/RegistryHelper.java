package com.legacy.structure_gel.api.registry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.Holder.Reference;
import net.minecraft.core.HolderSet.Named;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;

public class RegistryHelper {
   public static <T> boolean isInTag(Registry<T> registry, TagKey<T> tagKey, T object) {
      Optional<ResourceKey<T>> key = registry.getResourceKey(object);
      if (key.isPresent()) {
         Optional<Reference<T>> holder = registry.getHolder(key.get());
         if (holder.isPresent()) {
            Optional<Named<T>> tag = registry.getTag(tagKey);
            return tag.isPresent() ? tag.get().contains((Holder)holder.get()) : false;
         }
      }

      return false;
   }

   public static <T> boolean isInTag(RegistryAccess registryAccess, ResourceKey<Registry<T>> registryKey, TagKey<T> tagKey, T object) {
      Optional<Registry<T>> registry = registryAccess.registry(registryKey);
      return registry.isPresent() ? isInTag(registry.get(), tagKey, object) : false;
   }

   @SafeVarargs
   public static StructureProcessorList combineProcessors(StructureProcessorList... structureProcessors) {
      List<StructureProcessor> processors = new ArrayList<>();

      for (StructureProcessorList spl : structureProcessors) {
         processors.addAll(spl.list());
      }

      return new StructureProcessorList(processors);
   }

   public static StructureProcessorList combineProcessors(StructureProcessorList structureProcessorList, StructureProcessor... structureProcessors) {
      List<StructureProcessor> processors = new ArrayList<>(structureProcessorList.list());
      processors.addAll(Arrays.asList(structureProcessors));
      return new StructureProcessorList(processors);
   }
}
