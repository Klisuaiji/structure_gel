package com.legacy.structure_gel.api.block_entity;

import java.util.Optional;

import net.minecraft.Util;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentTable;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.SpawnData;

/**
 * Contains helper methods for modifying the various fields within a mob spawner
 * through it's NBT.
 *
 * @author Silver_David
 */
public class SpawnerAccessHelper
{
	/**
	 * The minimum time a spawner should wait before spawning a mob
	 */
	public static void setMinSpawnDelay(BaseSpawner spawner, int minSpawnDelay)
	{
		spawner.minSpawnDelay = minSpawnDelay;
	}

	/**
	 * The maximum time a spawner should wait before spawning a mob
	 */
	public static void setMaxSpawnDelay(BaseSpawner spawner, int maxSpawnDelay)
	{
		spawner.maxSpawnDelay = maxSpawnDelay;
	}

	/**
	 * The maximum amount of mobs that can spawn from this spawner at a time
	 */
	public static void setSpawnCount(BaseSpawner spawner, int spawnCount)
	{
		spawner.spawnCount = spawnCount;
	}

	/**
	 * How many entities can be around the spawner before it stops spawning mobs
	 */
	public static void setMaxNearbyEntities(BaseSpawner spawner, int maxNearbyEntities)
	{
		spawner.maxNearbyEntities = maxNearbyEntities;
	}

	/**
	 * How far the player can be from the spawner for it to spawn mobs
	 */
	public static void setRequiredPlayerRange(BaseSpawner spawner, int requiredPlayerRange)
	{
		spawner.requiredPlayerRange = requiredPlayerRange;
	}

	/**
	 * The horizontal area a spawner will search to try placing mobs. The vertical
	 * area is between the y level below the spawner and the y level above it (3
	 * blocks high)
	 */
	public static void setSpawnRange(BaseSpawner spawner, int spawnRange)
	{
		spawner.spawnRange = spawnRange;
	}

	/**
	 * Sets the passed entity as the mob that the spawner should spawn
	 */
	public static void setSpawnPotentials(BaseSpawner spawner, EntityType<?> entityType)
	{
		setSpawnPotentials(spawner, createSpawnerEntity(entityType));
	}

	/**
	 * Sets the mobs that will spawn
	 */
	public static void setSpawnPotentials(BaseSpawner spawner, SpawnData... spawnData)
	{
		SimpleWeightedRandomList<SpawnData> list = Util.make(new SimpleWeightedRandomList.Builder<SpawnData>(), builder ->
		{
			for (SpawnData data : spawnData)
				builder.add(data, 1);
		}).build();
		setSpawnPotentials(spawner, list);
	}

	/**
	 * Takes the input spawner entities and puts it into the mob spawner's data
	 */
	public static void setSpawnPotentials(BaseSpawner spawner, SimpleWeightedRandomList<SpawnData> spawnData)
	{
		spawner.spawnPotentials = spawnData;
	}

	/**
	 * @param entity
	 * @return Spawn data for the entity passed
	 */
	public static SpawnData createSpawnerEntity(EntityType<?> entity)
	{
		return createSpawnerEntity(entity, new CompoundTag(), Optional.empty(), Optional.empty());
	}

	/**
	 * @param entity
	 * @param entityTag
	 * @param customSpawnRules
	 * @return Spawn data for the entity passed
	 */
	public static SpawnData createSpawnerEntity(EntityType<?> entity, CompoundTag entityTag, Optional<SpawnData.CustomSpawnRules> customSpawnRules, Optional<EquipmentTable> equipment)
	{
		CompoundTag nbt = entityTag.copy();
		ResourceLocation name = BuiltInRegistries.ENTITY_TYPE.getKey(entity);
		nbt.putString("id", name != null ? BuiltInRegistries.ENTITY_TYPE.getKey(entity).toString() : "pig");
		return new SpawnData(nbt, customSpawnRules, equipment);
	}
}
