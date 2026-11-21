package com.legacy.structure_gel.core.capability.level;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.legacy.structure_gel.core.StructureGelMod;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

public class BuildingToolWorldData extends SavedData
{
	final Set<String> players;

	public BuildingToolWorldData()
	{
		this(Collections.emptySet());
	}

	public BuildingToolWorldData(Collection<String> players)
	{
		this.players = new HashSet<>(players);
	}

	public static BuildingToolWorldData get(ServerLevel level)
	{
		return level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(BuildingToolWorldData::new, BuildingToolWorldData::load), StructureGelMod.MODID + "/building_tool/world_data");
	}

	public void addPlayer(String playerName)
	{
		this.players.add(playerName);
		this.setDirty();
	}

	public boolean removePlayer(String playerName)
	{
		boolean ret = this.players.remove(playerName);
		this.setDirty();
		return ret;
	}

	public Set<String> getPlayers()
	{
		return this.players;
	}
	
	public static final String PLAYERS_KEY = "players";

	public static BuildingToolWorldData load(CompoundTag tag, HolderLookup.Provider lookup)
	{
		return new BuildingToolWorldData(tag.getList(PLAYERS_KEY, Tag.TAG_STRING).stream().map(Tag::getAsString).toList());
	}

	@Override
	public CompoundTag save(CompoundTag tag, HolderLookup.Provider lookup)
	{
		ListTag playersTag = new ListTag();
		for (String player : this.players)
			playersTag.add(StringTag.valueOf(player));
		tag.put(PLAYERS_KEY, playersTag);

		return tag;
	}
}
