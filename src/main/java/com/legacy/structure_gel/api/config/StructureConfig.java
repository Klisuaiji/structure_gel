package com.legacy.structure_gel.api.config;

import com.legacy.blue_skies.compat.*;
import com.legacy.structure_gel.api.data.tags.FilterHolderSet;
import com.legacy.structure_gel.api.registry.registrar.Registrar;
import com.legacy.structure_gel.core.StructureGelMod;
import com.legacy.structure_gel.core.structure.modifiers.StructureConfigModifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;

public class StructureConfig {
   private final ResourceKey<Structure> structure;
   private final Optional<ConfigValue<List<? extends String>>> biomeFilter;

   private StructureConfig(ResourceKey<Structure> structure, Optional<ConfigValue<List<? extends String>>> biomeFilter) {
      this.structure = structure;
      this.biomeFilter = biomeFilter;
   }

   public static StructureConfig.Builder builder(ResourceKey<Structure> structure) {
      return new StructureConfig.Builder(structure);
   }

   public static StructureConfig.Builder builder(Registrar<Structure> structure) {
      return builder(structure.getKey());
   }

   public ResourceKey<Structure> getStructure() {
      return this.structure;
   }

   public Optional<HolderSet<Biome>> getBiomes(HolderGetter<Biome> biomeRegistry) {
      if (this.biomeFilter.isPresent()) {
         FilterHolderSet.Builder<Biome> builder = FilterHolderSet.builder(Registries.BIOME);
         List<? extends String> biomeStrings = (List<? extends String>)this.biomeFilter.get().get();
         if (biomeStrings.isEmpty()) {
            return Optional.empty();
         }

         for (String s : biomeStrings) {
            boolean whitelist = true;
            boolean isTag = false;
            if (s.startsWith("#!")) {
               s = s.replaceFirst("#!", "!#");
            }

            if (s.startsWith("!")) {
               whitelist = false;
               s = s.replaceFirst("!", "");
            }

            if (s.startsWith("#")) {
               isTag = true;
               s = s.replaceFirst("#", "");
            }

            ResourceLocation rl = ResourceLocation.parse(s);
            if (isTag) {
               builder.tags(whitelist, TagKey.create(Registries.BIOME, rl));
            } else {
               builder.values(whitelist, ResourceKey.create(Registries.BIOME, rl));
            }
         }

         return Optional.of(builder.build(biomeRegistry));
      } else {
         return Optional.empty();
      }
   }

   public static class Builder {
      private final ResourceKey<Structure> structure;
      private boolean hasBiomes = false;
      private List<String> biomeFilterStrings = new ArrayList<>();

      private Builder(ResourceKey<Structure> structure) {
         this.structure = structure;
      }

      public StructureConfig.Builder.BiomeConfigBuilder pushBiomes() {
         this.hasBiomes = true;
         return new StructureConfig.Builder.BiomeConfigBuilder();
      }

      public StructureConfig.Builder biomes(String... biomeStrings) {
         for (String s : biomeStrings) {
            this.biomeFilterStrings.add(s);
         }

         return this;
      }

      public StructureConfig build(net.neoforged.neoforge.common.ModConfigSpec.Builder configBuilder) {
         configBuilder.push(this.structure.location().getPath());
         Optional<ConfigValue<List<? extends String>>> biomeConfig;
         if (this.hasBiomes) {
            biomeConfig = Optional.of(
               ConfigBuilder.makeList(
                  configBuilder,
                  this.structure.location().toString(),
                  "The biomes this structure should generate in. Use # to define a tag. Use ! to define a blacklist. If no values are set, the structure's default biomes will be used.",
                  List.of("minecraft:plains, #minecraft:is_forest, !minecraft:birch_forest, !#minecraft:has_structure/woodland_mansion"),
                  this.biomeFilterStrings,
                  s -> s == null ? false : this.validateBiomeString(s.toString())
               )
            );
         } else {
            biomeConfig = Optional.empty();
         }

         StructureConfig ret = new StructureConfig(this.structure, biomeConfig);
         if (StructureConfigModifier.CONFIGS.put(this.structure, ret) != null) {
            throw new IllegalStateException("Cannot assign multiple configs to the same structure. Structure: " + this.structure.location());
         }

         configBuilder.pop();
         return ret;
      }

      private boolean validateBiomeString(String s) {
         String original = s;
         if (original.startsWith("#!")) {
            s = s.replaceFirst("#!", "!#");
         }

         if (s.startsWith("!")) {
            s = s.replaceFirst("!", "");
         }

         if (s.startsWith("#")) {
            s = s.replaceFirst("#", "");
         }

         return !LegacyRL.isValid(s)
            ? this.logBiomeErr(original, s + " is not a valid resource location. It contains invalid characters.")
            : true;
      }

      private boolean logBiomeErr(String incorrectString, String reason) {
         StructureGelMod.LOGGER
            .error("The biome config for structure {} contains an invalid string \"{}\". {}", this.structure.location().toString(), incorrectString, reason);
         return false;
      }

      public class BiomeConfigBuilder {
         public StructureConfig.Builder.BiomeConfigBuilder biomes(String... filterStrings) {
            Builder.this.biomes(filterStrings);
            return this;
         }

         @SafeVarargs
         public final StructureConfig.Builder.BiomeConfigBuilder whitelist(ResourceKey<Biome>... biomes) {
            for (ResourceKey<Biome> s : biomes) {
               Builder.this.biomeFilterStrings.add(s.location().toString());
            }

            return this;
         }

         @SafeVarargs
         public final StructureConfig.Builder.BiomeConfigBuilder whitelist(Registrar<Biome>... biomes) {
            for (Registrar<Biome> s : biomes) {
               Builder.this.biomeFilterStrings.add(s.getKey().location().toString());
            }

            return this;
         }

         @SafeVarargs
         public final StructureConfig.Builder.BiomeConfigBuilder whitelist(TagKey<Biome>... biomes) {
            for (TagKey<Biome> s : biomes) {
               Builder.this.biomeFilterStrings.add("#" + s.location().toString());
            }

            return this;
         }

         @SafeVarargs
         public final StructureConfig.Builder.BiomeConfigBuilder blacklist(ResourceKey<Biome>... biomes) {
            for (ResourceKey<Biome> s : biomes) {
               Builder.this.biomeFilterStrings.add("!" + s.location().toString());
            }

            return this;
         }

         @SafeVarargs
         public final StructureConfig.Builder.BiomeConfigBuilder blacklist(Registrar<Biome>... biomes) {
            for (Registrar<Biome> s : biomes) {
               Builder.this.biomeFilterStrings.add("!" + s.getKey().location().toString());
            }

            return this;
         }

         @SafeVarargs
         public final StructureConfig.Builder.BiomeConfigBuilder blacklist(TagKey<Biome>... biomes) {
            for (TagKey<Biome> s : biomes) {
               Builder.this.biomeFilterStrings.add("!#" + s.location().toString());
            }

            return this;
         }

         public StructureConfig.Builder popBiomes() {
            return Builder.this;
         }
      }
   }
}
