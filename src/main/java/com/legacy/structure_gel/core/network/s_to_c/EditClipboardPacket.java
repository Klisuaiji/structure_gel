package com.legacy.structure_gel.core.network.s_to_c;

import javax.annotation.Nullable;

import com.legacy.structure_gel.api.util.NetworkUtil;
import com.legacy.structure_gel.core.StructureGelMod;
import com.legacy.structure_gel.core.item.building_tool.CapturedBlocks;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record EditClipboardPacket(String ownerName, BoundingBox addKey, @Nullable CapturedBlocks captured, BoundingBox removeKey) implements CustomPacketPayload
{
	public static final Type<EditClipboardPacket> TYPE = new Type<>(StructureGelMod.locate("edit_clipboard"));

	public static final StreamCodec<RegistryFriendlyByteBuf, EditClipboardPacket> CODEC = new StreamCodec<>()
	{
		@Override
		public void encode(RegistryFriendlyByteBuf buff, EditClipboardPacket packet)
		{
			buff.writeUtf(packet.ownerName);
			NetworkUtil.writeBoundingBox(buff, packet.addKey);
			if (!packet.addKey.equals(BoundingBox.infinite()) && packet.captured != null)
			{
				CompoundTag tag = packet.captured.toCompressedTag(buff.registryAccess());
				NetworkUtil.writeCompressedNbt(buff, tag);
			}
			NetworkUtil.writeBoundingBox(buff, packet.removeKey);
		}
		
		@Override
		public EditClipboardPacket decode(RegistryFriendlyByteBuf buff)
		{
			String ownerName = buff.readUtf();
			BoundingBox addKey = NetworkUtil.readBoundingBox(buff);
			CapturedBlocks captured = null;
			if (!addKey.equals(BoundingBox.infinite()))
			{
				CompoundTag tag = NetworkUtil.readCompressedNbt(buff, () -> null);
				if (tag != null)
					captured = CapturedBlocks.fromCompressedTag(buff.registryAccess(), tag);
			}
			BoundingBox removeKey = NetworkUtil.readBoundingBox(buff);
			return new EditClipboardPacket(ownerName, addKey, captured, removeKey);
		}
	};

	@Override
	public Type<? extends CustomPacketPayload> type()
	{
		return TYPE;
	}

	public static void handler(EditClipboardPacket packet, IPayloadContext context)
	{
		if (FMLEnvironment.dist == Dist.CLIENT)
			context.enqueueWork(() -> handleClient(packet, context));
	}
	
	@OnlyIn(Dist.CLIENT)
	public static void handleClient(EditClipboardPacket packet, IPayloadContext context)
	{
		Player player = net.minecraft.client.Minecraft.getInstance().player;
		var data = StructureGelMod.proxy.getBuildingToolData(null, player.getGameProfile().getName());

		if (!packet.addKey.equals(BoundingBox.infinite()) && packet.captured != null)
			data.addToClipboardDirect(packet.addKey, packet.captured, packet.ownerName);
		if (!packet.removeKey.equals(BoundingBox.infinite()))
			data.removeFromClipboard(packet.removeKey, packet.ownerName);
	}
}
