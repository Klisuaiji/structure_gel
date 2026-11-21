package com.legacy.structure_gel.api.block.gel;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.legacy.structure_gel.api.registry.StructureGelRegistries;
import com.legacy.structure_gel.core.registry.SGRegistry;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.MutableComponent;

public interface GelSpreadBehavior
{
	public static final Codec<GelSpreadBehavior> CODEC = StructureGelRegistries.GEL_SPREAD_BEHAVIOR.byNameCodec().dispatch(GelSpreadBehavior::getType, GelSpreadBehaviorType::codec);

	GelSpreadBehaviorType getType();

	List<Vec3i> getSpreadOffsets(Context context);

	MutableComponent getTooltip();

	public static record Context(BlockPos spreadFrom, Optional<Direction.Axis> initialAxis)
	{
	}

	public static class FaceSpread implements GelSpreadBehavior
	{
		public static final FaceSpread INSTANCE = new FaceSpread();
		public static final MapCodec<FaceSpread> CODEC = MapCodec.unit(INSTANCE);

		@Override
		public GelSpreadBehaviorType getType()
		{
			return SGRegistry.GelSpreadBehaviors.FACE.get();
		}

		@Override
		public List<Vec3i> getSpreadOffsets(Context context)
		{
			List<Vec3i> offsets = new ArrayList<>();
			for (Direction dir : Direction.values())
			{
				offsets.add(dir.getUnitVec3i());
			}
			return offsets;
		}

		@Override
		public MutableComponent getTooltip()
		{
			return StructureGelBlock.legacyTranslation("default");
		}
	}

	public static class EdgeSpread implements GelSpreadBehavior
	{
		public static final EdgeSpread INSTANCE = new EdgeSpread();
		public static final MapCodec<EdgeSpread> CODEC = MapCodec.unit(INSTANCE);

		@Override
		public GelSpreadBehaviorType getType()
		{
			return SGRegistry.GelSpreadBehaviors.EDGE.get();
		}

		@Override
		public List<Vec3i> getSpreadOffsets(Context context)
		{
			List<Vec3i> offsets = new ArrayList<>();
			for (Direction v : Direction.Plane.VERTICAL)
			{
				for (Direction h : Direction.Plane.HORIZONTAL)
				{
					offsets.add(h.getUnitVec3i().offset(v.getUnitVec3i()));
				}
			}
			for (Direction h : Direction.Plane.HORIZONTAL)
			{
				offsets.add(h.getUnitVec3i().offset(h.getClockWise().getUnitVec3i()));
			}
			return offsets;
		}

		@Override
		public MutableComponent getTooltip()
		{
			return StructureGelBlock.legacyTranslation("edge_spread");
		}
	}

	public static class FaceAndEdgeSpread implements GelSpreadBehavior
	{
		public static final FaceAndEdgeSpread INSTANCE = new FaceAndEdgeSpread();
		public static final MapCodec<FaceAndEdgeSpread> CODEC = MapCodec.unit(INSTANCE);

		@Override
		public GelSpreadBehaviorType getType()
		{
			return SGRegistry.GelSpreadBehaviors.FACE_AND_EDGE.get();
		}

		@Override
		public List<Vec3i> getSpreadOffsets(Context context)
		{
			List<Vec3i> offsets = new FaceSpread().getSpreadOffsets(context);
			offsets.addAll(new EdgeSpread().getSpreadOffsets(context));
			return offsets;
		}

		@Override
		public MutableComponent getTooltip()
		{
			return StructureGelBlock.legacyTranslation("diagonal_spread");
		}
	}

	public static class InitialAxisSpread implements GelSpreadBehavior
	{
		public static final InitialAxisSpread INSTANCE = new InitialAxisSpread();
		public static final MapCodec<InitialAxisSpread> CODEC = MapCodec.unit(INSTANCE);

		@Override
		public GelSpreadBehaviorType getType()
		{
			return SGRegistry.GelSpreadBehaviors.INITIAL_AXIS.get();
		}

		@Override
		public List<Vec3i> getSpreadOffsets(Context context)
		{
			List<Vec3i> offsets = new ArrayList<>();
			for (Direction d : Direction.values())
				if (context.initialAxis.map(a -> a != d.getAxis()).orElse(true))
					offsets.add(d.getUnitVec3i());
			return offsets;
		}

		@Override
		public MutableComponent getTooltip()
		{
			return StructureGelBlock.legacyTranslation("axis_spread");
		}
	}
}
