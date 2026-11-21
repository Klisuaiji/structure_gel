package com.legacy.structure_gel.core.network.c_to_s;

import com.legacy.structure_gel.core.StructureGelMod;
import com.legacy.structure_gel.core.item.building_tool.ActionHistory;
import com.legacy.structure_gel.core.item.building_tool.BuildingToolBounds;
import com.legacy.structure_gel.core.item.building_tool.BuildingToolItem;
import com.legacy.structure_gel.core.item.building_tool.BuildingToolModes;
import com.legacy.structure_gel.core.item.building_tool.modes.CloneTool;
import com.legacy.structure_gel.core.network.SGPacketHandler;
import com.legacy.structure_gel.core.network.s_to_c.PlaySoundPacket;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ActionHistoryPacket(Action action, InteractionHand hand, boolean playSound) implements CustomPacketPayload
{

	public static final Type<ActionHistoryPacket> TYPE = new Type<>(StructureGelMod.locate("action_history"));

	public static final StreamCodec<RegistryFriendlyByteBuf, ActionHistoryPacket> CODEC = new StreamCodec<>()
	{
		@Override
		public void encode(RegistryFriendlyByteBuf buff, ActionHistoryPacket packet)
		{
			Action.STREAM_CODEC.encode(buff, packet.action);
			buff.writeBoolean(packet.hand == InteractionHand.MAIN_HAND);
			buff.writeBoolean(packet.playSound);
		}

		@Override
		public ActionHistoryPacket decode(RegistryFriendlyByteBuf buff)
		{
			return new ActionHistoryPacket(Action.STREAM_CODEC.decode(buff), buff.readBoolean() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND, buff.readBoolean());
		}
	};

	@Override
	public Type<? extends CustomPacketPayload> type()
	{
		return TYPE;
	}

	public static void handler(ActionHistoryPacket packet, IPayloadContext context)
	{
		context.enqueueWork(() ->
		{
			if (!(context.player() instanceof ServerPlayer player))
				return;

			ActionHistory history = ActionHistory.get(player);
			ItemStack item = player.getItemInHand(packet.hand);
			boolean succeeded = false;
			switch (packet.action)
			{
			case UNDO:
				succeeded = history.undo(player);
				break;
			case REDO:
				succeeded = history.redo(player);
				break;
			case RELEASE_GRABBED_CORNER:
				BuildingToolBounds.releaseGrabbedCorner(item, player);
				break;
			case COPY:
				if (BuildingToolItem.getMode(item) == BuildingToolModes.CLONE)
				{
					CloneTool.saveCapturedBlocks(item, player.level(), player);
					player.displayClientMessage(Component.translatable("info.structure_gel.building_tool.message.copy_region"), true);
					succeeded = true;
				}
				break;
			default:
				break;
			}
			if (succeeded && packet.playSound)
				SGPacketHandler.sendToClient(new PlaySoundPacket((byte) 0), player);
		});
	}

	public static ActionHistoryPacket undo(boolean playSound)
	{
		return new ActionHistoryPacket(Action.UNDO, InteractionHand.MAIN_HAND, playSound);
	}

	public static ActionHistoryPacket redo(boolean playSound)
	{
		return new ActionHistoryPacket(Action.REDO, InteractionHand.MAIN_HAND, playSound);
	}

	public static ActionHistoryPacket releaseGrabbedCorner(InteractionHand hand)
	{
		return new ActionHistoryPacket(Action.RELEASE_GRABBED_CORNER, hand, false);
	}

	public static ActionHistoryPacket copy(InteractionHand hand, boolean playSound)
	{
		return new ActionHistoryPacket(Action.COPY, hand, playSound);
	}

	public static enum Action
	{
		NONE,
		UNDO,
		REDO,
		RELEASE_GRABBED_CORNER,
		COPY;

		public static final StreamCodec<FriendlyByteBuf, Action> STREAM_CODEC = StreamCodec.of((b, a) -> b.writeInt(a.ordinal()), (b) -> Action.get(b.readInt()));

		public static Action get(int id)
		{
			Action[] actions = Action.values();
			if (id > -1 && id < actions.length)
				return actions[id];
			return Action.NONE;
		}
	}
}
