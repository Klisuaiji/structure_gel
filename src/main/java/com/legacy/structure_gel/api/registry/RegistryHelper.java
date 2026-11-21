package com.legacy.structure_gel.api.registry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;

/**
 * Contains helper methods for handling registry data.
 *
 * @author Silver_David
 */
public class RegistryHelper
{
	/**
	 * @return Attempts to get an object from registry data
	 */
	public static <T> Optional<T> get(LevelAccessor level, ResourceKey<T> key)
	{
		return get(level.registryAccess(), key);
	}

	public static <T> Optional<T> get(RegistryAccess registries, ResourceKey<T> key)
	{
		return get(registries, key.registryKey(), key);
	}

	public static <T> Optional<T> get(RegistryAccess registries, ResourceKey<Registry<T>> registryKey, ResourceKey<T> key)
	{
		return registries.lookup(registryKey).map(r -> r.get(key).map(Holder::value).orElse(null));
	}

	/**
	 * @param <T>
	 * @param registry
	 * @param tagKey
	 * @param object
	 * @return True if the object passed is in the tag passed
	 */
	public static <T> boolean isInTag(Registry<T> registry, TagKey<T> tagKey, T object)
	{
		Optional<ResourceKey<T>> key = registry.getResourceKey(object);
		if (key.isPresent())
		{
			Optional<Holder.Reference<T>> holder = registry.getHolder(key.get());
			if (holder.isPresent())
			{
				Optional<HolderSet.Named<T>> tag = registry.getTag(tagKey);
				return tag.isPresent() ? tag.get().contains(holder.get()) : false;
			}
		}
		return false;
	}

	public static <T> boolean isInTag(RegistryAccess registryAccess, ResourceKey<Registry<T>> registryKey, TagKey<T> tagKey, T object)
	{
		Optional<Registry<T>> registry = registryAccess.registry(registryKey);
		return registry.isPresent() ? isInTag(registry.get(), tagKey, object) : false;
	}

	/**
	 * Merges the passed structure processor lists and returns the result.
	 *
	 * @param structureProcessors
	 * @return The combined structure processor lists
	 */
	@SafeVarargs
	public static StructureProcessorList combineProcessors(StructureProcessorList... structureProcessors)
	{
		List<StructureProcessor> processors = new ArrayList<>();
		for (StructureProcessorList spl : structureProcessors)
			processors.addAll(spl.list());
		return new StructureProcessorList(processors);
	}

	/**
	 * Merges the passed structure processors into the passed structure processor
	 * list and returns the result
	 *
	 * @param structureProcessorList
	 * @param structureProcessors
	 * @return A copy of the passed structure processor list with the new processors
	 *         added to it
	 */
	public static StructureProcessorList combineProcessors(StructureProcessorList structureProcessorList, StructureProcessor... structureProcessors)
	{
		List<StructureProcessor> processors = new ArrayList<>(structureProcessorList.list());
		processors.addAll(Arrays.asList(structureProcessors));
		return new StructureProcessorList(processors);
	}
}
