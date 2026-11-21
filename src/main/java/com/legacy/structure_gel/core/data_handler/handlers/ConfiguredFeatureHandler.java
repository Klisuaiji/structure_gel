package com.legacy.structure_gel.core.data_handler.handlers;

import com.legacy.structure_gel.api.data_handler.handlers.DataHandler;
import com.legacy.structure_gel.api.data_handler.handlers.ProbabilityDataHandler;
import com.legacy.structure_gel.api.data_handler.parsing.DataMap;
import com.legacy.structure_gel.api.data_handler.parsing.DataParser;
import com.legacy.structure_gel.core.registry.SGRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.features.TreeFeatures;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

public class ConfiguredFeatureHandler extends ProbabilityDataHandler<ConfiguredFeatureHandler> {
   private static final DataParser PARSER = DataParser.of(SGRegistry.DataHandlerTypes.CONFIGURED_FEATURE, parser -> {
      parser.add("feature", TreeFeatures.OAK);
      parser.add("generation_chance", 1.0F, 0.0F, 1.0F);
   });
   private final ResourceKey<ConfiguredFeature<?, ?>> feature;

   public ConfiguredFeatureHandler(DataMap data) {
      super(data, "generation_chance");
      this.feature = data.get("feature", Registries.CONFIGURED_FEATURE);
   }

   public static DataParser parser() {
      return PARSER;
   }

   @Override
   protected void handle(DataHandler.Context context) {
      WorldGenLevel level = context.getLevel();
      BlockPos pos = context.getBlockPos();
      RandomSource rand = context.getRandom();
      ConfiguredFeature<?, ?> configuredFeature = (ConfiguredFeature<?, ?>)level.registryAccess()
         .registryOrThrow(Registries.CONFIGURED_FEATURE)
         .get(this.feature);
      if (configuredFeature != null) {
         configuredFeature.place(level, level.getLevel().getChunkSource().getGenerator(), rand, pos);
      }
   }
}
