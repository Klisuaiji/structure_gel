package com.legacy.structure_gel.core.network.s_to_c;

import com.legacy.structure_gel.api.util.NetworkUtil;
import com.legacy.structure_gel.core.StructureGelMod;
import com.legacy.structure_gel.core.capability.level.BuildingToolPlayerData;
import com.legacy.structure_gel.core.item.building_tool.CapturedBlocks;
import com.legacy.structure_gel.core.network.SGPacketHandler;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class EditClipboardPacket implements SGPacketHandler.ModPacket<EditClipboardPacket, EditClipboardPacket.Handler> {
   Level level;
   final BoundingBox addKey;
   final CapturedBlocks captured;
   final BoundingBox removeKey;

   public EditClipboardPacket(Level level, BoundingBox addKey, CapturedBlocks captured, BoundingBox removeKey) {
      this.level = level;
      this.addKey = addKey;
      this.captured = captured;
      this.removeKey = removeKey;
   }

   public EditClipboardPacket.Handler handler() {
      return new EditClipboardPacket.Handler();
   }

   public static class Handler implements SGPacketHandler.PlayHandler<EditClipboardPacket> {
      @Override
      public ResourceLocation id() {
         return StructureGelMod.locate("edit_clipboard");
      }

      public void encoder(EditClipboardPacket packet, FriendlyByteBuf buff) {
         NetworkUtil.writeBoundingBox(buff, packet.addKey);
         if (!packet.addKey.equals(BoundingBox.infinite()) && packet.captured != null) {
            CompoundTag tag = packet.captured.toCompressedTag(packet.level.registryAccess());
            NetworkUtil.writeCompressedNbt(buff, tag);
         }

         NetworkUtil.writeBoundingBox(buff, packet.removeKey);
      }

      public EditClipboardPacket decoder(FriendlyByteBuf buff) {
         if (FMLEnvironment.dist == Dist.CLIENT) {
            BoundingBox addKey = NetworkUtil.readBoundingBox(buff);
            CapturedBlocks captured = null;
            if (!addKey.equals(BoundingBox.infinite())) {
               CompoundTag tag = NetworkUtil.readCompressedNbt(buff, () -> null);
               if (tag != null) {
                  captured = CapturedBlocks.fromCompressedTag(Minecraft.getInstance().level.registryAccess(), tag);
               }
            }

            BoundingBox removeKey = NetworkUtil.readBoundingBox(buff);
            return new EditClipboardPacket(null, addKey, captured, removeKey);
         } else {
            return new EditClipboardPacket(null, BoundingBox.infinite(), null, BoundingBox.infinite());
         }
      }

      public void handler(EditClipboardPacket packet, IPayloadContext context) {
         if (FMLEnvironment.dist == Dist.CLIENT) {
            context.enqueueWork(() -> handleClient(packet, context));
         }
      }

      @OnlyIn(Dist.CLIENT)
      public static void handleClient(EditClipboardPacket packet, IPayloadContext context) {
         Player player = Minecraft.getInstance().player;
         BuildingToolPlayerData data = StructureGelMod.proxy.getBuildingToolData(player.level(), player.getGameProfile().getName());
         if (!packet.addKey.equals(BoundingBox.infinite()) && packet.captured != null) {
            data.addToClipboardDirect(packet.addKey, packet.captured);
         }

         if (!packet.removeKey.equals(BoundingBox.infinite())) {
            data.removeFromClipboard(packet.removeKey);
         }
      }
   }
}
