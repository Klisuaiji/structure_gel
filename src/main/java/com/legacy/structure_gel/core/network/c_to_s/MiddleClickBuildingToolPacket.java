package com.legacy.structure_gel.core.network.c_to_s;

import com.legacy.structure_gel.core.StructureGelMod;
import com.legacy.structure_gel.core.network.SGPacketHandler;
import com.legacy.structure_gel.core.registry.SGRegistry;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record MiddleClickBuildingToolPacket(InteractionHand hand, BlockPos pos)
   implements SGPacketHandler.ModPacket<MiddleClickBuildingToolPacket, MiddleClickBuildingToolPacket.Handler> {
   public MiddleClickBuildingToolPacket.Handler handler() {
      return new MiddleClickBuildingToolPacket.Handler();
   }

   public static class Handler implements SGPacketHandler.PlayHandler<MiddleClickBuildingToolPacket> {
      @Override
      public ResourceLocation id() {
         return StructureGelMod.locate("middle_click_building_tool");
      }

      public void encoder(MiddleClickBuildingToolPacket packet, FriendlyByteBuf buff) {
         buff.writeBoolean(packet.hand == InteractionHand.MAIN_HAND);
         buff.writeBoolean(packet.pos != null);
         if (packet.pos != null) {
            buff.writeBlockPos(packet.pos);
         }
      }

      public MiddleClickBuildingToolPacket decoder(FriendlyByteBuf buff) {
         InteractionHand hand = buff.readBoolean() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
         boolean isPosPresent = buff.readBoolean();
         BlockPos pos = isPosPresent ? buff.readBlockPos() : null;
         return new MiddleClickBuildingToolPacket(hand, pos);
      }

      public void handler(MiddleClickBuildingToolPacket packet, IPayloadContext context) {
         context.enqueueWork(() -> {
            ServerPlayer sender = this.serverPlayer(context);
            if (sender != null) {
               ItemStack stack = sender.getItemInHand(packet.hand);
               if (stack.is(SGRegistry.Items.BUILDING_TOOL.get())) {
                  SGRegistry.Items.BUILDING_TOOL.get().onMiddleClick(stack, sender, packet.pos);
                  sender.swing(packet.hand);
               }
            }
         });
      }
   }
}
