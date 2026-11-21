package com.legacy.structure_gel.api.registry.registrar;

import com.legacy.structure_gel.api.data.tags.FilterHolderSet;
import com.legacy.structure_gel.api.structure.ExtendedJigsawStructure;
import com.legacy.structure_gel.api.structure.GridStructurePlacement;
import com.legacy.structure_gel.api.structure.StructureAccessHelper;
import com.legacy.structure_gel.core.registry.SGRegistry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Holder.Reference;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings.SpawnerData;
import net.minecraft.world.level.levelgen.GenerationStep.Decoration;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureSpawnOverride;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import net.minecraft.world.level.levelgen.structure.Structure.StructureSettings;
import net.minecraft.world.level.levelgen.structure.StructureSet.StructureSelectionEntry;
import net.minecraft.world.level.levelgen.structure.StructureSpawnOverride.BoundingBoxType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;

public class StructureRegistrar<S extends Structure> {
   private final ResourceLocation registryName;
   private final Supplier<StructureType<S>> structureType;
   private final Map<String, Registrar.Pointer<S>> structures;
   private final Map<String, Registrar.Static<StructurePieceType>> pieceTypes;
   private final Registrar.Pointer<StructureSet> structureSet;

   protected StructureRegistrar(StructureRegistrar.Builder<S> builder) {
      this.registryName = builder.registryName;
      String modID = this.registryName.getNamespace();
      RegistrarHandler<StructureType<?>> typeHandler = RegistrarHandler.getOrCreate(Registries.STRUCTURE_TYPE, modID);
      RegistrarHandler<StructurePieceType> pieceHandler = RegistrarHandler.getOrCreate(Registries.STRUCTURE_PIECE, modID);
      RegistrarHandler<Structure> structureHandler = RegistrarHandler.getOrCreate(Registries.STRUCTURE, modID);
      RegistrarHandler<StructureSet> setHandler = RegistrarHandler.getOrCreate(Registries.STRUCTURE_SET, modID);
      this.structureType = !builder.existingType ? typeHandler.createStatic(this.registryName, builder.type) : builder.type;
      this.pieceTypes = builder.pieceTypes
         .entrySet()
         .stream()
         .collect(Collectors.toMap(Entry::getKey, e -> pieceHandler.createStatic(createName(this.registryName, e.getKey()), e.getValue())));
      this.structures = builder.structureBuilders
         .entrySet()
         .stream()
         .collect(Collectors.toMap(Entry::getKey, e -> structureHandler.createPointer(createName(this.registryName, e.getKey()), e.getValue()::build)));
      this.structureSet = setHandler.createPointer(
         this.registryName,
         bootstrapContext -> {
            List<StructureSelectionEntry> structureWeights = new ArrayList<>();
            HolderGetter<Structure> structureRegistry = bootstrapContext.lookup(Registries.STRUCTURE);
            builder.structureBuilders.forEach((name, structureBuilder) -> {
               Optional<Reference<S>> structure = this.structures.get(name).getHolder(structureRegistry);
               if (structure.isPresent()) {
                  Holder<S> holder = (Holder<S>)structure.get();
                  structureWeights.add(StructureSet.entry(holder, structureBuilder.weight));
               }
            });
            StructurePlacement placement = builder.structurePlacement.apply(bootstrapContext);
            if (placement == null) {
               throw new NullPointerException(
                  "Cannot register "
                     + this.registryName
                     + " without having a StructurePlacement set. If this used to be a GelStructure, GridStructurePlacement replaces that system."
               );
            } else {
               return new StructureSet(structureWeights, placement);
            }
         }
      );
      builder.structureBuilders.forEach((name, structureBuilder) -> {
         Registrar.Pointer<S> registrar = this.structures.get(name);
         if (registrar != null) {
            ResourceKey<S> key = registrar.getKey();
            if (structureBuilder.dimensions != null) {
               StructureAccessHelper.addValidDimensions(key, structureBuilder.dimensions);
            }

            if (structureBuilder.lakeProof) {
               StructureAccessHelper.setLakeProof(key);
            }
         }
      });
   }

