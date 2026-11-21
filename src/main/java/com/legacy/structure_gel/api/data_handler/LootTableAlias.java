package com.legacy.structure_gel.api.data_handler;

import javax.annotation.Nullable;

import com.legacy.structure_gel.api.registry.StructureGelRegistries;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * Stores remappings for loot tables in the event that a loot table has its
 * resource location changed, allowing for referencing it by the old name.
 * 
 * @author Silver_David
 *
 */
public class LootTableAlias
{

	/**
	 * @param alias
	 * @return The loot table from the alias registry, or the value passed if no
	 *         value was registered
	 */
	@Nullable
	public static ResourceKey<LootTable> getLootTable(@Nullable ResourceLocation alias)
	{
		return alias == null ? null : StructureGelRegistries.LOOT_TABLE_ALIAS.getOptional(alias).orElse(ResourceKey.create(Registries.LOOT_TABLE, alias));
	}
}
