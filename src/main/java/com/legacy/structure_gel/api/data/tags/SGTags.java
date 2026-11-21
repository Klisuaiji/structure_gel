package com.legacy.structure_gel.api.data.tags;

import com.legacy.structure_gel.core.StructureGelMod;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.Structure;

public interface SGTags
{
	public static interface StructureTags
	{
		/**
		 * Contains all structures that should prevent lakes from generating in them
		 */
		TagKey<Structure> LAKE_PROOF = create("lake_proof");

		public static TagKey<Structure> create(String name)
		{
			return SGTags.create(Registries.STRUCTURE, name);
		}
	}

	public interface BlockTags
	{
		TagKey<Block> GEL = create("gel");
		
		public static TagKey<Block> create(String name)
		{
			return SGTags.create(Registries.BLOCK, name);
		}
	}
	
	public static interface ItemTags
	{
		TagKey<Item> GEL = create("gel");
		TagKey<Item> GEL_INTERACTABLE = create("gel_interactable");

		public static TagKey<Item> create(String name)
		{
			return SGTags.create(Registries.ITEM, name);
		}
	}
	
	private static <T> TagKey<T> create(ResourceKey<Registry<T>> registryKey, String name)
	{
		return TagKey.create(registryKey, StructureGelMod.locate(name));
	}
}
