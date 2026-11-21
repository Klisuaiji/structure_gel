package com.legacy.structure_gel.core.capability.level;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedData.Factory;

public class BuildingToolWorldData extends SavedData {
   final Set<String> players;
   public static final String PLAYERS_KEY = "players";

   public BuildingToolWorldData() {
      this(Collections.emptySet());
   }

   public BuildingToolWorldData(Collection<String> players) {
      this.players = new HashSet<>(players);
   }

   public static BuildingToolWorldData get(ServerLevel level) {
      return (BuildingToolWorldData)level.getDataStorage()
         .computeIfAbsent(new Factory(BuildingToolWorldData::new, BuildingToolWorldData::load), "structure_gel/building_tool/world_data");
   }

   public void addPlayer(String playerName) {
      this.players.add(playerName);
      this.setDirty();
   }

   public boolean removePlayer(String playerName) {
      boolean ret = this.players.remove(playerName);
      this.setDirty();
      return ret;
   }

   public Set<String> getPlayers() {
      return this.players;
   }

   public static BuildingToolWorldData load(CompoundTag tag) {
      return new BuildingToolWorldData(tag.getList("players", 8).stream().<String>map(Tag::getAsString).toList());
   }

   public CompoundTag save(CompoundTag tag) {
      ListTag playersTag = new ListTag();

      for (String player : this.players) {
         playersTag.add(StringTag.valueOf(player));
      }

      tag.put("players", playersTag);
      return tag;
   }

   public void save(File file) {
      file.getParentFile().mkdirs();
      super.save(file);
   }
}
