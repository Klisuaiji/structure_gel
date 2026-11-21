package com.legacy.structure_gel.api.structure.jigsaw;

import java.util.List;
import java.util.function.Consumer;

import com.legacy.structure_gel.core.StructureGelMod;
import com.mojang.datafixers.util.Pair;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.pools.FeaturePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.ListPoolElement;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;

/**
 * Contains methods to access normally privated values in the jigsaw element or
 * jigsaw registry. Also allows for you to add to/remove from existing
 * JigsawPatterns, allowing you to add new houses to villages, change pillager
 * outpost designs, etc.
 *
 * @author Silver_David
 */
public class JigsawAccessHelper
{
	/**
	 * Adds the input jigsaw pieces to the pool passed in. Make sure the pool you're
	 * adding to has been initialized<br>
	 * <br>
	 * Note: Make sure the structure's jigsaw blocks have the proper connections
	 *
	 * @param pool
	 * @param pieces
	 * @see JigsawPoolBuilder
	 */
	public static void addToPool(Registry<StructureTemplatePool> registry, ResourceLocation pool, List<Pair<StructurePoolElement, Integer>> pieces)
	{
		modifyJigsawPattern(registry, pool, template ->
		{
			for (Pair<StructurePoolElement, Integer> pair : pieces)
				for (int i = 0; i < pair.getSecond(); i++)
					template.templates.add(pair.getFirst());
		});
	}

	/**
	 * Applies the passed consumer to a registered structure template stored under
	 * the key passed, if it exists
	 * 
	 * @param key
	 * @param action
	 */
	public static void modifyJigsawPattern(Registry<StructureTemplatePool> registry, ResourceLocation key, Consumer<StructureTemplatePool> action)
	{
		registry.getOptional(key).ifPresentOrElse(action, () -> StructureGelMod.LOGGER.error("Could not perform an action on {} because it does not exist.", key));
	}

	/**
	 * @param element
	 * @return The name of this element
	 */
	public static ResourceLocation getSingleJigsawPieceLocation(SinglePoolElement element)
	{
		return element.template.left().get();
	}

	/**
	 * @param element
	 * @return The elements stored in this element
	 */
	public static List<StructurePoolElement> getListJigsawPiecePieces(ListPoolElement element)
	{
		return element.elements;
	}

	/**
	 * @param element
	 * @return The configured feature stored in this element
	 */
	public static PlacedFeature getFeatureJigsawPieceFeatures(FeaturePoolElement element)
	{
		return element.feature.value();
	}
}
