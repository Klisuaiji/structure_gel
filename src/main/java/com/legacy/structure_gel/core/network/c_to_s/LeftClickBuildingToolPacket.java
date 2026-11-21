package com.legacy.structure_gel.core.network.c_to_s;

import javax.annotation.Nullable;

import com.legacy.structure_gel.core.StructureGelMod;
import com.legacy.structure_gel.core.registry.SGRegistry;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record LeftClickBuildingToolPacket(InteractionHand hand, @Nullable BlockPos pos, Direction clickedFace, boolean isHolding) implements CustomPacketPayload
{
	public static final Type<LeftClickBuildingToolPacket> TYPE = new Type<>(StructureGelMod.locate("left_click_building_tool"));

	public static final StreamCodec<RegistryFriendlyByteBuf, LeftClickBuildingToolPacket> CODEC = new StreamCodec<>()
	{
		@Override
		public void encode(RegistryFriendlyByteBuf buff, LeftClickBuildingToolPacket packet)
		{
			buff.writeBoolean(packet.hand == InteractionHand.MAIN_HAND);
			buff.writeBoolean(packet.pos != null);
			if (packet.pos != null)
				buff.writeBlockPos(packet.pos);
			buff.writeEnum(packet.clickedFace);
			buff.writeBoolean(packet.isHolding);
		}
		
		@Override
		public LeftClickBuildingToolPacket decode(RegistryFriendlyByteBuf buff)
		{
			InteractionHand hand = buff.readBoolean() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
			boolean isPosPresent = buff.readBoolean();
			BlockPos pos = isPosPresent ? buff.readBlockPos() : null;
			Direction clickedFace = buff.readEnum(Direction.class);
			boolean isHolding = buff.readBoolean();
			return new LeftClickBuildingToolPacket(hand, pos, clickedFace, isHolding);
		}
	};

	@Override
	public Type<? extends CustomPacketPayload> type()
	{
		return TYPE;
	}

	public static void handler(LeftClickBuildingToolPacket packet, IPayloadContext context)
	{
		context.enqueueWork(() ->
		{
			if (packet.pos == null)
				return;
			if (!(context.player() instanceof ServerPlayer player))
				return;
			ItemStack stack = player.getItemInHand(packet.hand);
			if (stack.is(SGRegistry.Items.BUILDING_TOOL.get()))
				SGRegistry.Items.BUILDING_TOOL.get().onLeftClick(stack, player, packet.pos, packet.clickedFace, packet.isHolding);
		});
	}
}
