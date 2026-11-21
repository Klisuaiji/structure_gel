package com.legacy.structure_gel.core.data_handler.handlers;

import com.legacy.structure_gel.api.data_handler.handlers.ProbabilityDataHandler;
import com.legacy.structure_gel.api.data_handler.parsing.DataMap;
import com.legacy.structure_gel.api.data_handler.parsing.DataParser;
import com.legacy.structure_gel.api.registry.StructureGelRegistries;
import com.legacy.structure_gel.core.registry.SGRegistry;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

public class ArchaeologyHandler extends ProbabilityDataHandler<ArchaeologyHandler>
{
	public static final DataParser.Factory PARSER = DataParser.of(builder ->
	{
		builder.add("block", Blocks.SUSPICIOUS_SAND.defaultBlockState()).setDefaultValName("minecraft:suspicious_sand");
		builder.add("loot", BuiltInLootTables.DESERT_PYRAMID_ARCHAEOLOGY.location()).setSuggestions(StructureGelRegistries.LOOT_TABLE_ALIAS::keySet, ResourceLocation::toString);
		builder.add("generation_chance", 1.0F, 0.0F, 1.0F);
	});

	private final ResourceLocation lootTable;
	private final BlockState block;

	public ArchaeologyHandler(DataMap data)
	{
		super(data, "generation_chance");
		this.lootTable = data.get("loot", ResourceLocation.class);
		this.block = data.get("block", BlockState.class);
	}

	@Override
	protected void handle(Context context)
	{
		BlockState dataHandler = context.getDataHandlerState();
		WorldGenLevel level = context.getLevel();
		BlockPos pos = context.getBlockPos();
		BlockState state = this.createState(dataHandler, this.block);
		level.setBlock(pos, state, Block.UPDATE_CLIENTS + Block.UPDATE_KNOWN_SHAPE);
		if (level.getBlockEntity(pos) instanceof BrushableBlockEntity brushable)
		{
			brushable.setLootTable(ResourceKey.create(Registries.LOOT_TABLE, this.lootTable), pos.asLong());
		}
	}
}
