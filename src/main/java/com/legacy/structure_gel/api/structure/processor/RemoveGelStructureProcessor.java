package com.legacy.structure_gel.api.structure.processor;

import javax.annotation.Nullable;

import com.legacy.structure_gel.api.block.gel.StructureGelBlock;
import com.legacy.structure_gel.api.data.tags.SGTags;
import com.legacy.structure_gel.core.registry.SGRegistry;
import com.legacy.structure_gel.core.structure.jigsaw.GelSinglePoolElement;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/**
 * Replaces blocks tagged as structure_gel:gel with air and makes air behave as
 * a structure void.<br>
 * <br>
 * This processor is automatically added to anything using
 * {@link GelSinglePoolElement}.
 *
 * @author Silver_David
 */
public final class RemoveGelStructureProcessor extends StructureProcessor
{
	public static final RemoveGelStructureProcessor INSTANCE = new RemoveGelStructureProcessor();
	public static final MapCodec<RemoveGelStructureProcessor> CODEC = MapCodec.unit(() -> INSTANCE);

	private RemoveGelStructureProcessor()
	{
	}

	@Nullable
	@Override
	public StructureTemplate.StructureBlockInfo processBlock(LevelReader level, BlockPos pos, BlockPos pos2, StructureTemplate.StructureBlockInfo existing, StructureTemplate.StructureBlockInfo placed, StructurePlaceSettings settings)
	{
		if (placed.state().is(SGTags.BlockTags.GEL))
			return new StructureTemplate.StructureBlockInfo(placed.pos(), placed.state().getBlock() instanceof StructureGelBlock gel ? gel.getReplacementState() : Blocks.AIR.defaultBlockState(), null);
		return placed.state().is(Blocks.AIR) ? null : placed;
	}

	@Override
	protected StructureProcessorType<?> getType()
	{
		return SGRegistry.Processors.REMOVE_FILLER.get();
	}
}
