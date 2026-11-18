package com.legacy.structure_gel.core.network.c_to_s;

import com.legacy.structure_gel.core.StructureGelMod;
import com.legacy.structure_gel.core.item.building_tool.ActionHistory;
import com.legacy.structure_gel.core.item.building_tool.BuildingToolBounds;
import com.legacy.structure_gel.core.item.building_tool.BuildingToolItem;
import com.legacy.structure_gel.core.item.building_tool.BuildingToolModes;
import com.legacy.structure_gel.core.item.building_tool.modes.CloneTool;
import com.legacy.structure_gel.core.network.SGPacketHandler;
import com.legacy.structure_gel.core.network.s_to_c.PlaySoundPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.PlayPayloadContext;

public class ActionHistoryPacket implements SGPacketHandler.ModPacket<ActionHistoryPacket, ActionHistoryPacket.Handler> {
   private final ActionHistoryPacket.Action action;
   private final InteractionHand hand;
   private final boolean playSound;

   private ActionHistoryPacket(ActionHistoryPacket.Action action, InteractionHand hand, boolean playSound) {
      this.action = action;
      this.hand = hand;
      this.playSound = playSound;
   }

   public static ActionHistoryPacket undo(boolean playSound) {
      return new ActionHistoryPacket(ActionHistoryPacket.Action.UNDO, InteractionHand.MAIN_HAND, playSound);
   }

   public static ActionHistoryPacket redo(boolean playSound) {
      return new ActionHistoryPacket(ActionHistoryPacket.Action.REDO, InteractionHand.MAIN_HAND, playSound);
   }

   public static ActionHistoryPacket releaseGrabbedCorner(InteractionHand hand) {
      return new ActionHistoryPacket(ActionHistoryPacket.Action.RELEASE_GRABBED_CORNER, hand, false);
   }

   public static ActionHistoryPacket copy(InteractionHand hand, boolean playSound) {
      return new ActionHistoryPacket(ActionHistoryPacket.Action.COPY, hand, playSound);
   }

   public ActionHistoryPacket.Handler handler() {
      return new ActionHistoryPacket.Handler();
   }

   public static enum Action {
      NONE,
      UNDO,
      REDO,
      RELEASE_GRABBED_CORNER,
      COPY;

      public static ActionHistoryPacket.Action get(int id) {
         ActionHistoryPacket.Action[] actions = values();
         return id > -1 && id < actions.length ? actions[id] : NONE;
      }
   }

   public static class Handler implements SGPacketHandler.PlayHandler<ActionHistoryPacket> {
      @Override
      public ResourceLocation id() {
         return StructureGelMod.locate("action_history");
      }

      public void encoder(ActionHistoryPacket packet, FriendlyByteBuf buff) {
         buff.writeInt(packet.action.ordinal());
         buff.writeBoolean(packet.hand == InteractionHand.MAIN_HAND);
         buff.writeBoolean(packet.playSound);
      }

      public ActionHistoryPacket decoder(FriendlyByteBuf buff) {
         return new ActionHistoryPacket(
            ActionHistoryPacket.Action.get(buff.readInt()), buff.readBoolean() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND, buff.readBoolean()
         );
      }

      public void handler(ActionHistoryPacket packet, PlayPayloadContext context) {
         context.workHandler().submitAsync(() -> {
            ServerPlayer player = this.serverPlayer(context);
            ActionHistory history = ActionHistory.get(player);
            ItemStack item = player.getItemInHand(packet.hand);
            boolean succeeded = false;
            switch (packet.action) {
               case UNDO:
                  succeeded = history.undo(player);
                  break;
               case REDO:
                  succeeded = history.redo(player);
                  break;
               case RELEASE_GRABBED_CORNER:
                  BuildingToolBounds.releaseGrabbedCorner(item, player);
                  break;
               case COPY:
                  if (BuildingToolItem.getMode(item) == BuildingToolModes.CLONE) {
                     CloneTool.saveCapturedBlocks(item, player.level(), player);
                     player.displayClientMessage(Component.translatable("info.structure_gel.building_tool.message.copy_region"), true);
                     succeeded = true;
                  }
            }

            if (succeeded && packet.playSound) {
               SGPacketHandler.sendToClient(new PlaySoundPacket((byte)0), player);
            }
         });
      }
   }
}