   public static <S extends Structure> StructureRegistrar.Builder<S> builder(ResourceLocation registryName, Supplier<StructureType<S>> structureType) {
      return new StructureRegistrar.Builder<>(registryName, structureType, false);
   }

   public static <S extends Structure> StructureRegistrar.Builder<S> existingTypeBuilder(
      ResourceLocation registryName, Supplier<StructureType<S>> structureType
   ) {
      return new StructureRegistrar.Builder<>(registryName, structureType, true);
   }

   public static StructureRegistrar.Builder<ExtendedJigsawStructure> jigsawBuilder(ResourceLocation registryName) {
      return existingTypeBuilder(registryName, (Supplier<StructureType<S>>)SGRegistry.StructureTypes.EXTENDED_JIGSAW);
   }

   public StructureType<S> getType() {
      return this.structureType.get();
   }

   public Map<String, Registrar.Static<StructurePieceType>> getPieceTypes() {
      return this.pieceTypes;
   }

   @Nullable
   public Registrar.Static<StructurePieceType> getPieceType(String name) {
      return this.getPieceTypes().get(name);
   }

   public Registrar.Static<StructurePieceType> getPieceType() throws IllegalArgumentException {
      if (this.getPieceTypes().size() == 1) {
         return (Registrar.Static<StructurePieceType>)this.getPieceTypes().values().toArray()[0];
      } else {
         throw new IllegalArgumentException(
            String.format(
               "The structure %s has more than one piece type associated with it. A String must be passed to define which one to get.", this.registryName
            )
         );
      }
   }

   public Map<String, Registrar.Pointer<S>> getStructures() {
      return this.structures;
   }

   @Nullable
   public Registrar.Pointer<Structure> getStructure(String name) {
      return this.getStructureExact(name);
   }

   @Nullable
   public Registrar.Pointer<Structure> getStructure() throws IllegalArgumentException {
      return this.getStructureExact();
   }

   @Nullable
   public Registrar.Pointer<S> getStructureExact(String name) {
      return this.getStructures().get(name);
   }

   @Nullable
   public Registrar.Pointer<S> getStructureExact() throws IllegalArgumentException {
      Map<String, Registrar.Pointer<S>> structures = this.getStructures();
      int size = structures.size();
      if (size == 1) {
         return (Registrar.Pointer<S>)structures.values().toArray()[0];
      } else if (size < 1) {
         return null;
      } else {
         throw new IllegalArgumentException(
            String.format(
               "The structure %s has more than one configured structure feature associated with it. A String must be passed to define which one to get.",
               this.registryName
            )
         );
      }
   }

   @Nullable
   public Registrar.Pointer<StructureSet> getStructureSet() {
      return this.structureSet;
   }

   public ResourceLocation getRegistryName() {
      return this.registryName;
   }

   private static ResourceLocation createName(ResourceLocation name, String suffix) {
      return suffix.isEmpty() ? name : ResourceLocation.fromNamespaceAndPath(name.getNamespace(), name.getPath() + "_" + suffix);
   }

   public static class Builder<S extends Structure> {
      private final ResourceLocation registryName;
      @Nullable
      private final Supplier<StructureType<S>> type;
      private final boolean existingType;
      private final Map<String, StructureRegistrar.StructureBuilder<S>> structureBuilders = new HashMap<>();
      private final Map<String, Supplier<StructurePieceType>> pieceTypes = new HashMap<>();
      private Function<BootstrapContext<?>, StructurePlacement> structurePlacement;

      private Builder(ResourceLocation registryName, Supplier<StructureType<S>> type, boolean existingType) {
         this.registryName = registryName;
         this.type = type;
         this.existingType = existingType;
         this.structurePlacement = c -> GridStructurePlacement.builder(16, 0.8F).build(this.registryName);
      }

      public StructureRegistrar.StructureBuilder<S> pushStructure(BiFunction<BootstrapContext<?>, StructureSettings, S> structureFactory) {
         return this.pushStructure("", structureFactory);
      }

