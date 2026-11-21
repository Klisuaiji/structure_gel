package com.legacy.structure_gel.api.registry.registrar;

import com.legacy.structure_gel.core.util.Internal;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;

public class DimensionRegistrar {
   @Internal
   public static final List<DimensionRegistrar> REGISTRARS = new ArrayList<>();
   private final Registrar.Pointer<DimensionType> dimensionType;
   private final Registrar.Pointer<NoiseGeneratorSettings> noiseSettings;
   private final Registrar.Pointer<LevelStem> levelStem;
   private final ResourceKey<Level> levelKey;

   public DimensionRegistrar(
      ResourceLocation registryName,
      Function<BootstrapContext<?>, DimensionType> dimensionType,
      Function<BootstrapContext<?>, NoiseGeneratorSettings> noiseSettings,
      Function<BootstrapContext<?>, ChunkGenerator> chunkGeneratorFactory
   ) {
      String modID = registryName.getNamespace();
      RegistrarHandler<DimensionType> dimTypeHandler = RegistrarHandler.getOrCreate(Registries.DIMENSION_TYPE, modID);
      RegistrarHandler<NoiseGeneratorSettings> noiseSettingsHandler = RegistrarHandler.getOrCreate(Registries.NOISE_SETTINGS, modID);
      RegistrarHandler<LevelStem> levelStemHandler = RegistrarHandler.getOrCreate(Registries.LEVEL_STEM, modID);
      this.dimensionType = dimTypeHandler.createPointer(registryName, dimensionType);
      this.noiseSettings = noiseSettingsHandler.createPointer(registryName, noiseSettings);
      this.levelStem = levelStemHandler.createPointer(
         registryName, c -> new LevelStem((Holder)this.dimensionType.getHolder(c).get(), chunkGeneratorFactory.apply(c))
      );
      this.levelKey = ResourceKey.create(Registries.DIMENSION, registryName);
      REGISTRARS.add(this);
   }

   public Registrar.Pointer<DimensionType> getType() {
      return this.dimensionType;
   }

   public Registrar.Pointer<NoiseGeneratorSettings> getNoiseSettings() {
      return this.noiseSettings;
   }

   public Registrar.Pointer<LevelStem> getLevelStem() {
      return this.levelStem;
   }

   public ResourceKey<Level> getLevelKey() {
      return this.levelKey;
   }
}
