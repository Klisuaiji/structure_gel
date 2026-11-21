package com.legacy.structure_gel.core.network.c_to_s;

import com.legacy.structure_gel.core.StructureGelMod;
import com.legacy.structure_gel.core.capability.level.BuildingToolPlayerData;
import com.legacy.structure_gel.core.data_components.CloneRegion;
import com.legacy.structure_gel.core.item.building_tool.CapturedBlocks;
import com.legacy.structure_gel.core.network.SGPacketHandler;
import com.legacy.structure_gel.core.network.s_to_c.EditClipboardPacket;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record RequestClipboardPacket(CloneRegion cloneRegion) implements CustomPacketPayload
{
	public static final Type<RequestClipboardPacket> TYPE = new Type<>(StructureGelMod.locate("request_clipboard_packet"));

	public static final StreamCodec<RegistryFriendlyByteBuf, RequestClipboardPacket> CODEC = new StreamCodec<>()
	{
		@Override
		public void encode(RegistryFriendlyByteBuf buff, RequestClipboardPacket packet)
		{
			CloneRegion.STREAM_CODEC.encode(buff, packet.cloneRegion);
		}

		@Override
		public RequestClipboardPacket decode(RegistryFriendlyByteBuf buff)
		{
			return new RequestClipboardPacket(CloneRegion.STREAM_CODEC.decode(buff));
		}
	};

	@Override
	public Type<? extends CustomPacketPayload> type()
	{
		return TYPE;
	}

	public static void handler(RequestClipboardPacket packet, IPayloadContext context)
	{
		context.enqueueWork(() ->
		{
			if (!(context.player() instanceof ServerPlayer player))
				return;
			CloneRegion cloneRegion = packet.cloneRegion;
			String owner = cloneRegion.playerName();
			ServerLevel level = player.getServer().getLevel(cloneRegion.levelKey());
			CapturedBlocks captured = BuildingToolPlayerData.get(level, owner).getCapturedBlocks(cloneRegion.bounds(), owner);
			if (captured != null && !packet.cloneRegion.bounds().equals(BoundingBox.infinite()))
				SGPacketHandler.sendToClient(new EditClipboardPacket(owner, cloneRegion.bounds(), captured, BoundingBox.infinite()), player);
		});
	}
}
