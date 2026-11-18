package com.legacy.structure_gel.api.structure.jigsaw;

import com.legacy.structure_gel.core.StructureGelMod;
import com.mojang.datafixers.util.Pair;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.pools.FeaturePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.ListPoolElement;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;

public class JigsawAccessHelper {
   public static void addToPool(Registry<StructureTemplatePool> registry, ResourceLocation pool, List<Pair<StructurePoolElement, Integer>> pieces) {
      modifyJigsawPattern(registry, pool, template -> {
         for (Pair<StructurePoolElement, Integer> pair : pieces) {
            for (int i = 0; i < pair.getSecond(); i++) {
               template.templates.add((StructurePoolElement)pair.getFirst());
            }
         }
      });
   }

   public static void modifyJigsawPattern(Registry<StructureTemplatePool> registry, ResourceLocation key, Consumer<StructureTemplatePool> action) {
      registry.getOptional(key)
         .ifPresentOrElse(action, () -> StructureGelMod.LOGGER.error("Could not perform an action on {} because it does not exist.", key));
   }

   public static ResourceLocation getSingleJigsawPieceLocation(SinglePoolElement element) {
      return (ResourceLocation)element.template.left().get();
   }

   public static List<StructurePoolElement> getListJigsawPiecePieces(ListPoolElement element) {
      return element.elements;
   }

   public static PlacedFeature getFeatureJigsawPieceFeatures(FeaturePoolElement element) {
      return (PlacedFeature)element.feature.value();
   }
}
