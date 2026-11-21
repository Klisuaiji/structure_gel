package com.legacy.structure_gel.core.network.c_to_s;

import com.legacy.structure_gel.core.StructureGelMod;
import com.legacy.structure_gel.core.network.SGPacketHandler;
import com.legacy.structure_gel.core.registry.SGRegistry;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record LeftClickBuildingToolPacket(InteractionHand hand, BlockPos pos, Direction clickedFace, boolean isHolding)
   implements SGPacketHandler.ModPacket<LeftClickBuildingToolPacket, LeftClickBuildingToolPacket.Handler> {
   public LeftClickBuildingToolPacket.Handler handler() {
      return new LeftClickBuildingToolPacket.Handler();
   }

   public static class Handler implements SGPacketHandler.PlayHandler<LeftClickBuildingToolPacket> {
      @Override
      public ResourceLocation id() {
         return StructureGelMod.locate("left_click_building_tool");
      }

      public void encoder(LeftClickBuildingToolPacket packet, FriendlyByteBuf buff) {
         buff.writeBoolean(packet.hand == InteractionHand.MAIN_HAND);
         buff.writeBoolean(packet.pos != null);
         if (packet.pos != null) {
            buff.writeBlockPos(packet.pos);
         }

         buff.writeEnum(packet.clickedFace);
         buff.writeBoolean(packet.isHolding);
      }

      public LeftClickBuildingToolPacket decoder(FriendlyByteBuf buff) {
         InteractionHand hand = buff.readBoolean() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
         boolean isPosPresent = buff.readBoolean();
         BlockPos pos = isPosPresent ? buff.readBlockPos() : null;
         Direction clickedFace = (Direction)buff.readEnum(Direction.class);
         boolean isHolding = buff.readBoolean();
         return new LeftClickBuildingToolPacket(hand, pos, clickedFace, isHolding);
      }

      public void handler(LeftClickBuildingToolPacket packet, IPayloadContext context) {
         context.enqueueWork(() -> {
            if (packet.pos != null) {
               ServerPlayer sender = this.serverPlayer(context);
               if (sender != null) {
                  ItemStack stack = sender.getItemInHand(packet.hand);
                  if (stack.is(SGRegistry.Items.BUILDING_TOOL.get())) {
                     SGRegistry.Items.BUILDING_TOOL.get().onLeftClick(stack, sender, packet.pos, packet.clickedFace, packet.isHolding);
                  }
               }
            }
         });
      }
   }
}
