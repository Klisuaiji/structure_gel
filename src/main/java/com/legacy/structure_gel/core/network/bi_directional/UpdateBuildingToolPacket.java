package com.legacy.structure_gel.core.network.bi_directional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.legacy.structure_gel.api.util.NetworkUtil;
import com.legacy.structure_gel.core.StructureGelMod;
import com.legacy.structure_gel.core.data_components.BlockPalette;
import com.legacy.structure_gel.core.item.building_tool.BuildingToolItem;
import com.legacy.structure_gel.core.item.building_tool.BuildingToolMode;
import com.legacy.structure_gel.core.item.building_tool.BuildingToolModes;
import com.legacy.structure_gel.core.item.building_tool.ToolModeProperty;
import com.legacy.structure_gel.core.registry.SGRegistry;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record UpdateBuildingToolPacket(boolean toServer, InteractionHand hand, Optional<BuildingToolMode> mode, Map<ToolModeProperty<?>, Object> properties, Optional<BlockPos> posA, Optional<BlockPos> posB, boolean clearPoses, Optional<BlockPalette> palette, Optional<Integer> reachDistanceModifier, Optional<Float> cornerReachDistance, Optional<Boolean> causeBlockUpdates) implements CustomPacketPayload
{

	public static final Type<UpdateBuildingToolPacket> TYPE = new Type<>(StructureGelMod.locate("update_building_tool"));

	public static final StreamCodec<RegistryFriendlyByteBuf, UpdateBuildingToolPacket> CODEC = new StreamCodec<>()
	{
		@Override
		public void encode(RegistryFriendlyByteBuf buff, UpdateBuildingToolPacket packet)
		{
			buff.writeBoolean(packet.toServer);

			buff.writeBoolean(packet.hand == InteractionHand.MAIN_HAND);

			NetworkUtil.writeOptional(buff, packet.mode.map(BuildingToolMode::getName), buff::writeResourceLocation);

			buff.writeInt(packet.properties.size());
			for (var entry : packet.properties.entrySet())
			{
				ToolModeProperty property = entry.getKey();
				buff.writeUtf(property.getKey());
				buff.writeUtf(property.write(entry.getValue()));
			}

			NetworkUtil.writeOptional(buff, packet.posA, buff::writeBlockPos);
			NetworkUtil.writeOptional(buff, packet.posB, buff::writeBlockPos);
			buff.writeBoolean(packet.clearPoses);

			NetworkUtil.writeOptional(buff, packet.palette, p -> BlockPalette.STREAM_CODEC.encode(buff, p));

			NetworkUtil.writeOptional(buff, packet.reachDistanceModifier, buff::writeInt);

			NetworkUtil.writeOptional(buff, packet.cornerReachDistance, buff::writeFloat);

			NetworkUtil.writeOptional(buff, packet.causeBlockUpdates, buff::writeBoolean);
		}

		@Override
		public UpdateBuildingToolPacket decode(RegistryFriendlyByteBuf buff)
		{
			boolean toServer = buff.readBoolean();

			InteractionHand hand = buff.readBoolean() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;

			Optional<BuildingToolMode> mode = NetworkUtil.readOptional(buff, buff::readResourceLocation, () -> null).map(BuildingToolModes.REGISTRY::get);

			Map<ToolModeProperty<?>, Object> properties = new HashMap<>();
			int propertiesSize = buff.readInt();
			if (!mode.isPresent() && propertiesSize > 0)
			{
				StructureGelMod.LOGGER.warn("Can't read building tool properties from packet because no mode was sent");
			}
			else
			{
				for (int i = 0; i < propertiesSize; i++)
				{
					String k = buff.readUtf();
					String v = buff.readUtf();
					ToolModeProperty<?> property = mode.get().getProperties().get(k);
					properties.put(property, property.read(v));
				}
			}
			Optional<BlockPos> posA = NetworkUtil.readOptional(buff, buff::readBlockPos, () -> null);
			Optional<BlockPos> posB = NetworkUtil.readOptional(buff, buff::readBlockPos, () -> null);
			boolean clearPoses = buff.readBoolean();

			Optional<BlockPalette> palette = NetworkUtil.readOptional(buff, () -> BlockPalette.STREAM_CODEC.decode(buff), () -> null);

			Optional<Integer> reachDistanceModifier = NetworkUtil.readOptional(buff, buff::readInt, () -> 0);

			Optional<Float> cornerReachDistance = NetworkUtil.readOptional(buff, buff::readFloat, () -> 0.0F);

			Optional<Boolean> causeBlockUpdates = NetworkUtil.readOptional(buff, buff::readBoolean, () -> null);

			return new UpdateBuildingToolPacket(toServer, hand, mode, properties, posA, posB, clearPoses, palette, reachDistanceModifier, cornerReachDistance, causeBlockUpdates);
		}
	};

	@Override
	public Type<? extends CustomPacketPayload> type()
	{
		return TYPE;
	}

	public static void handler(UpdateBuildingToolPacket packet, IPayloadContext context)
	{
		if (context.flow().isClientbound())
			context.enqueueWork(() -> handleClient(packet));
		else
			context.enqueueWork(() -> handle(packet, context.player()));
	}

	@SuppressWarnings("resource")
	@OnlyIn(Dist.CLIENT)
	private static void handleClient(UpdateBuildingToolPacket packet)
	{
		handle(packet, net.minecraft.client.Minecraft.getInstance().player);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static void handle(UpdateBuildingToolPacket packet, Player player)
	{
		InteractionHand hand = packet.hand;
		ItemStack stack = player.getItemInHand(hand);
		if (stack.is(SGRegistry.Items.BUILDING_TOOL.get()))
		{
			if (packet.mode.isPresent())
			{
				var mode = packet.mode.get();
				BuildingToolItem.setMode(stack, mode);
				if (mode instanceof BuildingToolMode.ForCorners cornerMode)
				{
					if (packet.posA.isPresent())
						cornerMode.setPositionA(player, packet.posA.get(), stack, false);
					if (packet.posB.isPresent())
						cornerMode.setPositionB(player, packet.posB.get(), stack, false);
				}
				if (packet.clearPoses)
					mode.clearPoses(stack, player);
			}
			for (var entry : packet.properties.entrySet())
			{
				ToolModeProperty prop = entry.getKey();
				BuildingToolItem.setProperty(stack, prop, entry.getValue());
			}

			if (packet.palette.isPresent())
				BuildingToolItem.setPalette(stack, packet.palette.get());
			if (packet.reachDistanceModifier.isPresent())
			{
				int reach = packet.reachDistanceModifier.get();
				if (ToolModeProperty.REACH_DISTANCE.isValid(reach) && reach != BuildingToolItem.getReachDistanceModifier(stack))
				{
					BuildingToolItem.setReachDistanceModifier(stack, reach);
				}
			}
			if (packet.cornerReachDistance.isPresent())
			{
				BuildingToolItem.setSelectedCornerDistance(stack, packet.cornerReachDistance.get());
			}
			if (packet.causeBlockUpdates.isPresent())
			{
				BuildingToolItem.setCausesBlockUpdates(stack, packet.causeBlockUpdates.get());
			}
			if (packet.mode.isPresent())
			{
				packet.mode.get().onSelect(stack, player.level(), player);
			}
			player.swing(hand);
		}
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static class Builder
	{
		boolean toServer = true;
		InteractionHand hand = InteractionHand.MAIN_HAND;
		Optional<BuildingToolMode> mode = Optional.empty();
		Map<ToolModeProperty<?>, Object> properties = Map.of();
		Optional<BlockPos> posA = Optional.empty();
		Optional<BlockPos> posB = Optional.empty();
		boolean clearPoses = false;
		Optional<BlockPalette> palette = Optional.empty();
		Optional<Integer> reachDistanceModifier = Optional.empty();
		Optional<Float> cornerReachDistance = Optional.empty();
		Optional<Boolean> causeBlockUpdates = Optional.empty();

		private Builder()
		{
		}

		public Builder toClient()
		{
			this.toServer = false;
			return this;
		}

		public Builder toServer()
		{
			this.toServer = true;
			return this;
		}

		public Builder hand(InteractionHand hand)
		{
			this.hand = hand;
			return this;
		}

		public Builder mode(BuildingToolMode mode)
		{
			this.mode = Optional.ofNullable(mode);
			return this;
		}

		public Builder properties(Map<ToolModeProperty<?>, Object> properties)
		{
			this.properties = properties;
			return this;
		}

		public Builder posA(Optional<BlockPos> posA)
		{
			this.posA = posA;
			return this;
		}

		public Builder posB(Optional<BlockPos> posB)
		{
			this.posB = posB;
			return this;
		}

		public Builder clearPoses(boolean clearPoses)
		{
			this.clearPoses = clearPoses;
			return this;
		}

		public Builder palette(BlockPalette states)
		{
			this.palette = Optional.ofNullable(states);
			return this;
		}

		public Builder reachDistance(int reachDistance)
		{
			this.reachDistanceModifier = Optional.of(reachDistance);
			return this;
		}

		public Builder cornerReachDistance(float cornerReachDistance)
		{
			this.cornerReachDistance = Optional.of(cornerReachDistance);
			return this;
		}

		public Builder causeBlockUpdates(boolean val)
		{
			this.causeBlockUpdates = Optional.of(val);
			return this;
		}

		public UpdateBuildingToolPacket build()
		{
			return new UpdateBuildingToolPacket(toServer, hand, mode, properties, posA, posB, clearPoses, palette, reachDistanceModifier, cornerReachDistance, causeBlockUpdates);
		}
	}
}
