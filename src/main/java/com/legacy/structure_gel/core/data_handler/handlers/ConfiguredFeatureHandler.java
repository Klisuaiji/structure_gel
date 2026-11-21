package com.legacy.structure_gel.core.data_handler.handlers;

import java.util.Optional;

import com.legacy.structure_gel.api.data_handler.handlers.ProbabilityDataHandler;
import com.legacy.structure_gel.api.data_handler.parsing.DataMap;
import com.legacy.structure_gel.api.data_handler.parsing.DataParser;
import com.legacy.structure_gel.api.registry.RegistryHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.features.TreeFeatures;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

public class ConfiguredFeatureHandler extends ProbabilityDataHandler<ConfiguredFeatureHandler>
{
	public static final DataParser.Factory PARSER = DataParser.of(builder ->
	{
		builder.add("feature", TreeFeatures.OAK);
		builder.add("generation_chance", 1.0F, 0.0F, 1.0F);
	});

	private final ResourceKey<ConfiguredFeature<?, ?>> feature;

	public ConfiguredFeatureHandler(DataMap data)
	{
		super(data, "generation_chance");
		this.feature = data.get("feature", Registries.CONFIGURED_FEATURE);
	}

	@Override
	protected void handle(Context context)
	{
		WorldGenLevel level = context.getLevel();
		BlockPos pos = context.getBlockPos();
		RandomSource rand = context.getRandom();

		Optional<ConfiguredFeature<?, ?>> configuredFeature = RegistryHelper.get(level, this.feature);
		if (configuredFeature.isPresent())
		{
			configuredFeature.get().place(level, level.getLevel().getChunkSource().getGenerator(), rand, pos);
		}
	}
}
