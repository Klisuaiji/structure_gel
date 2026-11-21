package com.legacy.structure_gel.core.structure.modifiers;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.legacy.structure_gel.api.config.StructureConfig;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.neoforged.neoforge.common.world.ModifiableStructureInfo;
import net.neoforged.neoforge.common.world.StructureModifier;

public record StructureConfigModifier(HolderLookup.RegistryLookup<Biome> biomeRegistry) implements StructureModifier
{
	public static final Map<ResourceKey<Structure>, StructureConfig> CONFIGS = new HashMap<>();

	public static final MapCodec<StructureConfigModifier> CODEC = RecordCodecBuilder.mapCodec(instance ->
	{
		return instance.group(RegistryOps.retrieveRegistryLookup(Registries.BIOME).forGetter(StructureConfigModifier::biomeRegistry)).apply(instance, StructureConfigModifier::new);
	});

	@Override
	public void modify(Holder<Structure> structure, Phase phase, ModifiableStructureInfo.StructureInfo.Builder builder)
	{
		if (phase == Phase.MODIFY)
		{
			var opKey = structure.unwrapKey();
			if (opKey.isEmpty())
				return;
			ResourceKey<Structure> key = opKey.get();
			StructureConfig config = CONFIGS.get(key);
			if (config == null)
				return;

			Optional<HolderSet<Biome>> newBiomes = config.getBiomes(biomeRegistry);
			if (newBiomes.isPresent())
				builder.getStructureSettings().setBiomes(newBiomes.get());
		}
	}

	@Override
	public MapCodec<? extends StructureModifier> codec()
	{
		return CODEC;
	}
}