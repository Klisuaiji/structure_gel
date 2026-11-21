package com.legacy.structure_gel.core.network.s_to_c;

import com.legacy.structure_gel.core.StructureGelMod;
import com.legacy.structure_gel.core.client.screen.building_tool.BuildingToolScreen;
import com.legacy.structure_gel.core.network.SGPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record PlaySoundPacket(byte sound) implements SGPacketHandler.ModPacket<PlaySoundPacket, PlaySoundPacket.Handler> {
   public PlaySoundPacket.Handler handler() {
      return new PlaySoundPacket.Handler();
   }

   public static class Handler implements SGPacketHandler.PlayHandler<PlaySoundPacket> {
      public static final byte BUTTON_CLICK = 0;

      @Override
      public ResourceLocation id() {
         return StructureGelMod.locate("play_sound");
      }

      public void encoder(PlaySoundPacket packet, FriendlyByteBuf buff) {
         buff.writeByte(packet.sound);
      }

      public PlaySoundPacket decoder(FriendlyByteBuf buff) {
         return new PlaySoundPacket(buff.readByte());
      }

      public void handler(PlaySoundPacket packet, IPayloadContext context) {
         if (FMLEnvironment.dist == Dist.CLIENT && packet.sound == 0) {
            context.enqueueWork(() -> BuildingToolScreen.playClickSound());
         }
      }
   }
}
