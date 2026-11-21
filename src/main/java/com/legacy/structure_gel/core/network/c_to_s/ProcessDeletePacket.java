package com.legacy.structure_gel.core.network.c_to_s;

import com.legacy.structure_gel.core.StructureGelMod;
import com.legacy.structure_gel.core.item.building_tool.BuildingToolItem;
import com.legacy.structure_gel.core.network.SGPacketHandler;
import com.legacy.structure_gel.core.network.s_to_c.PlaySoundPacket;
import com.mojang.datafixers.util.Pair;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ProcessDeletePacket implements SGPacketHandler.ModPacket<ProcessDeletePacket, ProcessDeletePacket.Handler> {
   public ProcessDeletePacket.Handler handler() {
      return new ProcessDeletePacket.Handler();
   }

   public static class Handler implements SGPacketHandler.PlayHandler<ProcessDeletePacket> {
      @Override
      public ResourceLocation id() {
         return StructureGelMod.locate("process_delete");
      }

      public void encoder(ProcessDeletePacket packet, FriendlyByteBuf buff) {
      }

      public ProcessDeletePacket decoder(FriendlyByteBuf buff) {
         return new ProcessDeletePacket();
      }

      public void handler(ProcessDeletePacket packet, IPayloadContext context) {
         context.workHandler()
            .submitAsync(
               () -> {
                  ServerPlayer player = this.serverPlayer(context);
                  if (player != null && !context.level().isEmpty()) {
                     Pair<InteractionHand, ItemStack> tool = BuildingToolItem.getBuildingTool(player);
                     if (tool != null) {
                        ItemStack stack = (ItemStack)tool.getSecond();
                        if (BuildingToolItem.hasToolPermission(stack, player)
                           && BuildingToolItem.getMode(stack).onDelete((Level)context.level().get(), player, stack)) {
                           SGPacketHandler.sendToClient(new PlaySoundPacket((byte)0), player);
                        }
                     }
                  }
               }
            );
      }
   }
}
