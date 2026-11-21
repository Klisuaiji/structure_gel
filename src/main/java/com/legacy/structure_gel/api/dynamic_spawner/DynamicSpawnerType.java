package com.legacy.structure_gel.api.dynamic_spawner;

import com.legacy.structure_gel.core.dynamic_spawner.DynamicSpawner;

import net.minecraft.core.HolderLookup;

/**
 * Used by Dynamic Spawner blocks to determine what they spawn. Dynamic spawners
 * differ from vanilla spawners in that they do now save their mob spawn logic
 * in the spawner block entity. Instead, it's a registered logic that gets
 * assigned to the dynamic spawner block when it loads.<br>
 * <br>
 * Differences with vanilla mob spawners: <br>
 * 1) The settings of a spawner that already exists in the world can be modified
 * by changing what the DynamicSpawnerType produces.<br>
 * 2) Spawners created can reference data that may not be consistent when mods
 * update or get added, such as item tags.<br>
 * <br>
 * Disadvantages: <br>
 * 1) This type of spawner may not work with mods that modify spawners since it
 * uses a separate block entirely.
 * 
 * @author Silver_David
 *
 */
public interface DynamicSpawnerType
{
	void buildSpawner(DynamicSpawner.Builder builder, HolderLookup.Provider registryAccess);

	default DynamicSpawner.Builder create(HolderLookup.Provider registryAccess)
	{
		DynamicSpawner.Builder builder = new DynamicSpawner.Builder(registryAccess);
		this.buildSpawner(builder, registryAccess);
		return builder;
	}
}