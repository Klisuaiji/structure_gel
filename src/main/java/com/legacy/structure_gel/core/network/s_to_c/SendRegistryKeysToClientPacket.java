package com.legacy.structure_gel.core.network.s_to_c;

import com.legacy.structure_gel.core.StructureGelMod;
import com.legacy.structure_gel.core.client.ClientUtil;
import com.legacy.structure_gel.core.network.SGPacketHandler;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SendRegistryKeysToClientPacket(ResourceKey<? extends Registry<?>> registry, Collection<ResourceLocation> keys)
   implements SGPacketHandler.ModPacket<SendRegistryKeysToClientPacket, SendRegistryKeysToClientPacket.Handler> {
   public SendRegistryKeysToClientPacket.Handler handler() {
      return new SendRegistryKeysToClientPacket.Handler();
   }

   public static class Handler implements SGPacketHandler.PlayHandler<SendRegistryKeysToClientPacket> {
      @Override
      public ResourceLocation id() {
         return StructureGelMod.locate("send_registry_keys");
      }

      public void encoder(SendRegistryKeysToClientPacket packet, FriendlyByteBuf buff) {
         buff.writeResourceLocation(packet.registry.location());
         buff.writeCollection(packet.keys, (b, c) -> b.writeResourceLocation(c));
      }

      public SendRegistryKeysToClientPacket decoder(FriendlyByteBuf buff) {
         return new SendRegistryKeysToClientPacket(ResourceKey.createRegistryKey(buff.readResourceLocation()), buff.readList(b -> b.readResourceLocation()));
      }

      public void handler(SendRegistryKeysToClientPacket packet, IPayloadContext context) {
         if (FMLEnvironment.dist == Dist.CLIENT) {
            context.enqueueWork(() -> handleClient(packet, context));
         }
      }

      @OnlyIn(Dist.CLIENT)
      private static void handleClient(SendRegistryKeysToClientPacket packet, IPayloadContext context) {
         ResourceKey<? extends Registry<?>> registry = packet.registry;
         List<ResourceKey<?>> keys = new ArrayList<>(packet.keys.size());

         for (ResourceLocation key : packet.keys) {
            keys.add(ResourceKey.create(registry, key));
         }

         ClientUtil.REGISTRY_KEYS.put(registry, keys);
      }
   }
}
