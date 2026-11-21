package com.legacy.structure_gel.core.network.c_to_s;

import com.legacy.structure_gel.core.StructureGelMod;
import com.legacy.structure_gel.core.item.building_tool.BuildingToolItem;
import com.legacy.structure_gel.core.network.SGPacketHandler;
import com.legacy.structure_gel.core.registry.SGRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ExportPalettePacket implements SGPacketHandler.ModPacket<ExportPalettePacket, ExportPalettePacket.Handler> {
   final ItemStack palette;

   public ExportPalettePacket(ItemStack palette) {
      this.palette = palette;
   }

   public ExportPalettePacket.Handler handler() {
      return new ExportPalettePacket.Handler();
   }

   public static class Handler implements SGPacketHandler.PlayHandler<ExportPalettePacket> {
      @Override
      public ResourceLocation id() {
         return StructureGelMod.locate("export_palette");
      }

      public void encoder(ExportPalettePacket packet, FriendlyByteBuf buff) {
         buff.writeItem(packet.palette);
      }

      public ExportPalettePacket decoder(FriendlyByteBuf buff) {
         return new ExportPalettePacket(buff.readItem());
      }

      public void handler(ExportPalettePacket packet, IPayloadContext context) {
         context.enqueueWork(() -> {
            ServerPlayer player = this.serverPlayer(context);
            if (player != null && !context.level().isEmpty()) {
               if (BuildingToolItem.hasToolPermission(SGRegistry.Items.BUILDING_TOOL.getDefaultInstance(), player)) {
                  ItemStack paletteItem = packet.palette.copy();
                  if (!player.addItem(paletteItem)) {
                     player.drop(paletteItem, false);
                  }
               }
            }
         });
      }
   }
}
