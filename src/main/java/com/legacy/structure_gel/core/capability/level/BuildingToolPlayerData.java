package com.legacy.structure_gel.core.capability.level;

import com.legacy.structure_gel.core.SGConfig;
import com.legacy.structure_gel.core.StructureGelMod;
import com.legacy.structure_gel.core.item.building_tool.ActionHistory;
import com.legacy.structure_gel.core.item.building_tool.CapturedBlocks;
import com.legacy.structure_gel.core.mixin.common.DimensionDataStorageAccessor;
import com.legacy.structure_gel.core.network.SGPacketHandler;
import com.legacy.structure_gel.core.network.s_to_c.EditClipboardPacket;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectBidirectionalIterator;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap.Entry;
import java.io.File;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedData.Factory;

public class BuildingToolPlayerData extends SavedData {
   final ServerLevel level;
   final String playerName;
   final ActionHistory actionHistory;
   final Object2ObjectLinkedOpenHashMap<BoundingBox, CapturedBlocks> clipboard;
   private static final String PLAYER_KEY = "player";
   private static final String ACTION_HISTORY_KEY = "action_history";
   private static final String CLIPBOARD_KEY = "clipboard";
   private static final String BOUNDS_KEY = "bounds";
   private static final String CAPTURED_KEY = "captured";

   private BuildingToolPlayerData(ServerLevel level, String playerName) {
      this(level, playerName, new ActionHistory(playerName), new Object2ObjectLinkedOpenHashMap());
   }

   private BuildingToolPlayerData(
      ServerLevel level, String playerName, ActionHistory actionHistory, Object2ObjectLinkedOpenHashMap<BoundingBox, CapturedBlocks> clipboard
   ) {
      this.level = level;
      this.playerName = playerName;
      this.actionHistory = actionHistory;
      this.clipboard = clipboard;
   }

   public static String getID(String playerName) {
      return "structure_gel/building_tool/players/" + playerName;
   }

   private static Factory<BuildingToolPlayerData> factory(ServerLevel level, String playerName) {
      return new Factory(() -> new BuildingToolPlayerData(level, playerName), tag -> load(level, tag));
   }

   public static BuildingToolPlayerData get(ServerLevel level, String playerName) {
      String ID = getID(playerName);
      Map<String, SavedData> cache = ((DimensionDataStorageAccessor)level.getDataStorage()).structure_gel$getCache();
      if (cache.containsKey(ID) && cache.get(ID) == null) {
         cache.remove(ID);
      }

      BuildingToolPlayerData ret = (BuildingToolPlayerData)level.getDataStorage().computeIfAbsent(factory(level, playerName), ID);
      BuildingToolWorldData.get(level).addPlayer(playerName);
      return ret;
   }

   public Player getPlayer(Level level) {
      for (Player player : level.players()) {
         if (player.getGameProfile().getName().equals(this.playerName)) {
            return player;
         }
      }

      return null;
   }

   public ActionHistory getActionHistory() {
      return this.actionHistory;
   }

   @Nullable
   public CapturedBlocks getCapturedBlocks(BoundingBox bounds) {
      CapturedBlocks ret = (CapturedBlocks)this.clipboard.getAndMoveToFirst(bounds);
      if (ret != null) {
         this.setDirty();
      }

      return ret;
   }

   public void addToClipboard(BoundingBox bounds, CapturedBlocks captured) {
      this.clipboard.putAndMoveToFirst(bounds, captured);
      BoundingBox removeKey = BoundingBox.infinite();
      if (this.clipboard.size() > SGConfig.COMMON.getBuildingToolMaxClipboard()) {
         removeKey = (BoundingBox)this.clipboard.lastKey();
         this.clipboard.removeLast();
      }

      if (this.getPlayer(this.level) instanceof ServerPlayer player) {
         SGPacketHandler.sendToClient(new EditClipboardPacket(this.level, bounds, captured, removeKey), player);
      }

      this.setDirty();
   }

