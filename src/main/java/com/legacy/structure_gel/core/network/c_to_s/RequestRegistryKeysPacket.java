package com.legacy.structure_gel.core.network.c_to_s;

import com.legacy.structure_gel.core.StructureGelMod;
import com.legacy.structure_gel.core.network.SGPacketHandler;
import com.legacy.structure_gel.core.network.s_to_c.SendRegistryKeysToClientPacket;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Map.Entry;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.flag.FeatureElement;
import net.minecraft.world.flag.FeatureFlagSet;
import net.neoforged.neoforge.network.handling.PlayPayloadContext;

public record RequestRegistryKeysPacket(ResourceKey<? extends Registry<?>> registry)
   implements SGPacketHandler.ModPacket<RequestRegistryKeysPacket, RequestRegistryKeysPacket.Handler> {
   public RequestRegistryKeysPacket.Handler handler() {
      return new RequestRegistryKeysPacket.Handler();
   }

   public static class Handler implements SGPacketHandler.PlayHandler<RequestRegistryKeysPacket> {
      @Override
      public ResourceLocation id() {
         return StructureGelMod.locate("request_registry_keys");
      }

      public void encoder(RequestRegistryKeysPacket packet, FriendlyByteBuf buff) {
         buff.writeResourceKey(packet.registry);
      }

      public RequestRegistryKeysPacket decoder(FriendlyByteBuf buff) {
         return new RequestRegistryKeysPacket(buff.readRegistryKey());
      }

      public void handler(RequestRegistryKeysPacket packet, PlayPayloadContext context) {
         context.workHandler().submitAsync(() -> {
            ServerLevel level = this.serverLevel(context);
            if (level != null) {
               Optional<Registry<Object>> opRegistry = level.registryAccess().registry(packet.registry);
               if (opRegistry.isPresent()) {
                  Registry<?> registry = opRegistry.get();
                  List<ResourceLocation> keys = new ArrayList<>(registry.keySet().size());
                  FeatureFlagSet featureFlags = level.enabledFeatures();

                  for (Entry<? extends ResourceKey<?>, ?> entry : registry.entrySet()) {
                     if (!(entry.getValue() instanceof FeatureElement feature && !feature.isEnabled(featureFlags))) {
                        keys.add(entry.getKey().location());
                     }
                  }

                  SGPacketHandler.sendToClient(new SendRegistryKeysToClientPacket(packet.registry, keys), this.serverPlayer(context));
               }
            }
         });
      }
   }
}
