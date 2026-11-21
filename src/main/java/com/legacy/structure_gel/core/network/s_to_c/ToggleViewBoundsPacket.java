package com.legacy.structure_gel.core.network.s_to_c;

import com.legacy.structure_gel.core.StructureGelMod;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ToggleViewBoundsPacket(boolean shouldViewBounds) implements CustomPacketPayload
{
	public static final Type<ToggleViewBoundsPacket> TYPE = new Type<>(StructureGelMod.locate("toggle_view_bounds"));

	public static final StreamCodec<RegistryFriendlyByteBuf, ToggleViewBoundsPacket> CODEC = new StreamCodec<>()
	{
		@Override
		public void encode(RegistryFriendlyByteBuf buff, ToggleViewBoundsPacket packet)
		{
			buff.writeBoolean(packet.shouldViewBounds);
		}
		
		@Override
		public ToggleViewBoundsPacket decode(RegistryFriendlyByteBuf buff)
		{
			boolean shouldViewBounds = buff.readBoolean();
			return new ToggleViewBoundsPacket(shouldViewBounds);
		}
	};

	@Override
	public Type<? extends CustomPacketPayload> type()
	{
		return TYPE;
	}

	public static void handler(ToggleViewBoundsPacket packet, IPayloadContext context)
	{
		if (FMLEnvironment.dist == Dist.CLIENT)
			context.enqueueWork(() -> StructureGelMod.proxy.setViewBounds(packet.shouldViewBounds));
	}
}
