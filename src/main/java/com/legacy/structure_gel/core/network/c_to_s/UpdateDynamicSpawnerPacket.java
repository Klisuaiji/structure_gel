package com.legacy.structure_gel.core.network.c_to_s;

import com.legacy.structure_gel.api.dynamic_spawner.DynamicSpawnerType;
import com.legacy.structure_gel.api.registry.StructureGelRegistries;
import com.legacy.structure_gel.core.StructureGelMod;
import com.legacy.structure_gel.core.block_entity.DynamicSpawnerBlockEntity;
import com.legacy.structure_gel.core.network.SGPacketHandler;
import com.legacy.structure_gel.core.registry.SGRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class UpdateDynamicSpawnerPacket implements SGPacketHandler.ModPacket<UpdateDynamicSpawnerPacket, UpdateDynamicSpawnerPacket.Handler> {
   final BlockPos pos;
   final ResourceLocation type;
   final DynamicSpawnerBlockEntity.ExpRange expRange;

   public UpdateDynamicSpawnerPacket(BlockPos pos, ResourceLocation type, DynamicSpawnerBlockEntity.ExpRange expRange) {
      this.pos = pos;
      this.type = type;
      this.expRange = expRange;
   }

   public UpdateDynamicSpawnerPacket.Handler handler() {
      return new UpdateDynamicSpawnerPacket.Handler();
   }

   public static class Handler implements SGPacketHandler.PlayHandler<UpdateDynamicSpawnerPacket> {
      private static final ResourceLocation ERRORED = StructureGelMod.locate("errored");

      @Override
      public ResourceLocation id() {
         return StructureGelMod.locate("update_dynamic_spawner");
      }

      public void encoder(UpdateDynamicSpawnerPacket packet, FriendlyByteBuf buff) {
         buff.writeBlockPos(packet.pos);
         buff.writeResourceLocation(packet.type);
         buff.writeInt(packet.expRange.min);
         buff.writeInt(packet.expRange.max);
      }

      public UpdateDynamicSpawnerPacket decoder(FriendlyByteBuf buff) {
         try {
            BlockPos pos = buff.readBlockPos();
            ResourceLocation type = buff.readResourceLocation();
            DynamicSpawnerBlockEntity.ExpRange range = new DynamicSpawnerBlockEntity.ExpRange(buff.readInt(), buff.readInt());
            return new UpdateDynamicSpawnerPacket(pos, type, range);
         } catch (Throwable t) {
            return new UpdateDynamicSpawnerPacket(BlockPos.ZERO, ERRORED, DynamicSpawnerBlockEntity.ExpRange.VANILLA);
         }
      }

      public void handler(UpdateDynamicSpawnerPacket packet, IPayloadContext context) {
         if (packet.type.equals(ERRORED) && packet.pos.equals(BlockPos.ZERO)) {
            ServerPlayer sender = this.serverPlayer(context);
            StructureGelMod.LOGGER
               .warn(
                  "An invalid packet ({}) was sent to the server from {} at {} in {}",
                  packet.getClass().getSimpleName(),
                  sender.getGameProfile().getName(),
                  sender.blockPosition(),
                  sender.level().dimension().location()
               );
         } else {
            context.enqueueWork(() -> {
               ServerLevel level = this.serverLevel(context);
               BlockPos pos = packet.pos;
               BlockState state = level.getBlockState(pos);
               if (state.getBlock() == SGRegistry.Blocks.DYNAMIC_SPAWNER.get() && level.getBlockEntity(pos) instanceof DynamicSpawnerBlockEntity spawner) {
                  DynamicSpawnerType dynSpawn = (DynamicSpawnerType)StructureGelRegistries.DYNAMIC_SPAWNER_TYPE.get(packet.type);
                  if (dynSpawn != null) {
                     spawner.setSpawner(dynSpawn);
                  }

                  spawner.setExpRange(packet.expRange);
                  spawner.setChanged();
                  level.sendBlockUpdated(pos, state, state, 3);
               } else {
                  StructureGelMod.LOGGER.warn("Attempted to set DynamicSpawnerBlockEntity data for the wrong block.");
               }
            });
         }
      }
   }
}
