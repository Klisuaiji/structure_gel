package com.legacy.structure_gel.api.structure;

import com.legacy.structure_gel.api.registry.StructureGelRegistries;
import com.legacy.structure_gel.api.structure.base.IPieceBuilderModifier;
import com.legacy.structure_gel.api.structure.jigsaw.JigsawCapability;
import com.legacy.structure_gel.core.registry.SGRegistry;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.levelgen.heightproviders.ConstantHeight;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import net.minecraft.world.level.levelgen.heightproviders.UniformHeight;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.Structure.GenerationContext;
import net.minecraft.world.level.levelgen.structure.Structure.GenerationStub;
import net.minecraft.world.level.levelgen.structure.Structure.StructureSettings;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.pools.alias.PoolAliasBinding;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;

public class ExtendedJigsawStructure extends JigsawStructure implements IPieceBuilderModifier {
   public static final Codec<ExtendedJigsawStructure> CODEC = RecordCodecBuilder.mapCodec(
         instance -> instance.group(
               settingsCodec(instance),
               StructureTemplatePool.CODEC.fieldOf("start_pool").forGetter(jigsaw -> jigsaw.startPool),
               ResourceLocation.CODEC.optionalFieldOf("start_jigsaw_name").forGetter(jigsaw -> jigsaw.startJigsawName),
               Codec.intRange(0, 32).fieldOf("size").forGetter(jigsaw -> jigsaw.maxDepth),
               HeightProvider.CODEC.fieldOf("start_height").forGetter(jigsaw -> jigsaw.startHeight),
               Codec.BOOL.fieldOf("use_expansion_hack").forGetter(jigsaw -> jigsaw.useExpansionHack),
               Types.CODEC.optionalFieldOf("project_start_to_heightmap").forGetter(jigsaw -> jigsaw.projectStartToHeightmap),
               Codec.intRange(1, 128).fieldOf("max_distance_from_center").forGetter(jigsaw -> jigsaw.maxDistanceFromCenter),
               Codec.list(PoolAliasBinding.CODEC).optionalFieldOf("pool_aliases", List.of()).forGetter(jigsaw -> jigsaw.getPoolAliases()),
               JigsawCapability.CODEC.optionalFieldOf("capability").forGetter(jigsaw -> jigsaw.capability)
            )
            .apply(instance, ExtendedJigsawStructure::new)
      )
      .flatXmap(ExtendedJigsawStructure::verifyRange, ExtendedJigsawStructure::verifyRange)
      .codec();
   protected final Optional<JigsawCapability> capability;

   protected DataResult<ExtendedJigsawStructure> verifyRange() {
      byte dist = switch (this.terrainAdaptation()) {
         case NONE -> 0;
         case BURY, BEARD_THIN, BEARD_BOX -> 12;
         default -> throw new IncompatibleClassChangeError();
      };
      return this.maxDistanceFromCenter + dist > 128
         ? DataResult.error(
            () -> "Structure size including terrain adaptation must not exceed 128. Terrain adaptation dist: " + dist + ", Size: " + this.maxDistanceFromCenter
         )
         : DataResult.success(this);
   }

   protected ExtendedJigsawStructure(
      StructureSettings settings,
      Holder<StructureTemplatePool> startPool,
      Optional<ResourceLocation> startJigsawName,
      int maxDepth,
      HeightProvider startHeight,
      boolean useExpansionHack,
      Optional<Types> projectStartToHeightmap,
      int maxDistanceFromCenter,
      List<PoolAliasBinding> poolAliases,
      Optional<JigsawCapability> capability
   ) {
      super(settings, startPool, startJigsawName, maxDepth, startHeight, useExpansionHack, projectStartToHeightmap, maxDistanceFromCenter, poolAliases);
      this.capability = capability;
   }

   public static ExtendedJigsawStructure.Builder builder(StructureSettings settings, Holder<StructureTemplatePool> startPool) {
      return new ExtendedJigsawStructure.Builder(settings, startPool);
   }

