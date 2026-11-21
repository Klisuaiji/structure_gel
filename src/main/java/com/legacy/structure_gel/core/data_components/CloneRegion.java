package com.legacy.structure_gel.core.data_components;

import com.legacy.structure_gel.api.util.NetworkUtil;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public record CloneRegion(BoundingBox bounds, ResourceKey<Level> levelKey, String playerName)
{
	public static final Codec<CloneRegion> CODEC = RecordCodecBuilder.create(instance ->
	{
		//@formatter:off
		return instance.group(
				BoundingBox.CODEC.fieldOf("bounds").forGetter(CloneRegion::bounds),
				ResourceKey.codec(Registries.DIMENSION).fieldOf("level").forGetter(CloneRegion::levelKey),
				Codec.STRING.fieldOf("player").forGetter(CloneRegion::playerName)).apply(instance, CloneRegion::new);
		//@formatter:on
	});
	
	public static final StreamCodec<FriendlyByteBuf, CloneRegion> STREAM_CODEC = new StreamCodec<>()
	{
		@Override
		public CloneRegion decode(FriendlyByteBuf buff)
		{
			BoundingBox bounds = NetworkUtil.readBoundingBox(buff);
			ResourceKey<Level> levelKey = buff.readResourceKey(Registries.DIMENSION);
			String playerName = buff.readUtf();
			return new CloneRegion(bounds, levelKey, playerName);
		}

		@Override
		public void encode(FriendlyByteBuf buff, CloneRegion clone)
		{
			NetworkUtil.writeBoundingBox(buff, clone.bounds);
			buff.writeResourceKey(clone.levelKey);
			buff.writeUtf(clone.playerName);
		}
	};
}
