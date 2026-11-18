package com.legacy.structure_gel.core.capability.entity;

import com.legacy.structure_gel.api.block.GelPortalBlock;
import com.legacy.structure_gel.core.StructureGelMod;
import com.legacy.structure_gel.core.network.SGPacketHandler;
import com.legacy.structure_gel.core.network.s_to_c.UpdateGelPlayerPacket;
import com.legacy.structure_gel.core.util.Internal;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;

public class GelEntity {
   @Nullable
   private GelPortalBlock portal;
   @Nullable
   private static GelPortalBlock portalClient;
   private static int portalTimer = 0;

   @Nullable
   public GelPortalBlock getPortal() {
      return this.portal;
   }

   public void setPortal(@Nullable GelPortalBlock portal) {
      this.portal = portal;
   }

   @Nullable
   public static GelPortalBlock getPortalClient() {
      return portalClient;
   }

   @Internal
   public static void setPortalClient(Entity entity, GelPortalBlock portal) {
      if (entity.level().isClientSide()) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.player == entity) {
            portalClient = portal;
            portalTimer = 60;
         }
      }
   }

   @Internal
   public static void clientTick() {
      if (portalTimer > 0) {
         portalTimer--;
         if (portalTimer == 0) {
            portalClient = null;
         }
      }
   }

   public void copyFrom(GelEntity gelEntity) {
      this.portal = gelEntity.getPortal();
   }

   public void sendToClient(ServerPlayer player) {
      SGPacketHandler.sendToClient(new UpdateGelPlayerPacket(this), player);
   }

   public static class Serializer implements IAttachmentSerializer<CompoundTag, GelEntity> {
      public static final GelEntity.Serializer INSTANCE = new GelEntity.Serializer();
      private static final ResourceLocation EMPTY = StructureGelMod.locate("empty");
      private static final String PORTAL_KEY = "portal";

      public CompoundTag write(GelEntity gelEntity) {
         CompoundTag tag = new CompoundTag();
         ResourceLocation name = null;
         if (gelEntity.portal != null) {
            name = BuiltInRegistries.BLOCK.getKey(gelEntity.portal);
         }

         tag.putString("portal", name != null ? name.toString() : EMPTY.toString());
         return tag;
      }

      public GelEntity read(@Nullable IAttachmentHolder holder, CompoundTag tag) {
         GelEntity gelEntity = new GelEntity();
         ResourceLocation portal = new ResourceLocation(tag.getString("portal"));
         gelEntity.portal = !portal.equals(EMPTY)
               && BuiltInRegistries.BLOCK.containsKey(portal)
               && BuiltInRegistries.BLOCK.get(portal) instanceof GelPortalBlock gelPortalBlock
            ? gelPortalBlock
            : null;
         return gelEntity;
      }
   }
}
