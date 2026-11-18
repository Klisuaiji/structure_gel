package com.legacy.structure_gel.core.mixin.client;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.legacy.structure_gel.api.events.RegisterArmorTrimTexturesEvent;
import com.legacy.structure_gel.core.StructureGelMod;
import com.legacy.structure_gel.core.client.ClientUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.renderer.texture.atlas.SpriteSourceList;
import net.minecraft.client.renderer.texture.atlas.sources.PalettedPermutations;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.armortrim.ArmorTrim;
import net.minecraft.world.item.armortrim.TrimMaterial;
import net.neoforged.fml.ModLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

public class ArmorTrimMixins {
   @Mixin({ArmorTrim.class})
   private static class ArmorTrimMixin {
      @Inject(
         at = {@At("HEAD")},
         method = {"getColorPaletteSuffix"},
         cancellable = true
      )
      private void getCustomPaletteSuffix(ArmorMaterial armorMaterial, CallbackInfoReturnable<String> callback) {
         String override = ClientUtil.getCustomColorPaletteSuffix((ArmorTrim)this, armorMaterial);
         if (override != null) {
            callback.setReturnValue(override);
         }
      }
   }

   @Mixin({HumanoidArmorLayer.class})
   private static class HumanoidArmorLayerMixin {
      @ModifyVariable(
         at = @At("HEAD"),
         ordinal = 0,
         method = {"renderTrim(Lnet/minecraft/world/item/ArmorMaterial;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/item/armortrim/ArmorTrim;Lnet/minecraft/client/model/Model;Z)V"},
         remap = false
      )
      private int modifyLightForgeMethod(
         int original, ArmorMaterial pArmorMaterial, PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight, ArmorTrim pTrim
      ) {
         int customBlockLight = ClientUtil.getMaterialBrightness(pTrim);
         return customBlockLight > -1 ? LightTexture.pack(Math.max(customBlockLight, LightTexture.block(original)), LightTexture.sky(original)) : original;
      }

      @ModifyVariable(
         at = @At("HEAD"),
         ordinal = 0,
         method = {"renderTrim(Lnet/minecraft/world/item/ArmorMaterial;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/item/armortrim/ArmorTrim;Lnet/minecraft/client/model/HumanoidModel;Z)V"}
      )
      private int modifyLightVanillaMethod(
         int original, ArmorMaterial pArmorMaterial, PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight, ArmorTrim pTrim
      ) {
         int customBlockLight = ClientUtil.getMaterialBrightness(pTrim);
         return customBlockLight > -1 ? LightTexture.pack(Math.max(customBlockLight, LightTexture.block(original)), LightTexture.sky(original)) : original;
      }
   }

   @Mixin({SpriteSourceList.class})
   private abstract static class SpriteSourceListMixin {
      @Inject(
         at = {@At("RETURN")},
         method = {"load"}
      )
      private static void onLoadReturn(ResourceManager pResourceManager, ResourceLocation atlasLocation, CallbackInfoReturnable<SpriteSourceList> callback) {
         try {
            if (atlasLocation.equals(new ResourceLocation("armor_trims"))) {
               StructureGelMod.LOGGER.debug("Injecting armor trims into texture atlas: " + atlasLocation);
               Set<ResourceLocation> armorPatterns = new HashSet<>();
               Set<ResourceLocation> patterns = new HashSet<>();
               Map<String, ResourceLocation> trimMaterials = new HashMap<>();
               Map<ResourceKey<TrimMaterial>, Map<ArmorMaterial, String>> colorPaletteOverrides = new HashMap<>();
               Map<ResourceKey<TrimMaterial>, Integer> materialLight = new HashMap<>();
               RegisterArmorTrimTexturesEvent trimEvent = new RegisterArmorTrimTexturesEvent(
                  armorPatterns, patterns, trimMaterials, colorPaletteOverrides, materialLight
               );
               ModLoader.get().postEvent(trimEvent);
               ClientUtil.colorPaletteOverrides = colorPaletteOverrides;
               ClientUtil.materialLight = materialLight;
               SpriteSourceList ret = (SpriteSourceList)callback.getReturnValue();

               for (SpriteSource source : ((ArmorTrimMixins.SpriteSourceListMixin)ret).getSources()) {
                  if (source instanceof PalettedPermutations permutate
                     && permutate.paletteKey.equals(new ResourceLocation("trims/color_palettes/trim_palette"))) {
                     List<ResourceLocation> patternLocations = Stream.concat(
                           armorPatterns.stream(), armorPatterns.stream().map(rl -> rl.withSuffix("_leggings"))
                        )
                        .map(rl -> rl.withPrefix("trims/models/armor/"))
                        .toList();
                     permutate.textures = ImmutableList.builder().addAll(permutate.textures).addAll(patternLocations).addAll(patterns).build();
                     Map<String, ResourceLocation> materialLocations = trimMaterials.entrySet()
                        .stream()
                        .collect(Collectors.toMap(p -> p.getKey(), p -> p.getValue().withPrefix("trims/color_palettes/")));
                     permutate.permutations = ImmutableMap.builder().putAll(permutate.permutations).putAll(materialLocations).build();
                  }
               }
            }

            if (atlasLocation.equals(new ResourceLocation("blocks"))) {
               StructureGelMod.LOGGER.debug("Injecting armor trims into texture atlas: " + atlasLocation);
               Set<ResourceLocation> armorPatterns = new HashSet<>();
               Set<ResourceLocation> patterns = new HashSet<>();
               Map<String, ResourceLocation> trimMaterials = new HashMap<>();
               Map<ResourceKey<TrimMaterial>, Map<ArmorMaterial, String>> colorPaletteOverrides = new HashMap<>();
               Map<ResourceKey<TrimMaterial>, Integer> materialLight = new HashMap<>();
               RegisterArmorTrimTexturesEvent trimEvent = new RegisterArmorTrimTexturesEvent(
                  armorPatterns, patterns, trimMaterials, colorPaletteOverrides, materialLight
               );
               ModLoader.get().postEvent(trimEvent);
               ClientUtil.colorPaletteOverrides = colorPaletteOverrides;
               ClientUtil.materialLight = materialLight;
               SpriteSourceList ret = (SpriteSourceList)callback.getReturnValue();

               for (SpriteSource sourcex : ((ArmorTrimMixins.SpriteSourceListMixin)ret).getSources()) {
                  if (sourcex instanceof PalettedPermutations permutate
                     && permutate.paletteKey.equals(new ResourceLocation("trims/color_palettes/trim_palette"))) {
                     Map<String, ResourceLocation> materialLocations = trimMaterials.entrySet()
                        .stream()
                        .collect(Collectors.toMap(p -> p.getKey(), p -> p.getValue().withPrefix("trims/color_palettes/")));
                     permutate.permutations = ImmutableMap.builder().putAll(permutate.permutations).putAll(materialLocations).build();
                  }
               }
            }
         } catch (IllegalAccessError var15) {
            var15.printStackTrace();
         } catch (Exception var16) {
            StructureGelMod.LOGGER.error(var16);
            var16.printStackTrace();
         }
      }

      @Accessor("sources")
      abstract List<SpriteSource> getSources();
   }
}
