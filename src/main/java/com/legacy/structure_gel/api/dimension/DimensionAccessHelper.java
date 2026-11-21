package com.legacy.structure_gel.api.dimension;

import com.legacy.structure_gel.core.SGAccessor;
import java.util.List;
import net.minecraft.core.HolderGetter;
import net.minecraft.data.worldgen.SurfaceRuleData;
import net.minecraft.world.level.biome.OverworldBiomeBuilder;
import net.minecraft.world.level.biome.Climate.ParameterPoint;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.SurfaceRules.RuleSource;
import net.minecraft.world.level.levelgen.synth.NormalNoise.NoiseParameters;

public class DimensionAccessHelper {
   public static NoiseSettings overworldNoise() {
      return NoiseSettings.OVERWORLD_NOISE_SETTINGS;
   }

   public static NoiseSettings netherNoise() {
      return NoiseSettings.NETHER_NOISE_SETTINGS;
   }

   public static NoiseSettings endNoise() {
      return NoiseSettings.END_NOISE_SETTINGS;
   }

   public static NoiseSettings cavesNoise() {
      return NoiseSettings.CAVES_NOISE_SETTINGS;
   }

   public static NoiseSettings floatingIslandsNoise() {
      return NoiseSettings.FLOATING_ISLANDS_NOISE_SETTINGS;
   }

   public static NoiseRouter noneNoiseRouter() {
      return SGAccessor.NOISE_ROUTER_DATA_NONE.invokeStatic();
   }

   public static NoiseRouter overworldNoiseRouter(
      HolderGetter<DensityFunction> densityFunctions, HolderGetter<NoiseParameters> noiseParameters, boolean largeBiomes, boolean isAmplified
   ) {
      return SGAccessor.NOISE_ROUTER_DATA_OVERWORLD.invokeStatic(densityFunctions, noiseParameters, largeBiomes, isAmplified);
   }

   public static NoiseRouter netherNoiseRouter(HolderGetter<DensityFunction> densityFunctions, HolderGetter<NoiseParameters> noiseParameters) {
      return SGAccessor.NOISE_ROUTER_DATA_NETHER.invokeStatic(densityFunctions, noiseParameters);
   }

   public static NoiseRouter endNoiseRouter(HolderGetter<DensityFunction> densityFunctions, HolderGetter<NoiseParameters> noiseParameters) {
      return SGAccessor.NOISE_ROUTER_DATA_END.invokeStatic(densityFunctions, noiseParameters);
   }

   public static NoiseRouter cavesNoiseRouter(HolderGetter<DensityFunction> densityFunctions, HolderGetter<NoiseParameters> noiseParameters) {
      return SGAccessor.NOISE_ROUTER_DATA_CAVES.invokeStatic(densityFunctions, noiseParameters);
   }

   public static NoiseRouter floatingIslandsNoiseRouter(HolderGetter<DensityFunction> densityFunctions, HolderGetter<NoiseParameters> noiseParameters) {
      return SGAccessor.NOISE_ROUTER_DATA_FLOATING_ISLANDS.invokeStatic(densityFunctions, noiseParameters);
   }

   public static NoiseGeneratorSettings newDimensionSettings(
      NoiseSettings noise,
      BlockState defaultBlock,
      BlockState defaultFluid,
      NoiseRouter noiseRouter,
      RuleSource surfaceRule,
      int seaLevel,
      boolean useLegacyRandomSource
   ) {
      return newDimensionSettings(noise, defaultBlock, defaultFluid, noiseRouter, surfaceRule, seaLevel, false, false, false, useLegacyRandomSource);
   }

   public static NoiseGeneratorSettings newDimensionSettings(
      NoiseSettings noise,
      BlockState defaultBlock,
      BlockState defaultFluid,
      NoiseRouter noiseRouter,
      RuleSource surfaceRule,
      int seaLevel,
      boolean disableMobGeneration,
      boolean aquifersEnabled,
      boolean oreVeinsEnabled,
      boolean useLegacyRandomSource
   ) {
      return newDimensionSettings(
         noise,
         defaultBlock,
         defaultFluid,
         noiseRouter,
         surfaceRule,
         List.of(),
         seaLevel,
         disableMobGeneration,
         aquifersEnabled,
         oreVeinsEnabled,
         useLegacyRandomSource
      );
   }

   public static NoiseGeneratorSettings newDimensionSettings(
      NoiseSettings noise,
      BlockState defaultBlock,
      BlockState defaultFluid,
      NoiseRouter noiseRouter,
      RuleSource surfaceRule,
      List<ParameterPoint> spawnTarget,
      int seaLevel,
      boolean disableMobGeneration,
      boolean aquifersEnabled,
      boolean oreVeinsEnabled,
      boolean useLegacyRandomSource
   ) {
      return new NoiseGeneratorSettings(
         noise,
         defaultBlock,
         defaultFluid,
         noiseRouter,
         surfaceRule,
         spawnTarget,
         seaLevel,
         disableMobGeneration,
         aquifersEnabled,
         oreVeinsEnabled,
         useLegacyRandomSource
      );
   }

   public static NoiseGeneratorSettings newOverworldLikeSettings(
      boolean isAmplified,
      boolean largeBiomes,
      BlockState defaultBlock,
      BlockState defaultFluid,
      HolderGetter<DensityFunction> densityFunctions,
      HolderGetter<NoiseParameters> noiseParameters
   ) {
      return new NoiseGeneratorSettings(
         NoiseSettings.OVERWORLD_NOISE_SETTINGS,
         defaultBlock,
         defaultFluid,
         overworldNoiseRouter(densityFunctions, noiseParameters, largeBiomes, isAmplified),
         SurfaceRuleData.overworld(),
         new OverworldBiomeBuilder().spawnTarget(),
         63,
         false,
         true,
         true,
         false
      );
   }

   public static NoiseGeneratorSettings newFloatingIslandSettings(
      BlockState defaultBlock, BlockState defaultFluid, HolderGetter<DensityFunction> densityFunctions, HolderGetter<NoiseParameters> noiseParameters
   ) {
      return new NoiseGeneratorSettings(
         NoiseSettings.FLOATING_ISLANDS_NOISE_SETTINGS,
         defaultBlock,
         defaultFluid,
         floatingIslandsNoiseRouter(densityFunctions, noiseParameters),
         SurfaceRuleData.overworldLike(false, false, false),
         new OverworldBiomeBuilder().spawnTarget(),
         -64,
         false,
         false,
         false,
         true
      );
   }

   public static NoiseGeneratorSettings newCavesSettings(
      BlockState defaultBlock, BlockState defaultFluid, HolderGetter<DensityFunction> densityFunctions, HolderGetter<NoiseParameters> noiseParameters
   ) {
      return new NoiseGeneratorSettings(
         NoiseSettings.CAVES_NOISE_SETTINGS,
         defaultBlock,
         defaultFluid,
         cavesNoiseRouter(densityFunctions, noiseParameters),
         SurfaceRuleData.overworldLike(false, true, true),
         new OverworldBiomeBuilder().spawnTarget(),
         32,
         false,
         false,
         false,
         true
      );
   }
}
