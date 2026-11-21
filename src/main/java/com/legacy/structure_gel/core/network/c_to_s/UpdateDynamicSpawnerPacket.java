package com.legacy.structure_gel.core.network.c_to_s;

import com.legacy.structure_gel.api.dynamic_spawner.DynamicSpawnerType;
import com.legacy.structure_gel.api.registry.StructureGelRegistries;
import com.legacy.structure_gel.core.StructureGelMod;
import com.legacy.structure_gel.core.block_entity.DataHandlerBlockEntity;
import com.legacy.structure_gel.core.block_entity.DynamicSpawnerBlockEntity;
import com.legacy.structure_gel.core.block_entity.DynamicSpawnerBlockEntity.ExpRange;
import com.legacy.structure_gel.core.registry.SGRegistry;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Sends data from a {@link DataHandlerBlockEntity}'s gui to the server
 * 
 * @author Silver_David
 *
 */
public record UpdateDynamicSpawnerPacket(BlockPos pos, ResourceLocation spawnerType, ExpRange expRange) implements CustomPacketPayload
{

	public static final Type<UpdateDynamicSpawnerPacket> TYPE = new Type<>(StructureGelMod.locate("update_dynamic_spawner"));

	public static final StreamCodec<RegistryFriendlyByteBuf, UpdateDynamicSpawnerPacket> CODEC = new StreamCodec<>()
	{
		@Override
		public void encode(RegistryFriendlyByteBuf buff, UpdateDynamicSpawnerPacket packet)
		{
			buff.writeBlockPos(packet.pos);
			buff.writeResourceLocation(packet.spawnerType);
			buff.writeInt(packet.expRange.min);
			buff.writeInt(packet.expRange.max);
		}

		@Override
		public UpdateDynamicSpawnerPacket decode(RegistryFriendlyByteBuf buff)
		{
			try
			{
				BlockPos pos = buff.readBlockPos();
				ResourceLocation type = buff.readResourceLocation();
				ExpRange range = new ExpRange(buff.readInt(), buff.readInt());
				return new UpdateDynamicSpawnerPacket(pos, type, range);
			}
			catch (Throwable t)
			{
				return new UpdateDynamicSpawnerPacket(BlockPos.ZERO, ERRORED, ExpRange.VANILLA);
			}
		}
	};

	@Override
	public Type<? extends CustomPacketPayload> type()
	{
		return TYPE;
	}

	private static final ResourceLocation ERRORED = StructureGelMod.locate("errored");

	public static void handler(UpdateDynamicSpawnerPacket packet, IPayloadContext context)
	{
		if (!(context.player() instanceof ServerPlayer player))
			return;

		if (packet.spawnerType.equals(ERRORED) && packet.pos.equals(BlockPos.ZERO))
		{
			StructureGelMod.LOGGER.warn("An invalid packet ({}) was sent to the server from {} at {} in {}", packet.getClass().getSimpleName(), player.getGameProfile().getName(), player.blockPosition(), player.level().dimension().location());
			return;
		}

		context.enqueueWork(() ->
		{
			ServerLevel level = player.serverLevel();
			BlockPos pos = packet.pos;
			BlockState state = level.getBlockState(pos);
			if (state.getBlock() == SGRegistry.Blocks.DYNAMIC_SPAWNER.get() && level.getBlockEntity(pos) instanceof DynamicSpawnerBlockEntity spawner)
			{
				// Save
				DynamicSpawnerType dynSpawn = StructureGelRegistries.DYNAMIC_SPAWNER_TYPE.get(packet.spawnerType);
				if (dynSpawn != null)
					spawner.setSpawner(dynSpawn, level.registryAccess());

				spawner.setExpRange(packet.expRange);

				spawner.setChanged();
				level.sendBlockUpdated(pos, state, state, 3);
			}
			else
			{
				StructureGelMod.LOGGER.warn("Attempted to set DynamicSpawnerBlockEntity data for the wrong block.");
			}
		});
	}
}
