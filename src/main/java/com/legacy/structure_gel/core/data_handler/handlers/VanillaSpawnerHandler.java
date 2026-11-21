package com.legacy.structure_gel.core.data_handler.handlers;

import com.legacy.structure_gel.api.block_entity.SpawnerAccessHelper;
import com.legacy.structure_gel.api.data_handler.handlers.ProbabilityDataHandler;
import com.legacy.structure_gel.api.data_handler.parsing.DataMap;
import com.legacy.structure_gel.api.data_handler.parsing.DataParser;
import com.legacy.structure_gel.api.registry.RegistryHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class VanillaSpawnerHandler extends ProbabilityDataHandler<VanillaSpawnerHandler>
{
	public static final DataParser.Factory PARSER = DataParser.of(builder ->
	{
		builder.add("entity", Registries.ENTITY_TYPE, EntityType.PIG);
		builder.add("generation_chance", 1.0F, 0.0F, 1.0F);

		// Values obtained from BaseSpawner
		builder.addShort("min_spawn_delay", (short) 200, (short) 0, Short.MAX_VALUE);
		builder.addShort("max_spawn_delay", (short) 800, (short) 0, Short.MAX_VALUE);
		builder.addShort("spawn_count", (short) 4, (short) 0, Short.MAX_VALUE);
		builder.addShort("max_nearby_entities", (short) 6, (short) 0, Short.MAX_VALUE);
		builder.addShort("required_player_range", (short) 16, (short) 0, Short.MAX_VALUE);
		builder.addShort("spawn_range", (short) 4, (short) 0, Short.MAX_VALUE);

		builder.add("tag", new CompoundTag());

	});

	private final ResourceKey<EntityType<?>> entityType;
	private final short minSpawnDelay, maxSpawnDelay, spawnCount, maxNearbyEntities, requiredPlayerRange, spawnRange;
	private final CompoundTag tag;

	public VanillaSpawnerHandler(DataMap data)
	{
		super(data, "generation_chance");
		this.entityType = data.get("entity", Registries.ENTITY_TYPE);
		this.minSpawnDelay = data.getShort("min_spawn_delay");
		this.maxSpawnDelay = (short) Math.max(this.minSpawnDelay, data.getShort("max_spawn_delay"));
		this.spawnCount = data.getShort("spawn_count");
		this.maxNearbyEntities = data.getShort("max_nearby_entities");
		this.requiredPlayerRange = data.getShort("required_player_range");
		this.spawnRange = data.getShort("spawn_range");

		this.tag = data.get("tag", CompoundTag.class);
	}

	@Override
	protected void handle(Context context)
	{
		WorldGenLevel level = context.getLevel();
		BlockPos pos = context.getBlockPos();
		RandomSource rand = context.getRandom();

		level.setBlock(pos, Blocks.SPAWNER.defaultBlockState(), Block.UPDATE_CLIENTS);
		if (level.getBlockEntity(pos) instanceof SpawnerBlockEntity blockEntity)
		{
			EntityType<?> entity = RegistryHelper.get(level, this.entityType).orElse(null);
			if (entity != null)
				blockEntity.setEntityId(entity, rand);
			BaseSpawner spawner = blockEntity.getSpawner();
			
			SpawnerAccessHelper.setMinSpawnDelay(spawner, this.minSpawnDelay);
			SpawnerAccessHelper.setMinSpawnDelay(spawner, this.minSpawnDelay);
			SpawnerAccessHelper.setMaxSpawnDelay(spawner, this.maxSpawnDelay);
			SpawnerAccessHelper.setSpawnCount(spawner, this.spawnCount);
			SpawnerAccessHelper.setMaxNearbyEntities(spawner, this.maxNearbyEntities);
			SpawnerAccessHelper.setRequiredPlayerRange(spawner, this.requiredPlayerRange);
			SpawnerAccessHelper.setSpawnRange(spawner, this.spawnRange);

			blockEntity.loadWithComponents(this.tag, level.registryAccess());

			blockEntity.setChanged();
		}

		if (level instanceof ServerLevel)
		{
			BlockState updatedState = level.getBlockState(pos);
			((ServerLevel) level).sendBlockUpdated(pos, updatedState, updatedState, Block.UPDATE_ALL);
		}
	}
}