   public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
      return super.findGenerationPoint(context);
   }

   public StructureType<?> type() {
      return SGRegistry.StructureTypes.EXTENDED_JIGSAW.get();
   }

   @Override
   public void modifyPieceBuilder(StructurePiecesBuilder pieceBuilder, GenerationContext context) {
      if (this.capability.isPresent()) {
         this.capability.get().modifyPieceBuilder(pieceBuilder, context);
      }
   }

   public Optional<JigsawCapability> getCapability() {
      return this.capability;
   }

   public <T extends JigsawCapability> Optional<T> getCapability(Class<T> expectedClass) {
      return this.getCapability().map(cap -> expectedClass.isInstance(cap) ? expectedClass.cast(cap) : null);
   }

   public static class Builder {
      final StructureSettings settings;
      final Holder<StructureTemplatePool> startPool;
      Optional<ResourceLocation> startJigsawName = Optional.empty();
      int maxDepth = 7;
      HeightProvider startHeight = ConstantHeight.ZERO;
      boolean useExpansionHack = false;
      Optional<Types> projectStartToHeightmap = Optional.empty();
      int maxDistanceFromCenter = 80;
      List<PoolAliasBinding> poolAliases = new ArrayList<>();
      Optional<JigsawCapability> capability = Optional.empty();

      private Builder(StructureSettings settings, Holder<StructureTemplatePool> startPool) {
         this.settings = settings;
         this.startPool = startPool;
      }

      public ExtendedJigsawStructure.Builder startJigsaw(ResourceLocation startJigsawName) {
         this.startJigsawName = Optional.ofNullable(startJigsawName);
         return this;
      }

      public ExtendedJigsawStructure.Builder maxDepth(int maxDepth) {
         this.maxDepth = maxDepth;
         return this;
      }

      public ExtendedJigsawStructure.Builder startHeight(HeightProvider startHeight) {
         this.startHeight = startHeight;
         return this;
      }

      public ExtendedJigsawStructure.Builder startHeight(int startHeight) {
         return this.startHeight(ConstantHeight.of(VerticalAnchor.absolute(startHeight)));
      }

      public ExtendedJigsawStructure.Builder startHeight(int minHeight, int maxHeight) {
         return this.startHeight(UniformHeight.of(VerticalAnchor.absolute(minHeight), VerticalAnchor.absolute(maxHeight)));
      }

      public ExtendedJigsawStructure.Builder useExpansionHack() {
         this.useExpansionHack = true;
         return this;
      }

      public ExtendedJigsawStructure.Builder heightmap(Types heightmap) {
         this.projectStartToHeightmap = Optional.ofNullable(heightmap);
         return this;
      }

      public ExtendedJigsawStructure.Builder onSurface() {
         return this.heightmap(Types.WORLD_SURFACE_WG);
      }

      public ExtendedJigsawStructure.Builder onOceanFloor() {
         return this.heightmap(Types.OCEAN_FLOOR_WG);
      }

      public ExtendedJigsawStructure.Builder maxDistanceFromCenter(int maxDistanceFromCenter) {
         this.maxDistanceFromCenter = maxDistanceFromCenter;
         return this;
      }

      public ExtendedJigsawStructure.Builder poolAlias(PoolAliasBinding alias) {
         this.poolAliases.add(alias);
         return this;
      }

      public ExtendedJigsawStructure.Builder poolAlias(List<PoolAliasBinding> aliases) {
         this.poolAliases.addAll(aliases);
         return this;
      }

      public ExtendedJigsawStructure.Builder capability(JigsawCapability capability) {
         this.capability = Optional.ofNullable(capability);
         return this;
      }

      public ExtendedJigsawStructure build() {
         if (this.capability.isPresent() && !StructureGelRegistries.JIGSAW_TYPE.containsValue(this.capability.get().getType())) {
            throw new IllegalStateException(
               "JigsawCapabilityType not registered for " + this.capability.get() + ". Be sure to register it to StructureGelRegistries.Keys.JIGSAW_TYPE"
            );
         } else {
            return new ExtendedJigsawStructure(
               this.settings,
               this.startPool,
               this.startJigsawName,
               this.maxDepth,
               this.startHeight,
               this.useExpansionHack,
               this.projectStartToHeightmap,
               this.maxDistanceFromCenter,
               this.poolAliases,
               this.capability
            );
         }
      }
   }

   public record PlaceContext(
      Holder<StructureTemplatePool> startPool,
      Optional<ResourceLocation> startJigsawName,
      int maxDepth,
      boolean useExpansionHack,
      Optional<Types> projectStartToHeightmap,
      int maxDistanceFromCenter,
      Optional<JigsawCapability> capability
   ) {
   }
}
