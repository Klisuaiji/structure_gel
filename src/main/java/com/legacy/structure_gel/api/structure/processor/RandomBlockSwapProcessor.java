package com.legacy.structure_gel.api.structure.processor;

import javax.annotation.Nullable;

import com.legacy.structure_gel.core.registry.SGRegistry;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/**
 * Processor that replaces one block with another if a random chance succeeds.
 *
 * @author Silver_David
 */
public class RandomBlockSwapProcessor extends StructureProcessor
{
	public static final MapCodec<RandomBlockSwapProcessor> CODEC = RecordCodecBuilder.mapCodec((instance) ->
	{
		return instance.group(BuiltInRegistries.BLOCK.byNameCodec().fieldOf("condition").forGetter(processor ->
		{
			return processor.condition;
		}), Codec.FLOAT.fieldOf("chance").forGetter(processor ->
		{
			return processor.chance;
		}), BlockState.CODEC.fieldOf("change_to").forGetter(processor ->
		{
			return processor.changeTo;
		})).apply(instance, RandomBlockSwapProcessor::new);
	});

	private final Block condition;
	private final float chance;
	private final BlockState changeTo;

	/**
	 * @param condition
	 *            The block to change
	 * @param chance
	 *            Expressed as a percentage. 0.1F = 10%
	 * @param changeTo
	 *            The block state to change the condition to
	 */
	public RandomBlockSwapProcessor(Block condition, float chance, BlockState changeTo)
	{
		this.condition = condition;
		this.chance = chance;
		this.changeTo = changeTo;
	}

	/**
	 * Always replaces the condition block
	 * 
	 * @param condition
	 *            The block to change
	 * @param changeTo
	 *            The block state to change the condition to
	 */
	public RandomBlockSwapProcessor(Block condition, BlockState changeTo)
	{
		this(condition, 1.0F, changeTo);
	}

	/**
	 * @param condition
	 *            The block to change
	 * @param chance
	 *            Expressed as a percentage. 0.1F = 10%
	 * @param changeTo
	 *            The block to change the condition to
	 */
	public RandomBlockSwapProcessor(Block condition, float chance, Block changeTo)
	{
		this(condition, chance, changeTo.defaultBlockState());
	}

	/**
	 * Always replaces the condition block
	 * 
	 * @param condition
	 *            The block to change
	 * @param changeTo
	 *            The block to change the condition to
	 */
	public RandomBlockSwapProcessor(Block condition, Block changeTo)
	{
		this(condition, changeTo.defaultBlockState());
	}

	@Nullable
	@Override
	public StructureTemplate.StructureBlockInfo processBlock(LevelReader level, BlockPos pos, BlockPos pos2, StructureTemplate.StructureBlockInfo existing, StructureTemplate.StructureBlockInfo placed, StructurePlaceSettings settings)
	{
		if (placed.state().getBlock().equals(this.condition) && (this.chance == 1.0F || settings.getRandom(placed.pos()).nextFloat() < this.chance))
			return new StructureTemplate.StructureBlockInfo(placed.pos(), this.changeTo, null);
		return placed;
	}

	@Override
	protected StructureProcessorType<?> getType()
	{
		return SGRegistry.Processors.REPLACE_BLOCK.get();
	}
}
