package com.legacy.structure_gel.core.network.c_to_s;

import com.legacy.structure_gel.core.StructureGelMod;
import com.legacy.structure_gel.core.item.building_tool.BuildingToolItem;
import com.legacy.structure_gel.core.registry.SGRegistry;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ExportPalettePacket(ItemStack palette) implements CustomPacketPayload
{
	public static final Type<ExportPalettePacket> TYPE = new Type<>(StructureGelMod.locate("export_palette"));

	public static final StreamCodec<RegistryFriendlyByteBuf, ExportPalettePacket> CODEC = new StreamCodec<>()
	{
		@Override
		public void encode(RegistryFriendlyByteBuf buff, ExportPalettePacket packet)
		{
			ItemStack.STREAM_CODEC.encode(buff, packet.palette);
		}

		@Override
		public ExportPalettePacket decode(RegistryFriendlyByteBuf buff)
		{
			return new ExportPalettePacket(ItemStack.STREAM_CODEC.decode(buff));
		}
	};

	@Override
	public Type<? extends CustomPacketPayload> type()
	{
		return TYPE;
	}

	public static void handler(ExportPalettePacket packet, IPayloadContext context)
	{
		context.enqueueWork(() ->
		{
			if (!(context.player() instanceof ServerPlayer player))
				return;
			if (player == null || player.level() == null)
				return;

			if (BuildingToolItem.hasToolPermission(SGRegistry.Items.BUILDING_TOOL.getDefaultInstance(), player))
			{
				ItemStack paletteItem = packet.palette.copy();
				if (!player.addItem(paletteItem))
				{
					player.drop(paletteItem, false);
				}
			}
		});
	}
}
