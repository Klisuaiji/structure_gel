package com.legacy.structure_gel.api.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.legacy.structure_gel.api.data.tags.FilterHolderSet;
import com.legacy.structure_gel.api.registry.registrar.Registrar;
import com.legacy.structure_gel.core.StructureGelMod;
import com.legacy.structure_gel.core.structure.modifiers.StructureConfigModifier;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Enables modifying basic data about a structure through config files
 * 
 * @author Silver_David
 *
 */
public class StructureConfig
{
	private final ResourceKey<Structure> structure;
	private final Optional<ModConfigSpec.ConfigValue<List<? extends String>>> biomeFilter;

	private StructureConfig(ResourceKey<Structure> structure, Optional<ModConfigSpec.ConfigValue<List<? extends String>>> biomeFilter)
	{
		this.structure = structure;
		this.biomeFilter = biomeFilter;
	}

	public static Builder builder(ResourceKey<Structure> structure)
	{
		return new Builder(structure);
	}

	public static Builder builder(Registrar<Structure> structure)
	{
		return builder(structure.getKey());
	}

	public ResourceKey<Structure> getStructure()
	{
		return this.structure;
	}

	/**
	 * 
	 * @param biomeRegistry
	 *            In general, this should be a HolderLookup.RegistryLookup to
	 *            prevent crashing on values not currently in registry
	 */
	public Optional<HolderSet<Biome>> getBiomes(HolderGetter<Biome> biomeRegistry)
	{
		if (biomeFilter.isPresent())
		{
			FilterHolderSet.Builder<Biome> builder = FilterHolderSet.builder(Registries.BIOME);
			List<? extends String> biomeStrings = biomeFilter.get().get();
			if (biomeStrings.isEmpty()) // Do nothing if no data is present.
				return Optional.empty();
			for (String s : biomeStrings)
			{
				boolean whitelist = true;
				boolean isTag = false;

				if (s.startsWith("#!")) // Allow it to work, but fix the order of things for logic checks
					s = s.replaceFirst("#!", "!#");

				if (s.startsWith("!"))
				{
					whitelist = false;
					s = s.replaceFirst("!", "");
				}
				if (s.startsWith("#"))
				{
					isTag = true;
					s = s.replaceFirst("#", "");
				}

				ResourceLocation rl = ResourceLocation.tryParse(s); // This should've been validated earlier
				if (isTag)
					builder.tags(whitelist, TagKey.create(Registries.BIOME, rl));
				else
					builder.values(whitelist, ResourceKey.create(Registries.BIOME, rl));
			}
			return Optional.of(builder.build(biomeRegistry));
		}
		return Optional.empty();
	}

	public static class Builder
	{
		private final ResourceKey<Structure> structure;
		private boolean hasBiomes = false;
		private List<String> biomeFilterStrings = new ArrayList<>();

		private Builder(ResourceKey<Structure> structure)
		{
			this.structure = structure;
		}

		/**
		 * Moves the current builder into the biome filter portion. Biomes are an opt-in
		 * system. Leaving the config blank will use the structure's default biomes.
		 */
		public BiomeConfigBuilder pushBiomes()
		{
			this.hasBiomes = true;
			return new BiomeConfigBuilder();
		}

		public Builder biomes(String... biomeStrings)
		{
			for (var s : biomeStrings)
				this.biomeFilterStrings.add(s);
			return this;
		}

		public StructureConfig build(ModConfigSpec.Builder configBuilder)
		{
			configBuilder.push(structure.location().getPath());

			Optional<ModConfigSpec.ConfigValue<List<? extends String>>> biomeConfig;
			if (this.hasBiomes)
				biomeConfig = Optional.of(ConfigBuilder.makeList(configBuilder, structure.location().toString(), "The biomes this structure should generate in. Use # to define a tag. Use ! to define a blacklist. If no values are set, the structure's default biomes will be used.", List.of("minecraft:plains, #minecraft:is_forest, !minecraft:birch_forest, !#minecraft:has_structure/woodland_mansion"), this.biomeFilterStrings, s -> s == null ? false : validateBiomeString(s.toString())));
			else
				biomeConfig = Optional.empty();

			StructureConfig ret = new StructureConfig(structure, biomeConfig);
			if (StructureConfigModifier.CONFIGS.put(structure, ret) != null)
				throw new IllegalStateException("Cannot assign multiple configs to the same structure. Structure: " + this.structure.location());

			configBuilder.pop();

			return ret;
		}

		private boolean validateBiomeString(String s)
		{
			String original = s;
			if (original.startsWith("#!")) // Allow it to work, but fix the order of things for logic checks
				s = s.replaceFirst("#!", "!#");

			// Remove different tags for resource location check
			if (s.startsWith("!"))
				s = s.replaceFirst("!", "");
			if (s.startsWith("#"))
				s = s.replaceFirst("#", "");
			if (ResourceLocation.tryParse(s) == null)
				return logBiomeErr(original, s + " is not a valid resource location. It contains invalid characters.");

			return true;
		}

		private boolean logBiomeErr(String incorrectString, String reason)
		{
			StructureGelMod.LOGGER.error("The biome config for structure {} contains an invalid string \"{}\". {}", structure.location().toString(), incorrectString, reason);
			return false;
		}

		public class BiomeConfigBuilder
		{
			public BiomeConfigBuilder biomes(String... filterStrings)
			{
				Builder.this.biomes(filterStrings);
				return this;
			}

			@SafeVarargs
			public final BiomeConfigBuilder whitelist(ResourceKey<Biome>... biomes)
			{
				for (var s : biomes)
					Builder.this.biomeFilterStrings.add(s.location().toString());
				return this;
			}

			@SafeVarargs
			public final BiomeConfigBuilder whitelist(Registrar<Biome>... biomes)
			{
				for (var s : biomes)
					Builder.this.biomeFilterStrings.add(s.getKey().location().toString());
				return this;
			}

			@SafeVarargs
			public final BiomeConfigBuilder whitelist(TagKey<Biome>... biomes)
			{
				for (var s : biomes)
					Builder.this.biomeFilterStrings.add("#" + s.location().toString());
				return this;
			}

			@SafeVarargs
			public final BiomeConfigBuilder blacklist(ResourceKey<Biome>... biomes)
			{
				for (var s : biomes)
					Builder.this.biomeFilterStrings.add("!" + s.location().toString());
				return this;
			}

			@SafeVarargs
			public final BiomeConfigBuilder blacklist(Registrar<Biome>... biomes)
			{
				for (var s : biomes)
					Builder.this.biomeFilterStrings.add("!" + s.getKey().location().toString());
				return this;
			}

			@SafeVarargs
			public final BiomeConfigBuilder blacklist(TagKey<Biome>... biomes)
			{
				for (var s : biomes)
					Builder.this.biomeFilterStrings.add("!#" + s.location().toString());
				return this;
			}

			public Builder popBiomes()
			{
				return Builder.this;
			}
		}
	}
}
