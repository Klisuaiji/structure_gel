package com.legacy.structure_gel.core.network.s_to_c;

import com.legacy.structure_gel.core.StructureGelMod;
import com.legacy.structure_gel.core.capability.GelCapability;
import com.legacy.structure_gel.core.capability.entity.GelEntity;
import com.legacy.structure_gel.core.network.SGPacketHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.PlayPayloadContext;

public class UpdateGelPlayerPacket implements SGPacketHandler.ModPacket<UpdateGelPlayerPacket, UpdateGelPlayerPacket.Handler> {
   protected final GelEntity gelEntity;

   public UpdateGelPlayerPacket(GelEntity gelEntity) {
      this.gelEntity = gelEntity;
   }

   public UpdateGelPlayerPacket.Handler handler() {
      return new UpdateGelPlayerPacket.Handler();
   }

   public static class Handler implements SGPacketHandler.PlayHandler<UpdateGelPlayerPacket> {
      @Override
      public ResourceLocation id() {
         return StructureGelMod.locate("update_gel_player");
      }

      public void encoder(UpdateGelPlayerPacket packet, FriendlyByteBuf buff) {
         CompoundTag tag = GelEntity.Serializer.INSTANCE.write(packet.gelEntity);
         buff.writeNbt(tag);
      }

      public UpdateGelPlayerPacket decoder(FriendlyByteBuf buff) {
         GelEntity gelEntity = GelEntity.Serializer.INSTANCE.read(null, buff.readNbt());
         return new UpdateGelPlayerPacket(gelEntity);
      }

      public void handler(UpdateGelPlayerPacket packet, PlayPayloadContext context) {
         if (FMLEnvironment.dist == Dist.CLIENT) {
            context.workHandler().submitAsync(() -> handleClient(packet));
         }
      }

      @OnlyIn(Dist.CLIENT)
      private static void handleClient(UpdateGelPlayerPacket packet) {
         Minecraft mc = Minecraft.getInstance();
         GelCapability.ifPresent(mc.player, e -> e.copyFrom(packet.gelEntity));
      }
   }
}
