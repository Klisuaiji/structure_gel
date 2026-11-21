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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.legacy.structure_gel.api.events.RegisterArmorTrimTexturesEvent;
import com.legacy.structure_gel.core.StructureGelMod;
import com.legacy.structure_gel.core.client.ClientUtil;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.renderer.texture.atlas.SpriteSourceList;
import net.minecraft.client.renderer.texture.atlas.sources.PalettedPermutations;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import net.minecraft.world.item.equipment.trim.TrimMaterial;
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

	@Mixin(EquipmentLayerRenderer.class)
	private static class EquipmentLayerRendererMixin
	{
		@WrapOperation(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/Model;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;II)V"), method = "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/resources/ResourceLocation;)V")
		//@formatter:off
		private void modifyLight(
				Model target, 
				PoseStack poseStack, VertexConsumer buff, int packedLight, int packedOverlay, 
				Operation<Void> original, 
				EquipmentClientInfo.LayerType layerType, ResourceKey<EquipmentAsset> equipmentAsset, Model model, ItemStack stack)
		//@formatter:on
		{
			ArmorTrim trim = stack.get(DataComponents.TRIM);
			if (trim != null)
			{
				int customBlockLight = ClientUtil.getMaterialBrightness(trim);
				if (customBlockLight > -1)
					packedLight = LightTexture.pack(Math.max(customBlockLight, LightTexture.block(packedLight)), LightTexture.sky(packedLight));
			}
			original.call(target, poseStack, buff, packedLight, packedOverlay);
		}
	}
}