      public StructureRegistrar.StructureBuilder<S> pushStructure(String name, BiFunction<BootstrapContext<?>, StructureSettings, S> structureFactory) {
         return new StructureRegistrar.StructureBuilder<>(this, structureFactory, name);
      }

      public StructureRegistrar.StructureBuilder<S> pushStructure(
         BiFunction<BootstrapContext<?>, StructureSettings, S> structureFactory, Consumer<StructureRegistrar.StructureBuilder<S>> builderConsumer
      ) {
         return this.pushStructure("", structureFactory, builderConsumer);
      }

      public StructureRegistrar.StructureBuilder<S> pushStructure(
         String name, BiFunction<BootstrapContext<?>, StructureSettings, S> structureFactory, Consumer<StructureRegistrar.StructureBuilder<S>> builderConsumer
      ) {
         StructureRegistrar.StructureBuilder<S> structureBuilder = this.pushStructure(name, structureFactory);
         builderConsumer.accept(structureBuilder);
         return structureBuilder;
      }

      public StructureRegistrar.StructureBuilder<S> pushStructure(Function<StructureSettings, S> structureFactory) {
         return this.pushStructure("", (c, s) -> structureFactory.apply(s));
      }

      public StructureRegistrar.StructureBuilder<S> pushStructure(String name, Function<StructureSettings, S> structureFactory) {
         return this.pushStructure(name, (c, s) -> structureFactory.apply(s));
      }

      public StructureRegistrar.StructureBuilder<S> pushStructure(
         Function<StructureSettings, S> structureFactory, Consumer<StructureRegistrar.StructureBuilder<S>> builderConsumer
      ) {
         return this.pushStructure("", (c, s) -> structureFactory.apply(s), builderConsumer);
      }

      public StructureRegistrar.StructureBuilder<S> pushStructure(
         String name, Function<StructureSettings, S> structureFactory, Consumer<StructureRegistrar.StructureBuilder<S>> builderConsumer
      ) {
         return this.pushStructure(name, (c, s) -> structureFactory.apply(s));
      }

      private StructureRegistrar.Builder<S> addStructureBuilder(StructureRegistrar.StructureBuilder<S> structureBuilder) throws IllegalArgumentException {
         if (this.structureBuilders.put(structureBuilder.name, structureBuilder) != null) {
            throw new IllegalArgumentException(this.errMsg("added a Structure under an existing name \"" + structureBuilder.name + "\""));
         } else {
            return this;
         }
      }

      public StructureRegistrar.Builder<S> addPiece(String name, Supplier<StructurePieceType> pieceType) throws IllegalArgumentException {
         if (this.pieceTypes.put(name, pieceType) != null) {
            throw new IllegalArgumentException(this.errMsg("added a StructurePieceType under an existing name \"" + name + "\""));
         } else {
            return this;
         }
      }

      public StructureRegistrar.Builder<S> addPiece(Supplier<StructurePieceType> pieceType) throws IllegalArgumentException {
         return this.addPiece("", pieceType);
      }

      public StructureRegistrar.Builder<S> placement(Supplier<StructurePlacement> structurePlacement) {
         return this.placement(c -> structurePlacement.get());
      }

      public StructureRegistrar.Builder<S> placement(Function<BootstrapContext<?>, StructurePlacement> structurePlacement) {
         this.structurePlacement = structurePlacement;
         return this;
      }

      public StructureRegistrar<S> build() throws IllegalStateException {
         if (this.structureBuilders.isEmpty()) {
            throw new IllegalStateException(this.errMsg("has no Structure"));
         } else {
            return new StructureRegistrar<>(this);
         }
      }

      private String errMsg(String msg) {
         return "The StructureRegistrar.Builder \"" + this.registryName + "\" " + msg;
      }
   }

