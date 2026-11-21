package com.legacy.structure_gel.core.network.c_to_s;

import com.legacy.structure_gel.core.StructureGelMod;
import com.legacy.structure_gel.core.item.building_tool.BuildingToolItem;
import com.legacy.structure_gel.core.network.SGPacketHandler;
import com.legacy.structure_gel.core.network.s_to_c.PlaySoundPacket;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ProcessDeletePacket() implements CustomPacketPayload
{
	public static final Type<ProcessDeletePacket> TYPE = new Type<>(StructureGelMod.locate("process_delete"));

	public static final StreamCodec<RegistryFriendlyByteBuf, ProcessDeletePacket> CODEC = new StreamCodec<>()
	{
		@Override
		public void encode(RegistryFriendlyByteBuf buff, ProcessDeletePacket packet)
		{

		}

		@Override
		public ProcessDeletePacket decode(RegistryFriendlyByteBuf buff)
		{
			return new ProcessDeletePacket();
		}
	};

	@Override
	public Type<? extends CustomPacketPayload> type()
	{
		return TYPE;
	}

	public static void handler(ProcessDeletePacket packet, IPayloadContext context)
	{
		context.enqueueWork(() ->
		{
			if (!(context.player() instanceof ServerPlayer player))
				return;
			if (player == null || player.level() == null)
				return;
			var tool = BuildingToolItem.getBuildingTool(player);
			if (tool != null)
			{
				ItemStack stack = tool.getSecond();
				if (BuildingToolItem.hasToolPermission(stack, player) && BuildingToolItem.getMode(stack).onDelete(player.level(), player, stack))
				{
					SGPacketHandler.sendToClient(new PlaySoundPacket((byte) 0), player);
				}
			}
		});
	}
}
