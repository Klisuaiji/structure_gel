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
import com.legacy.structure_gel.core.network.s_to_c.UpdateGelPlayerPacket;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlerEvent;
import net.neoforged.neoforge.network.handling.ConfigurationPayloadContext;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.IPayloadRegistrar;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

@EventBusSubscriber(bus = Bus.MOD)
public class SGPacketHandler {
   @SubscribeEvent
   public static void registerPayloads(RegisterPayloadHandlerEvent event) {
      IPayloadRegistrar r = event.registrar("structure_gel").versioned("9").optional();
      playPacket(r, new EditClipboardPacket.Handler());
      playPacket(r, new PlaySoundPacket.Handler());
      playPacket(r, new SendRegistryKeysToClientPacket.Handler());
      playPacket(r, new SendStructureInfoToClientPacket.Handler());
      playPacket(r, new ToggleViewBoundsPacket.Handler());
      playPacket(r, new UpdateGelPlayerPacket.Handler());
      playPacket(r, new ActionHistoryPacket.Handler());
      playPacket(r, new LeftClickBuildingToolPacket.Handler());
      playPacket(r, new RightClickBuildingToolPacket.Handler());
      playPacket(r, new MiddleClickBuildingToolPacket.Handler());
      playPacket(r, new RequestClipboardPacket.Handler());
      playPacket(r, new RequestRegistryKeysPacket.Handler());
      playPacket(r, new UpdateDynamicSpawnerPacket.Handler());
      playPacket(r, new UpdateDataHandlerPacket.Handler());
      playPacket(r, new ProcessDeletePacket.Handler());
      playPacket(r, new ExportPalettePacket.Handler());
      playPacket(r, new UpdateBuildingToolPacket.Handler());
   }

   private static <T extends SGPacketHandler.ModPacket<T, ?>> void playPacket(IPayloadRegistrar registrar, SGPacketHandler.PlayHandler<T> handler) {
      registrar.play(handler.id(), handler::decoder, handler::handlePacket);
   }

   private static <T extends SGPacketHandler.ModPacket<T, ?>> void configurationPacket(
      IPayloadRegistrar registrar, SGPacketHandler.ConfigurationHandler<T> handler
   ) {
      registrar.configuration(handler.id(), handler::decoder, handler::handlePacket);
   }

   public static void sendToClient(CustomPacketPayload packet, ServerPlayer serverPlayer) {
      if (!(serverPlayer instanceof FakePlayer)) {
         serverPlayer.connection.send(packet);
      }
   }

   public static void sendToClients(CustomPacketPayload packet, ServerLevel level) {
      level.players().forEach(player -> sendToClient(packet, player));
   }

   public static void sendToClients(CustomPacketPayload packet, ServerLevel level, BlockPos pos) {
      sendToClients(packet, level, pos, 256);
   }

   public static void sendToClients(CustomPacketPayload packet, ServerLevel level, BlockPos pos, int range) {
      Vec3 centerPos = Vec3.atCenterOf(pos);
      level.players().forEach(player -> {
         if (centerPos.distanceTo(player.position()) <= range) {
            sendToClient(packet, player);
         }
      });
   }

   public static void sendToClients(CustomPacketPayload packet) {
      MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
      if (server != null) {
         server.getPlayerList().getPlayers().forEach(player -> sendToClient(packet, player));
      }
   }

   public static void sendToServer(CustomPacketPayload packet) {
      Minecraft.getInstance().getConnection().send(packet);
   }

   public interface ConfigurationHandler<T extends SGPacketHandler.ModPacket<T, ?>> extends SGPacketHandler.Handler<T, ConfigurationPayloadContext> {
      private void handlePacket(T packet, ConfigurationPayloadContext context) {
         if (context.flow().isServerbound()) {
            this.handleServerbound(packet, context);
         } else {
            this.handleClientbound(packet, context);
         }
      }
   }

   protected interface Handler<T extends SGPacketHandler.ModPacket<T, ?>, C extends IPayloadContext> extends SGPacketHandler.Serializer<T> {
      void handler(T var1, C var2);

      @OnlyIn(Dist.CLIENT)
      default void handleClientbound(T packet, C context) {
         this.handler(packet, context);
      }

      default void handleServerbound(T packet, C context) {
         this.handler(packet, context);
      }
   }

   public interface ModPacket<T extends SGPacketHandler.ModPacket<T, ?>, H extends SGPacketHandler.Handler<T, ?>> extends CustomPacketPayload {
      H handler();

      default ResourceLocation id() {
         return this.handler().id();
      }

      default void write(FriendlyByteBuf buff) {
         this.handler().encoder(this.getSelf(), buff);
      }

      default T getSelf() {
         return (T)this;
      }
   }

   public interface PlayHandler<T extends SGPacketHandler.ModPacket<T, ?>> extends SGPacketHandler.Handler<T, IPayloadContext> {
      private void handlePacket(T packet, IPayloadContext context) {
         if (context.flow().isServerbound()) {
            this.handleServerbound(packet, context);
         } else {
            this.handleClientbound(packet, context);
         }
      }
   }

   protected interface Serializer<T extends SGPacketHandler.ModPacket<T, ?>> {
      ResourceLocation id();

      void encoder(T var1, FriendlyByteBuf var2);

      T decoder(FriendlyByteBuf var1);

      @Nullable
      default MinecraftServer server(IPayloadContext context) {
         return context.level().<MinecraftServer>map(Level::getServer).orElseGet(ServerLifecycleHooks::getCurrentServer);
      }

      @Nullable
      default ServerPlayer serverPlayer(IPayloadContext context) {
         Optional<Player> player = context.player();
         return player.isPresent() && player.get() instanceof ServerPlayer s ? s : null;
      }

      @Nullable
      default ServerLevel serverLevel(IPayloadContext context) {
         Optional<Level> level = context.level();
         return level.isPresent() && level.get() instanceof ServerLevel s ? s : null;
      }
   }
}
