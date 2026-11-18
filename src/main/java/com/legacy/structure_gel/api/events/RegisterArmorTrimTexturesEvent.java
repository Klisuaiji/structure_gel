package com.legacy.structure_gel.api.events;

import com.legacy.structure_gel.core.StructureGelMod;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.armortrim.TrimMaterial;
import net.minecraft.world.item.armortrim.TrimPattern;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;

public class RegisterArmorTrimTexturesEvent extends Event implements IModBusEvent {
   private static final String darkerSuffix = "_darker";
   private final Set<ResourceLocation> armorPatterns;
   private final Set<ResourceLocation> patterns;
   private final Map<String, ResourceLocation> trimMaterials;
   private final Map<ResourceKey<TrimMaterial>, Map<ArmorMaterial, String>> colorPaletteOverrides;
   private final Map<ResourceKey<TrimMaterial>, Integer> materialLight;

   @Deprecated(
      forRemoval = true
   )
   public RegisterArmorTrimTexturesEvent(
      Set<ResourceLocation> armorPatterns,
      Map<String, ResourceLocation> trimMaterials,
      Map<ResourceKey<TrimMaterial>, Map<ArmorMaterial, String>> colorPaletteOverrides,
      Map<ResourceKey<TrimMaterial>, Integer> materialLight
   ) {
      this(armorPatterns, new HashSet<>(), trimMaterials, colorPaletteOverrides, materialLight);
   }

   public RegisterArmorTrimTexturesEvent(
      Set<ResourceLocation> armorPatterns,
      Set<ResourceLocation> patterns,
      Map<String, ResourceLocation> trimMaterials,
      Map<ResourceKey<TrimMaterial>, Map<ArmorMaterial, String>> colorPaletteOverrides,
      Map<ResourceKey<TrimMaterial>, Integer> materialLight
   ) {
      this.armorPatterns = armorPatterns;
      this.patterns = patterns;
      this.trimMaterials = trimMaterials;
      this.colorPaletteOverrides = colorPaletteOverrides;
      this.materialLight = materialLight;
   }

   public void registerPatternSprite(ResourceKey<TrimPattern> trimPattern) {
      this.registerPatternSprite(trimPattern.location());
   }

   public void registerPatternSprite(ResourceLocation trimPattern) {
      if (!this.armorPatterns.add(trimPattern)) {
         StructureGelMod.LOGGER.warn("Registered an armor trim pattern sprite that overrides an existing pattern. " + trimPattern);
      }
   }

   public void registerSprite(ResourceLocation trimPattern) {
      if (!this.patterns.add(trimPattern)) {
         StructureGelMod.LOGGER.warn("Registered an armor trim pattern sprite that overrides an existing pattern. " + trimPattern);
      }
   }

   public void registerMaterialSprite(ResourceKey<TrimMaterial> materialID, boolean withDarker) {
      this.registerMaterialSprite(materialID.location().getPath(), materialID.location(), withDarker);
   }

   public void registerMaterialSprite(String assetName, ResourceLocation materialID, boolean withDarker) {
      if (this.trimMaterials.put(assetName, materialID) != null) {
         StructureGelMod.LOGGER
            .warn("Registered an armor trim material palette that overrides an existing palette. asset_name=" + assetName + ", material_id=" + materialID);
      }

      if (withDarker && this.trimMaterials.put(assetName + "_darker", materialID.withSuffix("_darker")) != null) {
         StructureGelMod.LOGGER
            .warn(
               "Registered an armor trim material palette that overrides an existing palette. asset_name="
                  + assetName
                  + "_darker, material_id="
                  + materialID.withSuffix("_darker")
            );
      }
   }

   public void registerPaletteOverride(ResourceKey<TrimMaterial> material, Map<ArmorMaterial, String> overrides) {
      if (this.colorPaletteOverrides.put(material, overrides) != null) {
         StructureGelMod.LOGGER.warn("Registered an armor trim palette override that overrides an existing override. trim_material=" + material);
      }
   }

   public void registerMaterialBrightness(ResourceKey<TrimMaterial> material, int brightness) {
      this.materialLight.put(material, brightness);
   }
}
