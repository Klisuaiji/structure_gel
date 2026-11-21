package com.legacy.structure_gel.core.network.s_to_c;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.legacy.structure_gel.core.StructureGelMod;
import com.legacy.structure_gel.core.client.ClientUtil;

import net.minecraft.core.Registry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SendRegistryKeysToClientPacket(ResourceKey<? extends Registry<?>> registry, Collection<ResourceLocation> keys) implements CustomPacketPayload
{
	public static final Type<SendRegistryKeysToClientPacket> TYPE = new Type<>(StructureGelMod.locate("send_registry_keys"));

	public static final StreamCodec<RegistryFriendlyByteBuf, SendRegistryKeysToClientPacket> CODEC = new StreamCodec<>()
	{
		@Override
		public void encode(RegistryFriendlyByteBuf buff, SendRegistryKeysToClientPacket packet)
		{
			buff.writeResourceLocation(packet.registry.location());
			buff.writeCollection(packet.keys, (b, c) -> b.writeResourceLocation(c));
		}
		
		@Override
		public SendRegistryKeysToClientPacket decode(RegistryFriendlyByteBuf buff)
		{
			return new SendRegistryKeysToClientPacket(ResourceKey.createRegistryKey(buff.readResourceLocation()), buff.readList(b -> b.readResourceLocation()));
		}
	};

	@Override
	public Type<? extends CustomPacketPayload> type()
	{
		return TYPE;
	}

	public static void handler(SendRegistryKeysToClientPacket packet, IPayloadContext context)
	{
		if (FMLEnvironment.dist == Dist.CLIENT)
			context.enqueueWork(() -> handleClient(packet, context));
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@OnlyIn(Dist.CLIENT)
	private static void handleClient(SendRegistryKeysToClientPacket packet, IPayloadContext context)
	{
		ResourceKey<? extends Registry<?>> registry = packet.registry;
		List<ResourceKey<?>> keys = new ArrayList<>(packet.keys.size());
		for (var key : packet.keys)
			keys.add(ResourceKey.create((ResourceKey) registry, key)); // Generic types are like fake friends who don't show up to the hospital when you have surgery. Anyway this is casted to let me publish.
		ClientUtil.REGISTRY_KEYS.put(registry, keys);
	}
}
