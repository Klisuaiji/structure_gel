package com.legacy.structure_gel.core.data_components;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record BuildingToolProperties(Map<String, String> properties)
{
	public static final Codec<BuildingToolProperties> CODEC = RecordCodecBuilder.create(instance ->
	{
		return instance.group(Codec.unboundedMap(Codec.STRING, Codec.STRING).fieldOf("properties").forGetter(r -> r.properties)).apply(instance, BuildingToolProperties::new);
	});

	public static final StreamCodec<FriendlyByteBuf, BuildingToolProperties> STREAM_CODEC = new StreamCodec<>()
	{
		@Override
		public BuildingToolProperties decode(FriendlyByteBuf buff)
		{
			Map<String, String> props = new HashMap<>();
			int size = buff.readInt();
			for (int i = 0; i < size; i++)
			{
				String key = buff.readUtf();
				String value = buff.readUtf();
				props.put(key, value);
			}
			return new BuildingToolProperties(props);
		}

		@Override
		public void encode(FriendlyByteBuf buff, BuildingToolProperties value)
		{
			Map<String, String> props = value.properties;
			int size = props.size();
			buff.writeInt(size);
			for (var entry : props.entrySet())
			{
				buff.writeUtf(entry.getKey());
				buff.writeUtf(entry.getValue());
			}
		}
	};

	public static final BuildingToolProperties DEFAULT = new BuildingToolProperties(Map.of());

	public boolean contains(String key)
	{
		return this.properties.containsKey(key);
	}

	@Nullable
	public String get(String key)
	{
		return this.properties.get(key);
	}

	public Map<String, String> getProperties()
	{
		return new HashMap<>(this.properties);
	}

	public BuildingToolProperties withValue(String key, String value)
	{
		Map<String, String> props = this.getProperties();
		props.put(key, value);
		return new BuildingToolProperties(props);
	}
}
