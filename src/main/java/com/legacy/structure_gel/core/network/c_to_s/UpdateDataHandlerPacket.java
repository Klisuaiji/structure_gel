package com.legacy.structure_gel.core.network.c_to_s;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.legacy.structure_gel.api.data_handler.handlers.DataHandler;
import com.legacy.structure_gel.core.StructureGelMod;
import com.legacy.structure_gel.core.block.DataHandlerBlock;
import com.legacy.structure_gel.core.block_entity.DataHandlerBlockEntity;
import com.legacy.structure_gel.core.block_entity.DataHandlerBlockEntity.RawHandler;
import com.legacy.structure_gel.core.registry.SGRegistry;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.Weight;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Sends data from a {@link DataHandlerBlockEntity}'s gui to the server
 * 
 * @author Silver_David
 *
 */
public record UpdateDataHandlerPacket(Mode mode, BlockPos pos, List<RawHandler> handlers, String name, boolean useGravity, boolean waterlogged, Vec3 offset, boolean connectToBlocks, boolean markPostProcessing) implements CustomPacketPayload
{

	public static final Type<UpdateDataHandlerPacket> TYPE = new Type<>(StructureGelMod.locate("update_data_handler"));

	public static final StreamCodec<RegistryFriendlyByteBuf, UpdateDataHandlerPacket> CODEC = new StreamCodec<>()
	{
		@Override
		public void encode(RegistryFriendlyByteBuf buff, UpdateDataHandlerPacket packet)
		{
			Mode.STREAM_CODEC.encode(buff, packet.mode);
			buff.writeBlockPos(packet.pos);
			buff.writeInt(packet.handlers.size());
			for (var handler : packet.handlers)
			{
				buff.writeInt(handler.getWeight().asInt());
				buff.writeResourceLocation(handler.typeName());
				buff.writeInt(handler.dataEntries().size());
				for (var entry : handler.dataEntries().entrySet())
				{
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

		@Override
		public UpdateDataHandlerPacket decode(RegistryFriendlyByteBuf buff)
		{
			try
			{
				Mode mode = Mode.STREAM_CODEC.decode(buff);
				BlockPos pos = buff.readBlockPos();
				List<RawHandler> handlers = new ArrayList<>();
				int size = buff.readInt();
				for (int i = 0; i < size; i++)
				{
					Weight weight = Weight.of(buff.readInt());
					ResourceLocation type = buff.readResourceLocation();
					LinkedHashMap<String, String> dataEntries = new LinkedHashMap<>();
					int entrySize = buff.readInt();
					for (int e = 0; e < entrySize; e++)
					{
						dataEntries.put(buff.readUtf(), buff.readUtf());
					}
					handlers.add(new RawHandler(weight, type, dataEntries));
				}

				String name = buff.readUtf();
				boolean useGravity = buff.readBoolean();
				boolean waterlogged = buff.readBoolean();
				Vec3 offset = new Vec3(buff.readDouble(), buff.readDouble(), buff.readDouble());
				boolean connectToBlocks = buff.readBoolean();
				boolean markPostProcessing = buff.readBoolean();
				return new UpdateDataHandlerPacket(mode, pos, handlers, name, useGravity, waterlogged, offset, connectToBlocks, markPostProcessing);
			}
			catch (Throwable t)
			{
				return new UpdateDataHandlerPacket(Mode.ERROR, BlockPos.ZERO, Collections.emptyList(), "", false, false, Vec3.ZERO, false, false);
			}
		}
	};

	@Override
	public Type<? extends CustomPacketPayload> type()
	{
		return TYPE;
	}

	public static void handler(UpdateDataHandlerPacket packet, IPayloadContext context)
	{
		if (packet.mode == Mode.ERROR)
		{
			if (!(context.player() instanceof ServerPlayer player))
				return;
			StructureGelMod.LOGGER.warn("An invalid packet ({}) was sent to the server from {} at {} in {}", packet.getClass().getSimpleName(), player.getGameProfile().getName(), player.blockPosition(), player.level().dimension().location());
			return;
		}

		context.enqueueWork(() ->
		{
			if (!(context.player() instanceof ServerPlayer player))
				return;
			ServerLevel level = player.serverLevel();
			BlockPos pos = packet.pos;
			BlockEntity blockEntity = level.getBlockEntity(pos);
			BlockState state = level.getBlockState(pos);
			if (state.is(SGRegistry.Blocks.DATA_HANDLER.get()) && blockEntity instanceof DataHandlerBlockEntity)
			{
				DataHandlerBlockEntity dataHandler = (DataHandlerBlockEntity) blockEntity;

				// Save
				if (packet.mode.shouldSave())
				{
					dataHandler.setHandlers(WeightedRandomList.create(packet.handlers));
					dataHandler.setCustomName(packet.name.isBlank() ? null : Component.literal(packet.name));
					dataHandler.setUseGravity(packet.useGravity);
					dataHandler.setOffset(packet.offset);
					dataHandler.setMarkPostProcessing(packet.markPostProcessing);
					boolean waterlogged = packet.waterlogged;
					boolean connectToBlocks = packet.connectToBlocks;
					BlockState newState = state.setValue(DataHandlerBlock.WATERLOGGED, waterlogged).setValue(DataHandlerBlock.CONNECT_TO_BLOCKS, connectToBlocks);
					level.setBlock(pos, newState, Block.UPDATE_ALL);
					blockEntity.setChanged();
					level.sendBlockUpdated(pos, state, newState, Block.UPDATE_ALL);
				}

				// Generate
				if (packet.mode.shouldGenerate())
				{
					ItemStack stack = SGRegistry.Blocks.DATA_HANDLER.get() instanceof DataHandlerBlock d ? d.getCloneItemStack(level, pos, state) : SGRegistry.Blocks.DATA_HANDLER.asItem().getDefaultInstance();

					// Copy Block Entity data to the item
					CompoundTag beTag = dataHandler.saveCustomOnly(level.registryAccess());
					// If this got removed, it used to be deprecated. Probably switched to components or smthn
					dataHandler.removeComponentsFromTag(beTag);

					BlockItem.setBlockEntityData(stack, dataHandler.getType(), beTag);
					stack.applyComponents(dataHandler.collectComponents());
					
					// Copy block state data
					stack.set(DataComponents.BLOCK_STATE, new BlockItemStateProperties(Map.of()).with(DataHandlerBlock.WATERLOGGED, packet.waterlogged).with(DataHandlerBlock.CONNECT_TO_BLOCKS, packet.connectToBlocks));

					// Only give if the player doesn't already have this item
					if (!player.getInventory().contains(stack))
					{
						ItemEntity itemEntity = player.drop(stack, false);
						if (itemEntity != null)
						{
							// Same logic as give command
							itemEntity.setNoPickUpDelay();
							itemEntity.setTarget(player.getUUID());
						}
					}
					int offsetAmt = 8;
					BoundingBox bounds = BoundingBox.fromCorners(pos.offset(-offsetAmt, -offsetAmt, -offsetAmt), pos.offset(offsetAmt, offsetAmt, offsetAmt));
					DataHandler.process(state, dataHandler.saveWithFullMetadata(level.registryAccess()), pos, level, level.random, bounds, new DummyPiece(bounds), true);
				}
			}
			else
			{
				StructureGelMod.LOGGER.warn("Attempted to set DataHandlerBlockEntity data for the wrong block.");
			}
		});
	}

	public static enum Mode
	{
		ERROR(-1),
		SAVE(0),
		GENERATE(1);

		public static final StreamCodec<FriendlyByteBuf, Mode> STREAM_CODEC = StreamCodec.of((b, m) -> b.writeByte(m.flag), b -> Mode.from(b.readByte()));

		byte flag;

		Mode(int flag)
		{
			this.flag = (byte) flag;
		}

		private boolean shouldSave()
		{
			return this == SAVE || this == GENERATE;
		}

		private boolean shouldGenerate()
		{
			return this == GENERATE;
		}

		private static Mode from(byte flag)
		{
			for (var m : Mode.values())
				if (m.flag == flag)
					return m;
			return ERROR;
		}
	}

	private static final class DummyPiece extends StructurePiece
	{
		protected DummyPiece(BoundingBox bounds)
		{
			super(StructurePieceType.JIGSAW, 0, bounds);
			this.setOrientation(Direction.NORTH);
		}

		@Override
		protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag)
		{
		}

		@Override
		public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator chunkGen, RandomSource rand, BoundingBox bounds, ChunkPos chunkPos, BlockPos pos)
		{
		}

	}
}
