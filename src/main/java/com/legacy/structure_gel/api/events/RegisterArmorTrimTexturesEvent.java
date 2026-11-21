package com.legacy.structure_gel.api.events;

import java.util.Map;
import java.util.Set;

import com.legacy.structure_gel.core.StructureGelMod;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.armortrim.TrimMaterial;
import net.minecraft.world.item.armortrim.TrimPattern;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;

/**
 * Runs when the texture atlas for {@code armor_trims.json} is loaded, before
 * the textures are stitched.
 * <p>
 * This event is fired on the Mod bus {@link IModBusEvent} for
 * {@link Dist#CLIENT}
 * 
 * @author Silver_David
 *
 */
public class RegisterArmorTrimTexturesEvent extends Event implements IModBusEvent
{
	private static final String darkerSuffix = "_darker";
	private final Set<ResourceLocation> armorPatterns;
	private final Set<ResourceLocation> patterns;
	private final Map<String, ResourceLocation> trimMaterials;
	private final Map<ResourceKey<TrimMaterial>, Integer> materialLight;

	public RegisterArmorTrimTexturesEvent(Set<ResourceLocation> armorPatterns, Set<ResourceLocation> patterns, Map<String, ResourceLocation> trimMaterials, Map<ResourceKey<TrimMaterial>, Integer> materialLight)
	{
		this.armorPatterns = armorPatterns;
		this.patterns = patterns;
		this.trimMaterials = trimMaterials;
		this.materialLight = materialLight;
	}

	/**
	 * Registers a trim pattern to create texture locations when the
	 * {@code armor_trims.json} atlas is created. This expects that you follow the
	 * same naming conventions as vanilla:<br>
	 * {@code mod:textures/trims/models/armor/trimPattern.png}<br>
	 * {@code mod:textures/trims/models/armor/trimPattern_leggings}.png
	 * 
	 * @param trimPattern
	 *            The trim pattern to generate textures for. This assumes the
	 *            asset_id is also the registry name.
	 */
	public void registerPatternSprite(ResourceKey<TrimPattern> trimPattern)
	{
		this.registerPatternSprite(trimPattern.location());
	}

	/**
	 * Registers a trim pattern to create texture locations when the
	 * {@code armor_trims.json} atlas is created. This expects that you follow the
	 * same naming conventions as vanilla:<br>
	 * {@code mod:textures/trims/models/armor/trimPattern.png}<br>
	 * {@code mod:textures/trims/models/armor/trimPattern_leggings}.png
	 * 
	 * @param trimPattern
	 *            The pattern to generate textures for.
	 */
	public void registerPatternSprite(ResourceLocation trimPattern)
	{
		if (!this.armorPatterns.add(trimPattern))
			StructureGelMod.LOGGER.warn("Registered an armor trim pattern sprite that overrides an existing pattern. " + trimPattern);
	}
	
	/**
	 * Registers the passed texture as a trim pattern. No modification is done to
	 * the value passed in.
	 * 
	 * @param trimPattern
	 */
	public void registerSprite(ResourceLocation trimPattern)
	{
		if (!this.patterns.add(trimPattern))
			StructureGelMod.LOGGER.warn("Registered an armor trim pattern sprite that overrides an existing pattern. " + trimPattern);
	}

	/**
	 * Registers a trim material to create a color palette when the
	 * {@code armor_trims.json} atlas is created. This expects that you follow the
	 * same naming conventions as vanilla:<br>
	 * {@code mod:textures/trims/color_palettes/materialID.png}
	 * 
	 * @param materialID
	 *            The material to create a color palette from. This assumes the
	 *            asset_name is also the registry name.
	 * @param withDarker
	 *            Registers a material palette with the "_darker" suffix. Used when
	 *            the trim material matches the armor's material.
	 */
	public void registerMaterialSprite(ResourceKey<TrimMaterial> materialID, boolean withDarker)
	{
		this.registerMaterialSprite(materialID.location().getPath(), materialID.location(), withDarker);
	}

	/**
	 * Registers a trim material to create a color palette when the armor_trims.json
	 * atlas is created. This expects that you follow the same naming conventions as
	 * vanilla:<br>
	 * {@code mod:textures/trims/color_palettes/materialID.png}
	 * 
	 * @param assetName
	 *            The name generated textures will have appended to them. This name
	 *            will be referenced by the TrimMaterial's asset_name field.
	 *            Including your mod ID in this will help prevent unintended
	 *            overrides.
	 * @param materialID
	 *            The material to create a color palette from
	 * @param withDarker
	 *            Registers a material palette with the "_darker" suffix. Used when
	 *            the trim material matches the armor's material.
	 */
	public void registerMaterialSprite(String assetName, ResourceLocation materialID, boolean withDarker)
	{
		if (this.trimMaterials.put(assetName, materialID) != null)
			StructureGelMod.LOGGER.warn("Registered an armor trim material palette that overrides an existing palette. asset_name=" + assetName + ", material_id=" + materialID);

		if (withDarker && this.trimMaterials.put(assetName + darkerSuffix, materialID.withSuffix(darkerSuffix)) != null)
			StructureGelMod.LOGGER.warn("Registered an armor trim material palette that overrides an existing palette. asset_name=" + assetName + darkerSuffix + ", material_id=" + materialID.withSuffix(darkerSuffix));
	}

	/**
	 * Registers a trim material to be emissive at the brightness passed.
	 * 
	 * @param material
	 * @param brightness
	 */
	public void registerMaterialBrightness(ResourceKey<TrimMaterial> material, int brightness)
	{
		this.materialLight.put(material, brightness);
	}
}
