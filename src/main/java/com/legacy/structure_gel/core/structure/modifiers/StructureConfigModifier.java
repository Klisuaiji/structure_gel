package com.legacy.structure_gel.core.structure.modifiers;

import com.legacy.structure_gel.api.config.StructureConfig;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.HolderLookup.RegistryLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.neoforged.neoforge.common.world.StructureModifier;
import net.neoforged.neoforge.common.world.ModifiableStructureInfo.StructureInfo.Builder;
import net.neoforged.neoforge.common.world.StructureModifier.Phase;

public record StructureConfigModifier(RegistryLookup<Biome> biomeRegistry) implements StructureModifier {
   public static final Map<ResourceKey<Structure>, StructureConfig> CONFIGS = new HashMap<>();
   public static final Codec<StructureConfigModifier> CODEC = RecordCodecBuilder.create(
      instance -> instance.group(RegistryOps.retrieveRegistryLookup(Registries.BIOME).forGetter(StructureConfigModifier::biomeRegistry))
         .apply(instance, StructureConfigModifier::new)
   );

   public void modify(Holder<Structure> structure, Phase phase, Builder builder) {
      if (phase == Phase.MODIFY) {
         Optional<ResourceKey<Structure>> opKey = structure.unwrapKey();
         if (opKey.isEmpty()) {
            return;
         }

         ResourceKey<Structure> key = opKey.get();
         StructureConfig config = CONFIGS.get(key);
         if (config == null) {
            return;
         }

         Optional<HolderSet<Biome>> newBiomes = config.getBiomes(this.biomeRegistry);
         if (newBiomes.isPresent()) {
            builder.getStructureSettings().setBiomes(newBiomes.get());
         }
      }
   }

   public Codec<? extends StructureModifier> codec() {
      return CODEC;
   }
}
