package com.legacy.structure_gel.api.structure.jigsaw;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.legacy.structure_gel.core.structure.jigsaw.GelSinglePoolElement;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool.Projection;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;

public class JigsawPoolBuilder {
   private final JigsawRegistryHelper jigsawRegistryHelper;
   private Map<ResourceLocation, Integer> names = ImmutableMap.of();
   private int weight = 1;
   private ResourceKey<StructureProcessorList> processors = ResourceKey.create(Registries.PROCESSOR_LIST, ResourceLocation.parse("empty"));
   private boolean maintainWater = true;

   public JigsawPoolBuilder(JigsawRegistryHelper jigsawRegistryHelper) {
      this.jigsawRegistryHelper = jigsawRegistryHelper;
   }

   public JigsawPoolBuilder names(Map<String, Integer> nameMap) {
      return this.namesR(nameMap.entrySet().stream().collect(Collectors.toMap(k -> this.jigsawRegistryHelper.locatePiece(k.getKey()), v -> v.getValue())));
   }

   public JigsawPoolBuilder namesR(Map<ResourceLocation, Integer> nameMap) {
      this.names = nameMap;
      return this;
   }

   public JigsawPoolBuilder names(Collection<String> names) {
      return this.namesR(names.stream().collect(Collectors.toMap(k -> this.jigsawRegistryHelper.locatePiece(k), v -> this.weight)));
   }

   public JigsawPoolBuilder namesR(Collection<ResourceLocation> names) {
      return this.namesR(names.stream().collect(Collectors.toMap(k -> (ResourceLocation)k, v -> this.weight)));
   }

   public JigsawPoolBuilder names(String... names) {
      return this.names(Arrays.asList(names));
   }

   public JigsawPoolBuilder namesR(ResourceLocation... names) {
      return this.namesR(Arrays.asList(names));
   }

   public JigsawPoolBuilder weight(int weight) {
      this.weight = weight;
      return this.namesR(this.names.keySet());
   }

   public JigsawPoolBuilder processors(ResourceKey<StructureProcessorList> processors) {
      this.processors = processors;
      return this;
   }

   public JigsawPoolBuilder maintainWater(boolean maintainWater) {
      this.maintainWater = maintainWater;
      return this;
   }

   public ImmutableList<Pair<Function<Projection, ? extends StructurePoolElement>, Integer>> build() {
      List<Pair<Function<Projection, ? extends StructurePoolElement>, Integer>> jigsawList = new ArrayList<>();
      this.names
         .forEach(
            (rl, i) -> jigsawList.add(
               Pair.of(createGelPiece(rl, this.jigsawRegistryHelper.processorLookup.getOrThrow(this.processors), this.maintainWater, false), i)
            )
         );
      return ImmutableList.copyOf(jigsawList);
   }

   public static ImmutableList<Pair<Function<Projection, ? extends StructurePoolElement>, Integer>> build(Map<StructurePoolElement, Integer> pieceMap) {
      List<Pair<Function<Projection, ? extends StructurePoolElement>, Integer>> jigsawList = new ArrayList<>();
      pieceMap.forEach((jp, i) -> jigsawList.add(Pair.of((Function<Projection, StructurePoolElement>)placement -> jp, i)));
      return ImmutableList.copyOf(jigsawList);
   }

   public static ImmutableList<Pair<Function<Projection, ? extends StructurePoolElement>, Integer>> build(StructurePoolElement... pieces) {
      List<Pair<Function<Projection, ? extends StructurePoolElement>, Integer>> pairs = new ArrayList<>();

      for (StructurePoolElement p : pieces) {
         pairs.add(Pair.of((Function<Projection, StructurePoolElement>)placement -> p, 1));
      }

      return ImmutableList.copyOf(pairs);
   }

   public static ImmutableList<Pair<Function<Projection, ? extends StructurePoolElement>, Integer>> collect(JigsawPoolBuilder... builders) {
      return collect(Arrays.asList(builders));
   }

   public static ImmutableList<Pair<Function<Projection, ? extends StructurePoolElement>, Integer>> collect(List<JigsawPoolBuilder> builders) {
      List<Pair<Function<Projection, ? extends StructurePoolElement>, Integer>> pairs = new ArrayList<>();

      for (JigsawPoolBuilder builder : builders) {
         pairs.addAll(builder.build());
      }

      return ImmutableList.copyOf(pairs);
   }

   public JigsawPoolBuilder clone() {
      return new JigsawPoolBuilder(this.jigsawRegistryHelper)
         .weight(this.weight)
         .namesR(this.names)
         .maintainWater(this.maintainWater)
         .processors(this.processors);
   }

   private static Function<Projection, GelSinglePoolElement> createGelPiece(
      ResourceLocation name, Holder<StructureProcessorList> processors, boolean maintainWater, boolean ignoreEntities
   ) {
      return placement -> new GelSinglePoolElement(Either.left(name), processors, placement, maintainWater, ignoreEntities);
   }
}
