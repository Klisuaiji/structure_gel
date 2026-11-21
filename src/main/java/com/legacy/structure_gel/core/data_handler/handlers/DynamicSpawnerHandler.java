package com.legacy.structure_gel.core.data_handler.handlers;

import com.legacy.structure_gel.api.data_handler.handlers.ProbabilityDataHandler;
import com.legacy.structure_gel.api.data_handler.parsing.DataMap;
import com.legacy.structure_gel.api.data_handler.parsing.DataParser;
import com.legacy.structure_gel.api.dynamic_spawner.DynamicSpawnerType;
import com.legacy.structure_gel.api.registry.StructureGelRegistries;
import com.legacy.structure_gel.core.block_entity.DynamicSpawnerBlockEntity;
import com.legacy.structure_gel.core.block_entity.DynamicSpawnerBlockEntity.ExpRange;
import com.legacy.structure_gel.core.registry.SGRegistry;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class DynamicSpawnerHandler extends ProbabilityDataHandler<DynamicSpawnerHandler>
{
	public static final DataParser.Factory PARSER = DataParser.of(builder ->
	{
		builder.add("dynamic_spawner_type", StructureGelRegistries.Keys.DYNAMIC_SPAWNER_TYPE, SGRegistry.DynamicSpawnerTypes.DEFAULT.get());
		builder.add("generation_chance", 1.0F, 0.0F, 1.0F);
		builder.add("min_exp", ExpRange.VANILLA.min, 0, Integer.MAX_VALUE);
		builder.add("max_exp", ExpRange.VANILLA.max, 0, Integer.MAX_VALUE);
	});

	private ResourceKey<DynamicSpawnerType> dynamicSpawnerType;
	private ExpRange expRange;

	public DynamicSpawnerHandler(DataMap data)
	{
		super(data, "generation_chance");
		this.dynamicSpawnerType = data.get("dynamic_spawner_type", StructureGelRegistries.Keys.DYNAMIC_SPAWNER_TYPE);
		this.expRange = new ExpRange(data.getInt("min_exp"), data.getInt("max_exp"));
	}

	@Override
	protected void handle(Context context)
	{
		WorldGenLevel level = context.getLevel();
		BlockPos pos = context.getBlockPos();

		level.setBlock(pos, SGRegistry.Blocks.DYNAMIC_SPAWNER.get().defaultBlockState(), Block.UPDATE_CLIENTS);
		if (level.getBlockEntity(pos) instanceof DynamicSpawnerBlockEntity dynamicSpawner)
		{
			dynamicSpawner.setSpawner(StructureGelRegistries.DYNAMIC_SPAWNER_TYPE.getValueOrThrow(this.dynamicSpawnerType), level.registryAccess());
			dynamicSpawner.setExpRange(this.expRange);
			dynamicSpawner.setChanged();
		}

		if (level instanceof ServerLevel)
		{
			BlockState updatedState = level.getBlockState(pos);
			((ServerLevel) level).sendBlockUpdated(pos, updatedState, updatedState, Block.UPDATE_ALL);
		}
	}
}
