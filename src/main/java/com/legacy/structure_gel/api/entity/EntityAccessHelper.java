package com.legacy.structure_gel.api.entity;

import java.util.Optional;

import javax.annotation.Nullable;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * Contains helper methods to interact with entities.
 *
 * @author Silver_David
 */
public class EntityAccessHelper
{
	/**
	 * @param entity
	 * @return The entity's death loot table
	 */
	public static Optional<ResourceKey<LootTable>> getDeathLootTable(Mob entity)
	{
		return Optional.ofNullable(entity.lootTable);
	}

	/**
	 * Sets the loot table for the entity passed
	 *
	 * @param entity
	 * @param lootTable
	 */
	public static void setDeathLootTable(Mob entity, @Nullable ResourceKey<LootTable> lootTable)
	{
		entity.lootTable = lootTable;
	}
}
