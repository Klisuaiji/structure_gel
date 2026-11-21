package com.legacy.structure_gel.core.mixin.client;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.legacy.structure_gel.api.events.RegisterArmorTrimTexturesEvent;
import com.legacy.structure_gel.core.StructureGelMod;
import com.legacy.structure_gel.core.client.ClientUtil;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.renderer.texture.atlas.SpriteSourceList;
import net.minecraft.client.renderer.texture.atlas.sources.PalettedPermutations;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.armortrim.ArmorTrim;
import net.minecraft.world.item.armortrim.TrimMaterial;
import net.neoforged.fml.ModLoader;

public class ArmorTrimMixins
{
	@Mixin(SpriteSourceList.class)
	private static abstract class SpriteSourceListMixin
	{
		// Stitch modded armors onto the vanilla armor_trims.json atlas
		@Inject(at = @At("RETURN"), method = "load")
		private static void onLoadReturn(ResourceManager pResourceManager, ResourceLocation atlasLocation, CallbackInfoReturnable<SpriteSourceList> callback)
		{
			try
			{
				if (atlasLocation.equals(ResourceLocation.tryParse("armor_trims")))
				{
					StructureGelMod.LOGGER.debug("Injecting armor trims into texture atlas: " + atlasLocation);

					// Data filled into these collections from firing the event
					// Data filled into these collections from firing the event
					Set<ResourceLocation> armorPatterns = new HashSet<>();
					Set<ResourceLocation> patterns = new HashSet<>();
					Map<String, ResourceLocation> trimMaterials = new HashMap<>();
					Map<ResourceKey<TrimMaterial>, Integer> materialLight = new HashMap<>();
					RegisterArmorTrimTexturesEvent trimEvent = new RegisterArmorTrimTexturesEvent(armorPatterns, patterns, trimMaterials, materialLight);
					ModLoader.postEvent(trimEvent);
					ClientUtil.materialLight = materialLight;

					SpriteSourceList ret = callback.getReturnValue();
					for (SpriteSource source : ((SpriteSourceListMixin) (Object) ret).getSources())
					{
						if (source instanceof PalettedPermutations permutate && permutate.paletteKey.equals(ResourceLocation.tryParse("trims/color_palettes/trim_palette")))
						{
							List<ResourceLocation> patternLocations = Stream.concat(armorPatterns.stream(), armorPatterns.stream().map(rl -> rl.withSuffix("_leggings"))).map(rl -> rl.withPrefix("trims/models/armor/")).toList();
							permutate.textures = ImmutableList.<ResourceLocation>builder().addAll(permutate.textures).addAll(patternLocations).addAll(patterns).build();

							Map<String, ResourceLocation> materialLocations = trimMaterials.entrySet().stream().collect(Collectors.toMap(p -> p.getKey(), p -> p.getValue().withPrefix("trims/color_palettes/")));
							permutate.permutations = ImmutableMap.<String, ResourceLocation>builder().putAll(permutate.permutations).putAll(materialLocations).build();
						}
					}
				}

				if (atlasLocation.equals(ResourceLocation.tryParse("blocks")))
				{
					StructureGelMod.LOGGER.debug("Injecting armor trims into texture atlas: " + atlasLocation);

					// Data filled into these collections from firing the event
					Set<ResourceLocation> armorPatterns = new HashSet<>();
					Set<ResourceLocation> patterns = new HashSet<>();
					Map<String, ResourceLocation> trimMaterials = new HashMap<>();
					Map<ResourceKey<TrimMaterial>, Integer> materialLight = new HashMap<>();
					RegisterArmorTrimTexturesEvent trimEvent = new RegisterArmorTrimTexturesEvent(armorPatterns, patterns, trimMaterials, materialLight);
					ModLoader.postEvent(trimEvent);
					ClientUtil.materialLight = materialLight;

					SpriteSourceList ret = callback.getReturnValue();
					for (SpriteSource source : ((SpriteSourceListMixin) (Object) ret).getSources())
					{
						if (source instanceof PalettedPermutations permutate && permutate.paletteKey.equals(ResourceLocation.tryParse("trims/color_palettes/trim_palette")))
						{
							Map<String, ResourceLocation> materialLocations = trimMaterials.entrySet().stream().collect(Collectors.toMap(p -> p.getKey(), p -> p.getValue().withPrefix("trims/color_palettes/")));
							permutate.permutations = ImmutableMap.<String, ResourceLocation>builder().putAll(permutate.permutations).putAll(materialLocations).build();
						}
					}
				}
			}
			catch (IllegalAccessError e)
			{
				// Usually due to a mod missing a dependency and Structure Gel not loading access transformers completely
				e.printStackTrace();
			}
			catch (Exception e)
			{
				StructureGelMod.LOGGER.error(e);
				e.printStackTrace();
			}
		}

		@Accessor("sources")
		abstract List<SpriteSource> getSources();
	}

	@Mixin(HumanoidArmorLayer.class)
	private static class EquipmentLayerRendererMixin
	{
		// 1.21.1 renders trims from HumanoidArmorLayer#renderTrim instead of the
		// 1.21.4 EquipmentLayerRenderer#renderLayers pipeline.
		@ModifyVariable(
				method = "renderTrim(Lnet/minecraft/core/Holder;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/item/armortrim/ArmorTrim;Lnet/minecraft/client/model/HumanoidModel;Z)V",
				at = @At("HEAD"), argsOnly = true, ordinal = 0)
		private int modifyTrimLight(int packedLight, Holder<ArmorMaterial> material,
				com.mojang.blaze3d.vertex.PoseStack poseStack, MultiBufferSource buffers, ArmorTrim trim, HumanoidModel<?> model, boolean inner)
		{
			int customBlockLight = ClientUtil.getMaterialBrightness(trim);
			if (customBlockLight > -1)
				return LightTexture.pack(Math.max(customBlockLight, LightTexture.block(packedLight)), LightTexture.sky(packedLight));
			return packedLight;
		}
	}
}
