package com.legacy.structure_gel.core.network.s_to_c;

import java.util.ArrayList;
import java.util.List;

import com.legacy.structure_gel.core.StructureGelMod;
import com.legacy.structure_gel.core.client.renderers.StructureBoundsRenderer;
import com.legacy.structure_gel.core.structure.StructureInfo;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SendStructureInfoToClientPacket(StructureInfo structureInfo) implements CustomPacketPayload
{
	public static final Type<SendStructureInfoToClientPacket> TYPE = new Type<>(StructureGelMod.locate("send_structure_info"));

	public static final StreamCodec<RegistryFriendlyByteBuf, SendStructureInfoToClientPacket> CODEC = new StreamCodec<>()
	{
		@Override
		public void encode(RegistryFriendlyByteBuf buff, SendStructureInfoToClientPacket packet)
		{
			StructureInfo info = packet.structureInfo;

			if (info.dimension() != null)
			{
				buff.writeResourceLocation(info.dimension());

				writeBB(info.fullBounds(), buff);

				int count = info.pieces().size();
				buff.writeInt(count);

				for (int i = 0; i < count; i++)
				{
					StructureInfo.PieceInfo pieceInfo = info.pieces().get(i);
					writeBB(pieceInfo.bounds(), buff);
					buff.writeInt(pieceInfo.color());
				}
			}
		}
		
		private static void writeBB(BoundingBox bb, FriendlyByteBuf buff)
		{
			buff.writeInt(bb.minX());
			buff.writeInt(bb.minY());
			buff.writeInt(bb.minZ());
			buff.writeInt(bb.maxX());
			buff.writeInt(bb.maxY());
			buff.writeInt(bb.maxZ());
		}
		
		@Override
		public SendStructureInfoToClientPacket decode(RegistryFriendlyByteBuf buff)
		{
			ResourceLocation dimensionType = buff.readResourceLocation();

			BoundingBox fullBounds = readBB(buff);

			int count = buff.readInt();
			List<StructureInfo.PieceInfo> pieces = new ArrayList<>(count);
			for (int i = 0; i < count; i++)
			{
				BoundingBox pieceBounds = readBB(buff);
				int pieceColor = buff.readInt();
				pieces.add(new StructureInfo.PieceInfo(pieceBounds, pieceColor));
			}
			return new SendStructureInfoToClientPacket(new StructureInfo(dimensionType, fullBounds, pieces));
		}
		
		private static BoundingBox readBB(FriendlyByteBuf buff)
		{
			return new BoundingBox(buff.readInt(), buff.readInt(), buff.readInt(), buff.readInt(), buff.readInt(), buff.readInt());
		}
	};

	@Override
	public Type<? extends CustomPacketPayload> type()
	{
		return TYPE;
	}

	public static void handler(SendStructureInfoToClientPacket packet, IPayloadContext context)
	{
		if (FMLEnvironment.dist == Dist.CLIENT)
			context.enqueueWork(() -> StructureBoundsRenderer.addInfo(packet.structureInfo));
	}
}
