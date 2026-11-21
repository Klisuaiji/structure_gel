package com.legacy.structure_gel.core.network.c_to_s;

import java.util.ArrayList;
import java.util.List;

import com.legacy.structure_gel.core.StructureGelMod;
import com.legacy.structure_gel.core.network.SGPacketHandler;
import com.legacy.structure_gel.core.network.s_to_c.SendRegistryKeysToClientPacket;

import net.minecraft.core.Registry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.flag.FeatureElement;
import net.minecraft.world.flag.FeatureFlagSet;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record RequestRegistryKeysPacket(ResourceKey<? extends Registry<?>> registry) implements CustomPacketPayload
{
	public static final Type<RequestRegistryKeysPacket> TYPE = new Type<>(StructureGelMod.locate("request_registry_keys"));

	public static final StreamCodec<RegistryFriendlyByteBuf, RequestRegistryKeysPacket> CODEC = new StreamCodec<>()
	{
		@Override
		public void encode(RegistryFriendlyByteBuf buff, RequestRegistryKeysPacket packet)
		{
			buff.writeResourceKey(packet.registry);
		}

		@Override
		public RequestRegistryKeysPacket decode(RegistryFriendlyByteBuf buff)
		{
			return new RequestRegistryKeysPacket(buff.readRegistryKey());
		}
	};

	@Override
	public Type<? extends CustomPacketPayload> type()
	{
		return TYPE;
	}

	public static void handler(RequestRegistryKeysPacket packet, IPayloadContext context)
	{
		context.enqueueWork(() ->
		{
			if (!(context.player() instanceof ServerPlayer player))
				return;
			ServerLevel level = player.serverLevel();
			if (level != null)
			{
				var opRegistry = level.registryAccess().lookup(packet.registry);
				if (opRegistry.isPresent())
				{
					Registry<?> registry = opRegistry.get();
					List<ResourceLocation> keys = new ArrayList<>(registry.keySet().size());
					FeatureFlagSet featureFlags = level.enabledFeatures();
					for (var entry : registry.entrySet())
					{
						if (!(entry.getValue() instanceof FeatureElement feature) || feature.isEnabled(featureFlags))
						{
							keys.add(entry.getKey().location());
						}
					}
					SGPacketHandler.sendToClient(new SendRegistryKeysToClientPacket(packet.registry, keys), player);
				}
			}
		});
	}
}
