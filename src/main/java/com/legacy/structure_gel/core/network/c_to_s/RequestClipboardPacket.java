package com.legacy.structure_gel.core.network.c_to_s;

import com.legacy.structure_gel.api.util.NetworkUtil;
import com.legacy.structure_gel.core.StructureGelMod;
import com.legacy.structure_gel.core.capability.level.BuildingToolPlayerData;
import com.legacy.structure_gel.core.item.building_tool.CapturedBlocks;
import com.legacy.structure_gel.core.network.SGPacketHandler;
import com.legacy.structure_gel.core.network.s_to_c.EditClipboardPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.neoforge.network.handling.PlayPayloadContext;

public class RequestClipboardPacket implements SGPacketHandler.ModPacket<RequestClipboardPacket, RequestClipboardPacket.Handler> {
   final BoundingBox bounds;

   public RequestClipboardPacket(BoundingBox bounds) {
      this.bounds = bounds;
   }

   public RequestClipboardPacket.Handler handler() {
      return new RequestClipboardPacket.Handler();
   }

   public static class Handler implements SGPacketHandler.PlayHandler<RequestClipboardPacket> {
      @Override
      public ResourceLocation id() {
         return StructureGelMod.locate("request_clipboard_packet");
      }

      public void encoder(RequestClipboardPacket packet, FriendlyByteBuf buff) {
         NetworkUtil.writeBoundingBox(buff, packet.bounds);
      }

      public RequestClipboardPacket decoder(FriendlyByteBuf buff) {
         return new RequestClipboardPacket(NetworkUtil.readBoundingBox(buff));
      }

      public void handler(RequestClipboardPacket packet, PlayPayloadContext context) {
         context.workHandler().submitAsync(() -> {
            ServerPlayer player = this.serverPlayer(context);
            if (player != null) {
               ServerLevel level = player.serverLevel();
               CapturedBlocks captured = BuildingToolPlayerData.get(level, player.getGameProfile().getName()).getCapturedBlocks(packet.bounds);
               if (captured != null && !packet.bounds.equals(BoundingBox.infinite())) {
                  SGPacketHandler.sendToClient(new EditClipboardPacket(level, packet.bounds, captured, BoundingBox.infinite()), player);
               }
            }
         });
      }
   }
}