   public static class StructureBuilder<S extends Structure> {
      private final StructureRegistrar.Builder<S> parentBuilder;
      private final String name;
      private final BiFunction<BootstrapContext<?>, StructureSettings, S> structureFactory;
      private Function<BootstrapContext<?>, HolderSet<Biome>> biomes = c -> c.lookup(Registries.BIOME).getOrThrow(BiomeTags.IS_OVERWORLD);
      private TerrainAdjustment terrainAdjustment = TerrainAdjustment.NONE;
      private Decoration generationStep = Decoration.SURFACE_STRUCTURES;
      private Map<MobCategory, Supplier<StructureSpawnOverride>> spawns = new HashMap<>();
      private int weight = 1;
      @Nullable
      private Collection<ResourceKey<Level>> dimensions = null;
      private boolean lakeProof = true;

      private StructureBuilder(StructureRegistrar.Builder<S> parentBuilder, BiFunction<BootstrapContext<?>, StructureSettings, S> structureFactory, String name) {
         this.parentBuilder = parentBuilder;
         this.structureFactory = structureFactory;
         this.name = name;
      }

      public final StructureRegistrar.StructureBuilder<S> biomes(Function<BootstrapContext<?>, HolderSet<Biome>> biomes) {
         this.biomes = biomes;
         return this;
      }

      public final StructureRegistrar.StructureBuilder<S> biomes(Supplier<FilterHolderSet.Builder<Biome>> biomes) {
         return this.biomes(c -> biomes.get().build(c.lookup(Registries.BIOME)));
      }

      @SafeVarargs
      public final StructureRegistrar.StructureBuilder<S> biomes(ResourceKey<Biome>... biomes) {
         return this.biomes(c -> HolderSet.direct(Arrays.stream(biomes).map(c.lookup(Registries.BIOME)::getOrThrow).toList()));
      }

      public final StructureRegistrar.StructureBuilder<S> biomes(TagKey<Biome> tagKey) {
         return this.biomes(c -> c.lookup(Registries.BIOME).getOrThrow(tagKey));
      }

      public final StructureRegistrar.StructureBuilder<S> spawns(MobCategory category, Supplier<StructureSpawnOverride> spawnPool) {
         this.spawns.put(category, spawnPool);
         return this;
      }

      public final StructureRegistrar.StructureBuilder<S> spawns(MobCategory category, BoundingBoxType boundingBoxType, Supplier<List<SpawnerData>> spawns) {
         return this.spawns(category, () -> new StructureSpawnOverride(boundingBoxType, WeightedRandomList.create(spawns.get())));
      }

      public final StructureRegistrar.StructureBuilder<S> noSpawns(BoundingBoxType boundingBoxType, MobCategory... categories) {
         for (MobCategory category : categories.length == 0 ? MobCategory.values() : categories) {
            this.spawns(category, boundingBoxType, () -> Collections.emptyList());
         }

         return this;
      }

      public final StructureRegistrar.StructureBuilder<S> terrainAdjustment(TerrainAdjustment terrainAdjustment) {
         this.terrainAdjustment = terrainAdjustment;
         return this;
      }

      public final StructureRegistrar.StructureBuilder<S> generationStep(Decoration generationStep) {
         this.generationStep = generationStep;
         return this;
      }

      public final StructureRegistrar.StructureBuilder<S> weight(int weight) {
         this.weight = weight;
         return this;
      }

      @SafeVarargs
      public final StructureRegistrar.StructureBuilder<S> dimensions(ResourceKey<Level>... dimensions) {
         this.dimensions = List.of(dimensions);
         return this;
      }

      public final StructureRegistrar.StructureBuilder<S> lakeProof(boolean lakeProof) {
         this.lakeProof = lakeProof;
         return this;
      }

      public final StructureRegistrar.Builder<S> popStructure() throws IllegalStateException {
         this.parentBuilder.addStructureBuilder(this);
         return this.parentBuilder;
      }

      private S build(BootstrapContext<?> context) {
         return this.structureFactory
            .apply(
               context,
               new StructureSettings(
                  this.biomes.apply(context),
                  this.spawns
                     .entrySet()
                     .stream()
                     .filter(e -> e.getValue().get() != null)
                     .collect(Collectors.toMap(Entry::getKey, e -> (StructureSpawnOverride)((Supplier)e.getValue()).get())),
                  this.generationStep,
                  this.terrainAdjustment
               )
            );
      }
   }
}
