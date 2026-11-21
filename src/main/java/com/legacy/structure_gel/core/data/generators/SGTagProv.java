package com.legacy.structure_gel.core.data.generators;

import java.util.concurrent.CompletableFuture;

import com.legacy.structure_gel.api.data.tags.SGTags;
import com.legacy.structure_gel.core.StructureGelMod;
import com.legacy.structure_gel.core.registry.SGRegistry;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.data.tags.StructureTagsProvider;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.neoforged.neoforge.common.data.BlockTagsProvider;

public class SGTagProv
{
	public static class BlockTagProv extends BlockTagsProvider
	{
		public BlockTagProv(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider)
		{
			super(output, lookupProvider, StructureGelMod.MODID);
		}

		@Override
		protected void addTags(HolderLookup.Provider prov)
		{
			this.tag(SGTags.BlockTags.GEL).add(SGRegistry.Blocks.RED_GEL.get(), SGRegistry.Blocks.BLUE_GEL.get(), SGRegistry.Blocks.GREEN_GEL.get(), SGRegistry.Blocks.CYAN_GEL.get(), SGRegistry.Blocks.ORANGE_GEL.get(), SGRegistry.Blocks.YELLOW_GEL.get());

			this.tag(BlockTags.MINEABLE_WITH_PICKAXE).add(SGRegistry.Blocks.DYNAMIC_SPAWNER.get());
			this.tag(BlockTags.FEATURES_CANNOT_REPLACE).add(SGRegistry.Blocks.DYNAMIC_SPAWNER.get());
			this.tag(BlockTags.LAVA_POOL_STONE_CANNOT_REPLACE).add(SGRegistry.Blocks.DYNAMIC_SPAWNER.get());

			this.tag(BlockTags.DRAGON_IMMUNE).add(SGRegistry.Blocks.DATA_HANDLER.get());
			this.tag(BlockTags.WITHER_IMMUNE).add(SGRegistry.Blocks.DATA_HANDLER.get());
		}
	}

	public static class ItemTagProv extends ItemTagsProvider
	{

		public ItemTagProv(PackOutput output, CompletableFuture<Provider> lookupProvider, CompletableFuture<TagLookup<Block>> blockTags)
		{
			super(output, lookupProvider, blockTags, StructureGelMod.MODID);
		}

		@Override
		protected void addTags(Provider provider)
		{
			this.copy(SGTags.BlockTags.GEL, SGTags.ItemTags.GEL);
			this.tag(SGTags.ItemTags.GEL_INTERACTABLE).add(Items.DEBUG_STICK, Items.GUNPOWDER).addTag(SGTags.ItemTags.GEL);
		}
	}

	public static class StructureTagProv extends StructureTagsProvider
	{
		public StructureTagProv(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider)
		{
			super(output, lookupProvider, StructureGelMod.MODID);
		}

		@Override
		protected void addTags(HolderLookup.Provider prov)
		{
			this.tag(SGTags.StructureTags.LAKE_PROOF).add(BuiltinStructures.PILLAGER_OUTPOST, BuiltinStructures.WOODLAND_MANSION, BuiltinStructures.JUNGLE_TEMPLE, BuiltinStructures.DESERT_PYRAMID, BuiltinStructures.IGLOO, BuiltinStructures.STRONGHOLD);
		}
	}
}
