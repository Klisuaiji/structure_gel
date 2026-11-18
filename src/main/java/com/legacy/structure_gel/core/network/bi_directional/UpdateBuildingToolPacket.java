package com.legacy.structure_gel.core.network.bi_directional;

import com.legacy.structure_gel.api.util.NetworkUtil;
import com.legacy.structure_gel.core.StructureGelMod;
import com.legacy.structure_gel.core.item.building_tool.BuildingToolItem;
import com.legacy.structure_gel.core.item.building_tool.BuildingToolMode;
import com.legacy.structure_gel.core.item.building_tool.BuildingToolModes;
import com.legacy.structure_gel.core.item.building_tool.ToolModeProperty;
import com.legacy.structure_gel.core.network.SGPacketHandler;
import com.legacy.structure_gel.core.registry.SGRegistry;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.util.random.WeightedEntry.Wrapper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.handling.PlayPayloadContext;

public record UpdateBuildingToolPacket(
   boolean toServer,
   InteractionHand hand,
   Optional<BuildingToolMode> mode,
   Map<ToolModeProperty<?>, Object> properties,
   Optional<BlockPos> posA,
   Optional<BlockPos> posB,
   boolean clearPoses,
   Optional<WeightedRandomList<Wrapper<BlockState>>> palette,
   Optional<Integer> reachDistanceModifier,
   Optional<Float> cornerReachDistance,
   Optional<Boolean> causeBlockUpdates
) implements SGPacketHandler.ModPacket<UpdateBuildingToolPacket, UpdateBuildingToolPacket.Handler> {
   public static UpdateBuildingToolPacket.Builder builder() {
      return new UpdateBuildingToolPacket.Builder();
   }

   public UpdateBuildingToolPacket.Handler handler() {
      return new UpdateBuildingToolPacket.Handler();
   }

   public static class Builder {
      boolean toServer = true;
      InteractionHand hand = InteractionHand.MAIN_HAND;
      Optional<BuildingToolMode> mode = Optional.empty();
      Map<ToolModeProperty<?>, Object> properties = Map.of();
      Optional<BlockPos> posA = Optional.empty();
      Optional<BlockPos> posB = Optional.empty();
      boolean clearPoses = false;
      Optional<WeightedRandomList<Wrapper<BlockState>>> palette = Optional.empty();
      Optional<Integer> reachDistanceModifier = Optional.empty();
      Optional<Float> cornerReachDistance = Optional.empty();
      Optional<Boolean> causeBlockUpdates = Optional.empty();

      private Builder() {
      }

      public UpdateBuildingToolPacket.Builder toClient() {
         this.toServer = false;
         return this;
      }

      public UpdateBuildingToolPacket.Builder toServer() {
         this.toServer = true;
         return this;
      }

      public UpdateBuildingToolPacket.Builder hand(InteractionHand hand) {
         this.hand = hand;
         return this;
      }

      public UpdateBuildingToolPacket.Builder mode(BuildingToolMode mode) {
         this.mode = Optional.ofNullable(mode);
         return this;
      }

      public UpdateBuildingToolPacket.Builder properties(Map<ToolModeProperty<?>, Object> properties) {
         this.properties = properties;
         return this;
      }

      public UpdateBuildingToolPacket.Builder posA(Optional<BlockPos> posA) {
         this.posA = posA;
         return this;
      }

      public UpdateBuildingToolPacket.Builder posB(Optional<BlockPos> posB) {
         this.posB = posB;
         return this;
      }

      public UpdateBuildingToolPacket.Builder clearPoses(boolean clearPoses) {
         this.clearPoses = clearPoses;
         return this;
      }

      public UpdateBuildingToolPacket.Builder palette(WeightedRandomList<Wrapper<BlockState>> states) {
         this.palette = Optional.ofNullable(states);
         return this;
      }

      public UpdateBuildingToolPacket.Builder reachDistance(int reachDistance) {
         this.reachDistanceModifier = Optional.of(reachDistance);
         return this;
      }

      public UpdateBuildingToolPacket.Builder cornerReachDistance(float cornerReachDistance) {
         this.cornerReachDistance = Optional.of(cornerReachDistance);
         return this;
      }

      public UpdateBuildingToolPacket.Builder causeBlockUpdates(boolean val) {
         this.causeBlockUpdates = Optional.of(val);
         return this;
      }

      public UpdateBuildingToolPacket build() {
         return new UpdateBuildingToolPacket(
            this.toServer,
            this.hand,
            this.mode,
            this.properties,
            this.posA,
            this.posB,
            this.clearPoses,
            this.palette,
            this.reachDistanceModifier,
            this.cornerReachDistance,
            this.causeBlockUpdates
         );
      }
   }

   public static class Handler implements SGPacketHandler.PlayHandler<UpdateBuildingToolPacket> {
      @Override
      public ResourceLocation id() {
         return StructureGelMod.locate("update_building_tool");
      }

      public void encoder(UpdateBuildingToolPacket packet, FriendlyByteBuf buff) {
         buff.writeBoolean(packet.toServer);
         buff.writeBoolean(packet.hand == InteractionHand.MAIN_HAND);
         NetworkUtil.writeOptional(buff, packet.mode.map(BuildingToolMode::getName), buff::writeResourceLocation);
         buff.writeInt(packet.properties.size());

         for (Entry<ToolModeProperty<?>, Object> entry : packet.properties.entrySet()) {
            ToolModeProperty property = entry.getKey();
            buff.writeUtf(property.getKey());
            buff.writeUtf(property.write(entry.getValue()));
         }

         NetworkUtil.writeOptional(buff, packet.posA, buff::writeBlockPos);
         NetworkUtil.writeOptional(buff, packet.posB, buff::writeBlockPos);
         buff.writeBoolean(packet.clearPoses);
         NetworkUtil.writeOptional(buff, packet.palette, states -> {
            List<Wrapper<BlockState>> list = states.unwrap();
            buff.writeInt(list.size());

            for (Wrapper<BlockState> state : list) {
               buff.writeNbt(NbtUtils.writeBlockState((BlockState)state.getData()));
               buff.writeInt(state.getWeight().asInt());
            }
         });
         NetworkUtil.writeOptional(buff, packet.reachDistanceModifier, buff::writeInt);
         NetworkUtil.writeOptional(buff, packet.cornerReachDistance, buff::writeFloat);
         NetworkUtil.writeOptional(buff, packet.causeBlockUpdates, buff::writeBoolean);
      }

      public UpdateBuildingToolPacket decoder(FriendlyByteBuf buff) {
         boolean toServer = buff.readBoolean();
         InteractionHand hand = buff.readBoolean() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
         Optional<BuildingToolMode> mode = NetworkUtil.<ResourceLocation>readOptional(buff, buff::readResourceLocation, () -> null)
            .map(BuildingToolModes.REGISTRY::get);
         Map<ToolModeProperty<?>, Object> properties = new HashMap<>();
         int propertiesSize = buff.readInt();
         if (!mode.isPresent() && propertiesSize > 0) {
            StructureGelMod.LOGGER.warn("Can't read building tool properties from packet because no mode was sent");
         } else {
            for (int i = 0; i < propertiesSize; i++) {
               String k = buff.readUtf();
               String v = buff.readUtf();
               ToolModeProperty<?> property = mode.get().getProperties().get(k);
               properties.put(property, property.read(v));
            }
         }

         Optional<BlockPos> posA = NetworkUtil.readOptional(buff, buff::readBlockPos, () -> null);
         Optional<BlockPos> posB = NetworkUtil.readOptional(buff, buff::readBlockPos, () -> null);
         boolean clearPoses = buff.readBoolean();
         Optional<WeightedRandomList<Wrapper<BlockState>>> palette = NetworkUtil.readOptional(buff, () -> {
            net.minecraft.util.random.SimpleWeightedRandomList.Builder<BlockState> paletteBuilder = SimpleWeightedRandomList.builder();
            int size = buff.readInt();

            for (int i = 0; i < size; i++) {
               BlockState state = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), buff.readNbt());
               int weight = buff.readInt();
               paletteBuilder.add(state, weight);
            }

            return paletteBuilder.build();
         }, () -> null);
         Optional<Integer> reachDistanceModifier = NetworkUtil.readOptional(buff, buff::readInt, () -> 0);
         Optional<Float> cornerReachDistance = NetworkUtil.readOptional(buff, buff::readFloat, () -> 0.0F);
         Optional<Boolean> causeBlockUpdates = NetworkUtil.readOptional(buff, buff::readBoolean, () -> null);
         return new UpdateBuildingToolPacket(
            toServer, hand, mode, properties, posA, posB, clearPoses, palette, reachDistanceModifier, cornerReachDistance, causeBlockUpdates
         );
      }

      public void handler(UpdateBuildingToolPacket packet, PlayPayloadContext context) {
      }

      public void handleClientbound(UpdateBuildingToolPacket packet, PlayPayloadContext context) {
         context.workHandler().submitAsync(() -> handleClient(packet));
      }

      public void handleServerbound(UpdateBuildingToolPacket packet, PlayPayloadContext context) {
         context.workHandler().submitAsync(() -> handle(packet, (Player)context.player().get()));
      }

      @OnlyIn(Dist.CLIENT)
      private static void handleClient(UpdateBuildingToolPacket packet) {
         handle(packet, Minecraft.getInstance().player);
      }

      private static void handle(UpdateBuildingToolPacket packet, Player player) {
         InteractionHand hand = packet.hand;
         ItemStack stack = player.getItemInHand(hand);
         if (stack.is(SGRegistry.Items.BUILDING_TOOL.get())) {
            if (packet.mode.isPresent()) {
               BuildingToolMode mode = packet.mode.get();
               BuildingToolItem.setMode(stack, mode);
               if (mode instanceof BuildingToolMode.ForCorners cornerMode) {
                  if (packet.posA.isPresent()) {
                     cornerMode.setPositionA(player, packet.posA.get(), stack, false);
                  }

                  if (packet.posB.isPresent()) {
                     cornerMode.setPositionB(player, packet.posB.get(), stack, false);
                  }
               }

               if (packet.clearPoses) {
                  mode.clearPoses(stack, player);
               }
            }

            for (Entry<ToolModeProperty<?>, Object> entry : packet.properties.entrySet()) {
               ToolModeProperty prop = entry.getKey();
               BuildingToolItem.setProperty(stack, prop, entry.getValue());
            }

            if (packet.palette.isPresent()) {
               BuildingToolItem.setPalette(stack, packet.palette.get());
            }

            if (packet.reachDistanceModifier.isPresent()) {
               int reach = packet.reachDistanceModifier.get();
               if (ToolModeProperty.REACH_DISTANCE.isValid(reach) && reach != BuildingToolItem.getReachDistanceModifier(stack)) {
                  BuildingToolItem.setReachDistanceModifier(stack, reach);
               }
            }

            if (packet.cornerReachDistance.isPresent()) {
               BuildingToolItem.setSelectedCornerDistance(stack, packet.cornerReachDistance.get());
            }

            if (packet.causeBlockUpdates.isPresent()) {
               BuildingToolItem.setCausesBlockUpdates(stack, packet.causeBlockUpdates.get());
            }

            if (packet.mode.isPresent()) {
               packet.mode.get().onSelect(stack, player.level(), player);
            }

            player.swing(hand);
         }
      }
   }
}
