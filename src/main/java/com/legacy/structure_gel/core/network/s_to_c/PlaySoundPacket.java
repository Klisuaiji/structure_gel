package com.legacy.structure_gel.core.network.s_to_c;

import com.legacy.structure_gel.core.StructureGelMod;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record PlaySoundPacket(byte sound) implements CustomPacketPayload
{
	public static final Type<PlaySoundPacket> TYPE = new Type<>(StructureGelMod.locate("play_sound"));

	public static final StreamCodec<RegistryFriendlyByteBuf, PlaySoundPacket> CODEC = new StreamCodec<>()
	{
		@Override
		public void encode(RegistryFriendlyByteBuf buff, PlaySoundPacket packet)
		{
			buff.writeByte(packet.sound);
		}
		
		@Override
		public PlaySoundPacket decode(RegistryFriendlyByteBuf buff)
		{
			return new PlaySoundPacket(buff.readByte());
		}
	};

	@Override
	public Type<? extends CustomPacketPayload> type()
	{
		return TYPE;
	}

	public static void handler(PlaySoundPacket packet, IPayloadContext context)
	{
		if (FMLEnvironment.dist == Dist.CLIENT && packet.sound == 0)
			context.enqueueWork(() -> com.legacy.structure_gel.core.client.screen.building_tool.BuildingToolScreen.playClickSound());
	}
}