   public void addToClipboardDirect(BoundingBox bounds, CapturedBlocks captured) {
      this.clipboard.putAndMoveToFirst(bounds, captured);
      this.setDirty();
   }

   public void removeFromClipboard(BoundingBox bounds) {
      this.clipboard.remove(bounds);
      this.setDirty();
   }

   public void clearClipboard() {
      this.clipboard.clear();
      this.setDirty();
   }

   public CompoundTag save(CompoundTag tag) {
      tag.putString("player", this.playerName);
      if (!this.actionHistory.isExpired(this.level)) {
         tag.put(
            "action_history",
            (Tag)ActionHistory.CODEC
               .encodeStart(NbtOps.INSTANCE, this.actionHistory)
               .getOrThrow(false, s -> StructureGelMod.LOGGER.error("couldn't serialize action history for " + this.playerName + ": " + s))
         );
      }

      if (!this.clipboard.isEmpty()) {
         ListTag clipboardList = new ListTag();
         ObjectBidirectionalIterator var3 = this.clipboard.object2ObjectEntrySet().iterator();

         while (var3.hasNext()) {
            Entry<BoundingBox, CapturedBlocks> entry = (Entry<BoundingBox, CapturedBlocks>)var3.next();
            CompoundTag clipboardTag = new CompoundTag();
            BoundingBox b = (BoundingBox)entry.getKey();
            clipboardTag.putIntArray("bounds", new int[]{b.minX(), b.minY(), b.minZ(), b.maxX(), b.maxY(), b.maxZ()});
            clipboardTag.put("captured", ((CapturedBlocks)entry.getValue()).toCompressedTag(this.level.registryAccess()));
            clipboardList.add(clipboardTag);
         }

         tag.put("clipboard", clipboardList);
      }

      return tag;
   }

   private static BuildingToolPlayerData load(ServerLevel level, CompoundTag tag) {
      String playerName = tag.getString("player");
      ActionHistory actionHistory = tag.contains("action_history", 10)
         ? (ActionHistory)((Pair)ActionHistory.CODEC
               .decode(NbtOps.INSTANCE, tag.getCompound("action_history"))
               .getOrThrow(false, s -> StructureGelMod.LOGGER.error("couldn't deserialize action history: " + s)))
            .getFirst()
         : new ActionHistory(playerName);
      Object2ObjectLinkedOpenHashMap<BoundingBox, CapturedBlocks> clipboard = new Object2ObjectLinkedOpenHashMap();
      if (tag.contains("clipboard", 9)) {
         for (Tag t : tag.getList("clipboard", 10)) {
            if (t instanceof CompoundTag clipboardTag) {
               int[] boundsArray = clipboardTag.getIntArray("bounds");
               BoundingBox bounds = new BoundingBox(boundsArray[0], boundsArray[1], boundsArray[2], boundsArray[3], boundsArray[4], boundsArray[5]);
               CapturedBlocks captured = CapturedBlocks.fromCompressedTag(level.registryAccess(), clipboardTag.getCompound("captured"));
               clipboard.put(bounds, captured);
            }
         }
      }

      return new BuildingToolPlayerData(level, playerName, actionHistory, clipboard);
   }

   public boolean isDirty() {
      return super.isDirty() || this.actionHistory.isDirty();
   }

   public void save(File file) {
      file.getParentFile().mkdirs();
      super.save(file);
      this.actionHistory.setDirty(false);
      if (this.actionHistory.isExpired(this.level)) {
         this.level.getDataStorage().set(getID(this.playerName), null);
      }
   }

   public static class BuildingToolClientData extends BuildingToolPlayerData {
      public BuildingToolClientData(String playerName) {
         super(null, playerName);
      }

      @Override
      public CompoundTag save(CompoundTag tag) {
         return tag;
      }

      @Override
      public void save(File file) {
      }
   }
}
