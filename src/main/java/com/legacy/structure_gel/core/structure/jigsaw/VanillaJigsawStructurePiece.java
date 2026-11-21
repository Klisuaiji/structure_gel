package com.legacy.structure_gel.core.structure.jigsaw;

import com.legacy.structure_gel.api.structure.jigsaw.ExtendedJigsawStructurePiece;
import com.legacy.structure_gel.api.structure.jigsaw.IPieceFactory;
import com.legacy.structure_gel.core.registry.SGRegistry;
import com.legacy.structure_gel.core.util.Internal;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;

/**
 * The default implementation of {@link ExtendedJigsawStructurePiece}.
 *
 * @author Silver_David
 */
@Internal
public final class VanillaJigsawStructurePiece extends ExtendedJigsawStructurePiece
{
	public VanillaJigsawStructurePiece(IPieceFactory.Context context)
	{
		super(context);
	}

	public VanillaJigsawStructurePiece(StructurePieceSerializationContext context, CompoundTag nbt)
	{
		super(context, nbt);
	}

	@Override
	public StructurePieceType getType()
	{
		return SGRegistry.StructurePieceTypes.VANILLA_JIGSAW.get();
	}

	@Override
	public void handleDataMarker(String key, BlockPos pos, ServerLevelAccessor level, RandomSource rand, BoundingBox bounds)
	{

	}
}
