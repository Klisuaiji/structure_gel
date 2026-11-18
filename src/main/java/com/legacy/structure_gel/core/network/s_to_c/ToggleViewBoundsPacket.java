package com.legacy.structure_gel.core.network.s_to_c;

import com.legacy.structure_gel.core.StructureGelMod;
import com.legacy.structure_gel.core.network.SGPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.PlayPayloadContext;

public class ToggleViewBoundsPacket implements SGPacketHandler.ModPacket<ToggleViewBoundsPacket, ToggleViewBoundsPacket.Handler> {
   private final boolean shouldViewBounds;

   public ToggleViewBoundsPacket(boolean shouldViewBounds) {
      this.shouldViewBounds = shouldViewBounds;
   }

   public ToggleViewBoundsPacket.Handler handler() {
      return new ToggleViewBoundsPacket.Handler();
   }

   public static class Handler implements SGPacketHandler.PlayHandler<ToggleViewBoundsPacket> {
      @Override
      public ResourceLocation id() {
         return StructureGelMod.locate("toggle_view_bounds");
      }

      public void encoder(ToggleViewBoundsPacket packet, FriendlyByteBuf buff) {
         buff.writeBoolean(packet.shouldViewBounds);
      }

      public ToggleViewBoundsPacket decoder(FriendlyByteBuf buff) {
         boolean shouldViewBounds = buff.readBoolean();
         return new ToggleViewBoundsPacket(shouldViewBounds);
      }

      public void handler(ToggleViewBoundsPacket packet, PlayPayloadContext context) {
         if (FMLEnvironment.dist == Dist.CLIENT) {
            context.workHandler().submitAsync(() -> StructureGelMod.proxy.setViewBounds(packet.shouldViewBounds));
         }
      }
   }
}
