package com.legacy.structure_gel.api.block.gel;

import com.legacy.structure_gel.api.registry.StructureGelRegistries;
import com.legacy.structure_gel.core.registry.SGRegistry;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;

public interface GelSpreadRestriction
{
	public static final Codec<GelSpreadRestriction> CODEC = StructureGelRegistries.GEL_SPREAD_RESTRICTION.byNameCodec().dispatch(GelSpreadRestriction::getType, GelSpreadRestrictionType::codec);

	GelSpreadRestrictionType getType();

	boolean isPermitted(Context context);

	MutableComponent getTooltip();

	public static record Context(LevelReader level, BlockPos sourcePos, ItemStack placer, BlockPos placePos)
	{
	}

	public static class NoSkyRestriction implements GelSpreadRestriction
	{
		public static final NoSkyRestriction INSTANCE = new NoSkyRestriction();
		public static final MapCodec<NoSkyRestriction> CODEC = MapCodec.unit(INSTANCE);

		@Override
		public GelSpreadRestrictionType getType()
		{
			return SGRegistry.GelSpreadRestrictions.NO_SKY_VISIBILITY.get();
		}

		@Override
		public boolean isPermitted(Context context)
		{
			LevelReader level = context.level;
			BlockPos pos = context.placePos;
			if (level.canSeeSky(pos)) // Light level 15 check (glass would pass this)
				return false;
			for (int dy = 1; pos.above(dy).getY() < level.getMaxY(); dy++) // Check the actual blocks. If we find something, we can't see the sky
				if (!level.isEmptyBlock(pos.above(dy)))
					return true;
			return false;
		}

		@Override
		public MutableComponent getTooltip()
		{
			return StructureGelBlock.legacyTranslation("photosensitive");
		}
	}

	public static record DistanceRestriction(int maxDistance) implements GelSpreadRestriction
	{
		public static final MapCodec<DistanceRestriction> CODEC = RecordCodecBuilder.mapCodec(instance ->
		{
			return instance.group(ExtraCodecs.NON_NEGATIVE_INT.fieldOf("max_distance").forGetter(DistanceRestriction::maxDistance)).apply(instance, DistanceRestriction::new);
		});

		public static final DistanceRestriction FIFTY = new DistanceRestriction(50);

		@Override
		public GelSpreadRestrictionType getType()
		{
			return SGRegistry.GelSpreadRestrictions.DISTANCE.get();
		}

		@Override
		public boolean isPermitted(Context context)
		{
			return context.sourcePos.closerToCenterThan(context.placePos.getCenter(), this.maxDistance);
		}

		@Override
		public MutableComponent getTooltip()
		{
			return StructureGelBlock.legacyTranslation("max_distance", this.maxDistance);
		}
	}

	public static class ItemStackDistance implements GelSpreadRestriction
	{
		public static final ItemStackDistance INSTANCE = new ItemStackDistance();
		public static final MapCodec<ItemStackDistance> CODEC = MapCodec.unit(INSTANCE);

		@Override
		public GelSpreadRestrictionType getType()
		{
			return SGRegistry.GelSpreadRestrictions.ITEM_STACK_COUNT_DISTANCE.get();
		}

		@Override
		public boolean isPermitted(Context context)
		{
			return new DistanceRestriction(context.placer.getCount()).isPermitted(context);
		}

		@Override
		public MutableComponent getTooltip()
		{
			return StructureGelBlock.legacyTranslation("dynamic_spread_dist");
		}
	}
}
