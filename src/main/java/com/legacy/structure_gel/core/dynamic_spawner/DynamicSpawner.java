package com.legacy.structure_gel.core.dynamic_spawner;

import javax.annotation.Nullable;

import com.legacy.structure_gel.api.block_entity.SpawnerAccessHelper;
import com.legacy.structure_gel.core.registry.SGRegistry;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.SpawnData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class DynamicSpawner extends BaseSpawner implements Cloneable
{
	public DynamicSpawner()
	{
	}
	
	public void copyFrom(DynamicSpawner.Builder builder)
	{
		this.spawnDelay = builder.spawnDelay;
		this.spawnPotentials = builder.spawnPotentials.build();
		this.minSpawnDelay = builder.minSpawnDelay;
		this.maxSpawnDelay = builder.maxSpawnDelay;
		this.spawnCount = builder.spawnCount;
		this.maxNearbyEntities = builder.maxNearbyEntities;
		this.requiredPlayerRange = builder.requiredPlayerRange;
		this.spawnRange = builder.spawnRange;
	}
	
	public void copyFrom(DynamicSpawner spawner)
	{
		this.spawnDelay = spawner.spawnDelay;
		this.spawnPotentials = spawner.spawnPotentials;
		this.minSpawnDelay = spawner.minSpawnDelay;
		this.maxSpawnDelay = spawner.maxSpawnDelay;
		this.spawnCount = spawner.spawnCount;
		this.maxNearbyEntities = spawner.maxNearbyEntities;
		this.requiredPlayerRange = spawner.requiredPlayerRange;
		this.spawnRange = spawner.spawnRange;
	}

	@Override
	public void broadcastEvent(Level level, BlockPos pos, int param)
	{
		level.blockEvent(pos, SGRegistry.Blocks.DYNAMIC_SPAWNER.get(), param, 0);
	}

	@Override
	public void setNextSpawnData(@Nullable Level level, BlockPos pos, SpawnData spawnData)
	{
		super.setNextSpawnData(level, pos, spawnData);
		if (level != null)
		{
			BlockState state = level.getBlockState(pos);
			level.sendBlockUpdated(pos, state, state, Block.UPDATE_INVISIBLE);
		}
	}

	@Override
	protected DynamicSpawner clone()
	{
		DynamicSpawner clone = new DynamicSpawner();
		clone.copyFrom(this);
		return clone;
	}
	
	public static class Builder
	{
		private final HolderLookup.Provider lookupProvider;
		
		private int spawnDelay = 20;
		private SimpleWeightedRandomList.Builder<SpawnData> spawnPotentials = SimpleWeightedRandomList.builder();
		private int minSpawnDelay = 200;
		private int maxSpawnDelay = 800;
		private int spawnCount = 4;
		private int maxNearbyEntities = 6;
		private int requiredPlayerRange = 16;
		private int spawnRange = 4;
	    
	    public Builder(HolderLookup.Provider lookupProvider)
	    {
	    	this.lookupProvider = lookupProvider;
	    }
	    
	    public HolderLookup.Provider lookup()
	    {
	    	return this.lookupProvider;
	    }
	    
		public Builder minSpawnDelay(int minSpawnDelay)
		{
			this.minSpawnDelay = minSpawnDelay;
			return this;
		}

		public Builder maxSpawnDelay(int maxSpawnDelay)
		{
			this.maxSpawnDelay = maxSpawnDelay;
			return this;
		}
		
		public Builder spawnDelay(int min, int max)
		{
			return this.minSpawnDelay(min).maxSpawnDelay(max);
		}

		public Builder spawnCount(int spawnCount)
		{
			this.spawnCount = spawnCount;
			return this;
		}

		public Builder maxNearbyEntities(int maxNearbyEntities)
		{
			this.maxNearbyEntities = maxNearbyEntities;
			return this;
		}

		public Builder requiredPlayerRange(int requiredPlayerRange)
		{
			this.requiredPlayerRange = requiredPlayerRange;
			return this;
		}

		public Builder spawnRange(int spawnRange)
		{
			this.spawnRange = spawnRange;
			return this;
		}

		public Builder spawnData(EntityType<?> entityType)
		{
			return spawnData(entityType, 1);
		}

		public Builder spawnData(SpawnData spawnData)
		{
			return spawnData(spawnData, 1);
		}
		
		public Builder spawnData(EntityType<?> entityType, int weight)
		{
			return spawnData(SpawnerAccessHelper.createSpawnerEntity(entityType), weight);
		}

		public Builder spawnData(SpawnData spawnData, int weight)
		{
			this.spawnPotentials.add(spawnData, weight);
			return this;
		}

		public Builder spawnData(SimpleWeightedRandomList.Builder<SpawnData> spawnData)
		{
			spawnData.build().unwrap().forEach(wrapper -> this.spawnPotentials.add(wrapper.data(), wrapper.weight().asInt()));
			return this;
		}
	}
}