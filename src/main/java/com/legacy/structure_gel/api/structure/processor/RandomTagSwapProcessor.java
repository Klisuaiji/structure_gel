package com.legacy.structure_gel.api.structure.processor;

import javax.annotation.Nullable;

import com.legacy.structure_gel.core.registry.SGRegistry;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/**
 * Processor that replaces blocks of a specified tag with a different block if a
 * random chance succeeds.
 *
 * @author Silver_David
 */
public class RandomTagSwapProcessor extends StructureProcessor
{
	public static final MapCodec<RandomTagSwapProcessor> CODEC = RecordCodecBuilder.mapCodec((instance) ->
	{
		return instance.group(TagKey.codec(Registries.BLOCK).fieldOf("condition").forGetter(processor ->
		{
			return processor.condition;
		}), Codec.FLOAT.fieldOf("chance").forGetter(processor ->
		{
			return processor.chance;
		}), BlockState.CODEC.fieldOf("change_to").forGetter(processor ->
		{
			return processor.changeTo;
		})).apply(instance, RandomTagSwapProcessor::new);
	});

	public final TagKey<Block> condition;
	public final float chance;
	public final BlockState changeTo;

	/**
	 * @param condition
	 *            The block state to change
	 * @param chance
	 *            Expressed as a percentage. 0.1F = 10%
	 * @param changeTo
	 *            The block state to change the condition to
	 */
	public RandomTagSwapProcessor(TagKey<Block> condition, float chance, BlockState changeTo)
	{
		this.condition = condition;
		this.chance = chance;
		this.changeTo = changeTo;
	}

	/**
	 * Always replaces the condition block state
	 * 
	 * @param condition
	 *            The block to change
	 * @param changeTo
	 *            The block state to change the condition to
	 */
	public RandomTagSwapProcessor(TagKey<Block> condition, BlockState changeTo)
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
	public RandomTagSwapProcessor(TagKey<Block> condition, float chance, Block changeTo)
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
	public RandomTagSwapProcessor(TagKey<Block> condition, Block changeTo)
	{
		this(condition, changeTo.defaultBlockState());
	}

	@Nullable
	@Override
	public StructureTemplate.StructureBlockInfo processBlock(LevelReader level, BlockPos pos, BlockPos pos2, StructureTemplate.StructureBlockInfo existing, StructureTemplate.StructureBlockInfo placed, StructurePlaceSettings settings)
	{
		if (placed.state().is(this.condition) && (this.chance == 1.0F || settings.getRandom(placed.pos()).nextFloat() < this.chance))
			return new StructureTemplate.StructureBlockInfo(placed.pos(), this.changeTo, null);
		return placed;
	}

	@Override
	protected StructureProcessorType<?> getType()
	{
		return SGRegistry.Processors.REPLACE_TAG.get();
	}
}
