package com.legacy.structure_gel.core.data_components;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.util.random.Weight;
import net.minecraft.util.random.WeightedEntry;
import net.minecraft.util.random.WeightedEntry.Wrapper;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public record BlockPalette(WeightedRandomList<WeightedEntry.Wrapper<BlockState>> blocks)
{
	public static final Codec<BlockPalette> CODEC = RecordCodecBuilder.create(instance ->
	{
		return instance.group(WeightedRandomList.codec(WeightedEntry.Wrapper.codec(BlockState.CODEC)).fieldOf("blocks").forGetter(p ->
		{
			return p.blocks;
		})).apply(instance, BlockPalette::new);
	});

	public static final StreamCodec<FriendlyByteBuf, BlockPalette> STREAM_CODEC = new StreamCodec<>()
	{
		@Override
		public BlockPalette decode(FriendlyByteBuf buff)
		{
			int size = buff.readInt();
			var builder = SimpleWeightedRandomList.<BlockState>builder();
			for (int i = 0; i < size; i++)
			{
				int weight = buff.readInt();
				BlockState state = Block.BLOCK_STATE_REGISTRY.byId(buff.readInt());
				builder.add(state, weight);
			}
			return BlockPalette.of(builder.build());
		}

		@Override
		public void encode(FriendlyByteBuf buff, BlockPalette palette)
		{
			var blocks = palette.blocks.unwrap();
			int size = blocks.size();
			buff.writeInt(size);
			for (int i = 0; i < size; i++)
			{
				WeightedEntry.Wrapper<BlockState> b = blocks.get(i);
				Weight weight = b.getWeight();
				BlockState state = b.data();
				
				buff.writeInt(weight.asInt());
				buff.writeInt(Block.BLOCK_STATE_REGISTRY.getId(state));
			}
		}
	};

	public static final BlockPalette EMPTY = of(SimpleWeightedRandomList.empty());
	
	public static BlockPalette of(WeightedRandomList<WeightedEntry.Wrapper<BlockState>> palette)
	{
		return new BlockPalette(palette);
	}
	
	public boolean isEmpty()
	{
		return this.blocks.isEmpty();
	}
	
	public Optional<Wrapper<BlockState>> getRandom(RandomSource rand)
	{
		return this.blocks.getRandom(rand);
	}
}
