package com.legacy.structure_gel.core.network;

import com.legacy.structure_gel.core.network.bi_directional.UpdateBuildingToolPacket;
import com.legacy.structure_gel.core.network.c_to_s.ActionHistoryPacket;
import com.legacy.structure_gel.core.network.c_to_s.ExportPalettePacket;
import com.legacy.structure_gel.core.network.c_to_s.LeftClickBuildingToolPacket;
import com.legacy.structure_gel.core.network.c_to_s.MiddleClickBuildingToolPacket;
import com.legacy.structure_gel.core.network.c_to_s.ProcessDeletePacket;
import com.legacy.structure_gel.core.network.c_to_s.RequestClipboardPacket;
import com.legacy.structure_gel.core.network.c_to_s.RequestRegistryKeysPacket;
import com.legacy.structure_gel.core.network.c_to_s.RightClickBuildingToolPacket;
import com.legacy.structure_gel.core.network.c_to_s.UpdateDataHandlerPacket;
import com.legacy.structure_gel.core.network.c_to_s.UpdateDynamicSpawnerPacket;
import com.legacy.structure_gel.core.network.s_to_c.EditClipboardPacket;
import com.legacy.structure_gel.core.network.s_to_c.PlaySoundPacket;
import com.legacy.structure_gel.core.network.s_to_c.SendRegistryKeysToClientPacket;
import com.legacy.structure_gel.core.network.s_to_c.SendStructureInfoToClientPacket;
import com.legacy.structure_gel.core.network.s_to_c.ToggleViewBoundsPacket;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

@EventBusSubscriber(bus = Bus.MOD)
public class SGPacketHandler
{
	@SubscribeEvent
	public static void registerPayloads(RegisterPayloadHandlersEvent event)
	{
		PayloadRegistrar registrar = event.registrar("10");

		// s to c
		registrar.playToClient(EditClipboardPacket.TYPE, EditClipboardPacket.CODEC, EditClipboardPacket::handler);
		registrar.playToClient(PlaySoundPacket.TYPE, PlaySoundPacket.CODEC, PlaySoundPacket::handler);
		registrar.playToClient(SendRegistryKeysToClientPacket.TYPE, SendRegistryKeysToClientPacket.CODEC, SendRegistryKeysToClientPacket::handler);
		registrar.playToClient(SendStructureInfoToClientPacket.TYPE, SendStructureInfoToClientPacket.CODEC, SendStructureInfoToClientPacket::handler);
		registrar.playToClient(ToggleViewBoundsPacket.TYPE, ToggleViewBoundsPacket.CODEC, ToggleViewBoundsPacket::handler);

		// c to s
		registrar.playToServer(ActionHistoryPacket.TYPE, ActionHistoryPacket.CODEC, ActionHistoryPacket::handler);
		registrar.playToServer(LeftClickBuildingToolPacket.TYPE, LeftClickBuildingToolPacket.CODEC, LeftClickBuildingToolPacket::handler);
		registrar.playToServer(RightClickBuildingToolPacket.TYPE, RightClickBuildingToolPacket.CODEC, RightClickBuildingToolPacket::handler);
		registrar.playToServer(MiddleClickBuildingToolPacket.TYPE, MiddleClickBuildingToolPacket.CODEC, MiddleClickBuildingToolPacket::handler);
		registrar.playToServer(RequestClipboardPacket.TYPE, RequestClipboardPacket.CODEC, RequestClipboardPacket::handler);
		registrar.playToServer(RequestRegistryKeysPacket.TYPE, RequestRegistryKeysPacket.CODEC, RequestRegistryKeysPacket::handler);
		registrar.playToServer(UpdateDynamicSpawnerPacket.TYPE, UpdateDynamicSpawnerPacket.CODEC, UpdateDynamicSpawnerPacket::handler);
		registrar.playToServer(UpdateDataHandlerPacket.TYPE, UpdateDataHandlerPacket.CODEC, UpdateDataHandlerPacket::handler);
		registrar.playToServer(ProcessDeletePacket.TYPE, ProcessDeletePacket.CODEC, ProcessDeletePacket::handler);
		registrar.playToServer(ExportPalettePacket.TYPE, ExportPalettePacket.CODEC, ExportPalettePacket::handler);
		
		// bi directional
		registrar.playBidirectional(UpdateBuildingToolPacket.TYPE, UpdateBuildingToolPacket.CODEC, UpdateBuildingToolPacket::handler);
	}

	/**
	 * Server -> Client player passed
	 */
	public static void sendToClient(CustomPacketPayload packet, ServerPlayer serverPlayer)
	{
		if (!(serverPlayer instanceof FakePlayer))
			PacketDistributor.sendToPlayer(serverPlayer, packet);
	}

	/**
	 * Server -> Clients in same world
	 */
	public static void sendToClients(CustomPacketPayload packet, ServerLevel level)
	{
		level.players().forEach(player -> sendToClient(packet, player));
	}

	/**
	 * Server -> Clients in same world within 256 blocks
	 */
	public static void sendToClients(CustomPacketPayload packet, ServerLevel level, BlockPos pos)
	{
		sendToClients(packet, level, pos, 256);
	}

	/**
	 * Server -> Clients in same world within range
	 */
	public static void sendToClients(CustomPacketPayload packet, ServerLevel level, BlockPos pos, int range)
	{
		Vec3 centerPos = Vec3.atCenterOf(pos);
		level.players().forEach(player ->
		{
			if (centerPos.distanceTo(player.position()) <= range)
				sendToClient(packet, player);
		});
	}

	/**
	 * Server -> Clients in every level
	 */
	public static void sendToClients(CustomPacketPayload packet)
	{
		MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
		if (server != null)
			server.getPlayerList().getPlayers().forEach(player -> sendToClient(packet, player));
	}

	/**
	 * Client to server
	 *
	 * @param packet
	 */
	public static void sendToServer(CustomPacketPayload packet)
	{
		PacketDistributor.sendToServer(packet);
	}
}
