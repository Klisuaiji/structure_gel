package com.legacy.structure_gel.core.network.c_to_s;

import com.legacy.blue_skies.compat.*;
import com.legacy.structure_gel.api.data_handler.handlers.DataHandler;
import com.legacy.structure_gel.core.StructureGelMod;
import com.legacy.structure_gel.core.block.DataHandlerBlock;
import com.legacy.structure_gel.core.block_entity.DataHandlerBlockEntity;
import com.legacy.structure_gel.core.network.SGPacketHandler;
import com.legacy.structure_gel.core.registry.SGRegistry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.Weight;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record UpdateDataHandlerPacket(
   byte mode,
   BlockPos pos,
   List<DataHandlerBlockEntity.RawHandler> handlers,
   String name,
   boolean useGravity,
   boolean waterlogged,
   Vec3 offset,
   boolean connectToBlocks,
   boolean markPostProcessing
) implements SGPacketHandler.ModPacket<UpdateDataHandlerPacket, UpdateDataHandlerPacket.Handler> {
   public UpdateDataHandlerPacket.Handler handler() {
      return new UpdateDataHandlerPacket.Handler();
   }

   private static final class DummyPiece extends StructurePiece {
      protected DummyPiece(BoundingBox bounds) {
         super(StructurePieceType.JIGSAW, 0, bounds);
         this.setOrientation(Direction.NORTH);
      }

      protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
      }

      public void postProcess(
         WorldGenLevel level,
         StructureManager structureManager,
         ChunkGenerator chunkGen,
         RandomSource rand,
         BoundingBox bounds,
         ChunkPos chunkPos,
         BlockPos pos
      ) {
      }
   }

   public static class Handler implements SGPacketHandler.PlayHandler<UpdateDataHandlerPacket> {
      @Override
      public ResourceLocation id() {
         return StructureGelMod.locate("update_data_handler");
      }

      public void encoder(UpdateDataHandlerPacket packet, FriendlyByteBuf buff) {
         buff.writeByte(packet.mode);
         buff.writeBlockPos(packet.pos);
         buff.writeInt(packet.handlers.size());

         for (DataHandlerBlockEntity.RawHandler handler : packet.handlers) {
            buff.writeInt(handler.getWeight().asInt());
            buff.writeResourceLocation(handler.typeName());
            buff.writeInt(handler.dataEntries().size());

            for (Entry<String, String> entry : handler.dataEntries().entrySet()) {
               buff.writeUtf(entry.getKey());
               buff.writeUtf(entry.getValue());
            }
         }

         buff.writeUtf(packet.name);
         buff.writeBoolean(packet.useGravity);
         buff.writeBoolean(packet.waterlogged);
         buff.writeDouble(packet.offset.x);
         buff.writeDouble(packet.offset.y);
         buff.writeDouble(packet.offset.z);
         buff.writeBoolean(packet.connectToBlocks);
         buff.writeBoolean(packet.markPostProcessing);
      }

      public UpdateDataHandlerPacket decoder(FriendlyByteBuf buff) {
         try {
            byte mode = buff.readByte();
            BlockPos pos = buff.readBlockPos();
            List<DataHandlerBlockEntity.RawHandler> handlers = new ArrayList<>();
            int size = buff.readInt();

            for (int i = 0; i < size; i++) {
               Weight weight = Weight.of(buff.readInt());
               ResourceLocation type = buff.readResourceLocation();
               LinkedHashMap<String, String> dataEntries = new LinkedHashMap<>();
               int entrySize = buff.readInt();

               for (int e = 0; e < entrySize; e++) {
                  dataEntries.put(buff.readUtf(), buff.readUtf());
               }

               handlers.add(new DataHandlerBlockEntity.RawHandler(weight, type, dataEntries));
            }

            String name = buff.readUtf();
            boolean useGravity = buff.readBoolean();
            boolean waterlogged = buff.readBoolean();
            Vec3 offset = new Vec3(buff.readDouble(), buff.readDouble(), buff.readDouble());
            boolean connectToBlocks = buff.readBoolean();
            boolean markPostProcessing = buff.readBoolean();
            return new UpdateDataHandlerPacket(mode, pos, handlers, name, useGravity, waterlogged, offset, connectToBlocks, markPostProcessing);
         } catch (Throwable t) {
            return new UpdateDataHandlerPacket((byte)-1, BlockPos.ZERO, Collections.emptyList(), "", false, false, Vec3.ZERO, false, false);
         }
      }

      public void handler(UpdateDataHandlerPacket packet, IPayloadContext context) {
         if (packet.mode == -1) {
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
            context.workHandler()
               .submitAsync(
                  () -> {
                     ServerLevel level = this.serverLevel(context);
                     BlockPos pos = packet.pos;
                     BlockEntity blockEntity = level.getBlockEntity(pos);
                     BlockState state = level.getBlockState(pos);
                     if (state.is(SGRegistry.Blocks.DATA_HANDLER.get()) && blockEntity instanceof DataHandlerBlockEntity dataHandler) {
                        dataHandler.setHandlers(WeightedRandomList.create(packet.handlers));
                        dataHandler.setCustomName(packet.name.isBlank() ? null : Component.literal(packet.name));
                        dataHandler.setUseGravity(packet.useGravity);
                        dataHandler.setOffset(packet.offset);
                        dataHandler.setMarkPostProcessing(packet.markPostProcessing);
                        boolean waterlogged = packet.waterlogged;
                        boolean connectToBlocks = packet.connectToBlocks;
                        BlockState newState = (BlockState)((BlockState)state.setValue(DataHandlerBlock.WATERLOGGED, waterlogged))
                           .setValue(DataHandlerBlock.CONNECT_TO_BLOCKS, connectToBlocks);
                        level.setBlock(pos, newState, 3);
                        blockEntity.setChanged();
                        level.sendBlockUpdated(pos, state, newState, 3);
                        if (packet.mode == 1) {
                           ServerPlayer player = this.serverPlayer(context);
                           ItemStack stack = SGRegistry.Blocks.DATA_HANDLER.get().getCloneItemStack(level, pos, state, true);
                           blockEntity.saveToItem(stack);
                           boolean hasItem = false;

                           for (ItemStack invStack : player.getInventory().items) {
                              if (!invStack.isEmpty() && invStack.is(SGRegistry.Blocks.DATA_HANDLER.asItem())) {
                                 ItemStack cloneInvStack = invStack.copy();
                                 CompoundTag tag = LegacyNbt.get(cloneInvStack);
                                 if (tag != null && tag.contains("display", 10)) {
                                    tag.remove("display");
                                 }

                                 if (ItemStack.isSameItemSameTags(cloneInvStack, stack)) {
                                    hasItem = true;
                                    break;
                                 }
                              }
                           }

                           if (!hasItem) {
                              ItemEntity itemEntity = player.drop(stack, false);
                              if (itemEntity != null) {
                                 itemEntity.setNoPickUpDelay();
                                 itemEntity.setTarget(player.getUUID());
                              }
                           }

                           int offsetAmt = 8;
                           BoundingBox bounds = BoundingBox.fromCorners(
                              pos.offset(-offsetAmt, -offsetAmt, -offsetAmt), pos.offset(offsetAmt, offsetAmt, offsetAmt)
                           );
                           DataHandler.process(
                              state, dataHandler.saveWithFullMetadata(), pos, level, level.random, bounds, new UpdateDataHandlerPacket.DummyPiece(bounds), true
                           );
                        }
                     } else {
                        StructureGelMod.LOGGER.warn("Attempted to set DataHandlerBlockEntity data for the wrong block.");
                     }
                  }
               );
         }
      }
   }
}
