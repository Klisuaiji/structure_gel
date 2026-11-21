package com.legacy.structure_gel.api.block_entity;

import com.legacy.structure_gel.api.dynamic_spawner.DynamicSpawnerType;
import com.legacy.structure_gel.core.block_entity.DynamicSpawnerBlockEntity;
import com.legacy.structure_gel.core.registry.SGRegistry;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;

/**
 * Contains methods for modifying block entity data.
 *
 * @author Silver_David
 */
public class BlockEntityAccessHelper
{
	/**
	 * Places a dynamic spawner with the {@link DynamicSpawnerType} passed
	 * 
	 * @param level
	 * @param pos
	 * @param type
	 */
	public static void placeDynamicSpawner(ServerLevelAccessor level, BlockPos pos, DynamicSpawnerType type)
	{
		level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
		level.setBlock(pos, SGRegistry.Blocks.DYNAMIC_SPAWNER.get().defaultBlockState(), 2);
		setDynamicSpawnerType(level, pos, type);
	}

	/**
	 * Attempts to set the {@link DynamicSpawnerType} of the block at pos if
	 * applicable
	 * 
	 * @param level
	 * @param pos
	 * @param type
	 */
	public static void setDynamicSpawnerType(ServerLevelAccessor level, BlockPos pos, DynamicSpawnerType type)
	{
		if (level.getBlockEntity(pos) instanceof DynamicSpawnerBlockEntity dynSpawner)
		{
			dynSpawner.setSpawner(type, level.registryAccess());
		}
	}
}
