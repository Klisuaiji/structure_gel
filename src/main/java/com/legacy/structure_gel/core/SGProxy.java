package com.legacy.structure_gel.core;

import javax.annotation.Nullable;

import com.legacy.structure_gel.core.block_entity.DataHandlerBlockEntity;
import com.legacy.structure_gel.core.block_entity.DynamicSpawnerBlockEntity;
import com.legacy.structure_gel.core.capability.level.BuildingToolPlayerData;
import com.legacy.structure_gel.core.data_components.CloneRegion;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class SGProxy
{
	public void openDataHandlerScreen(DataHandlerBlockEntity dataHandler)
	{
	}

	public void openBuildingToolScreen(ItemStack stack, InteractionHand hand)
	{
	}

	public void setViewBounds(boolean shouldViewBounds)
	{
	}

	public boolean shouldViewBounds()
	{
		return false;
	}

	public void clearToolRenderCache()
	{
	}

	public void openDynamicSpawnerScreen(DynamicSpawnerBlockEntity spawner)
	{
	}
	
	@Nullable
	public BuildingToolPlayerData getBuildingToolData(@Nullable ServerLevel level, String player)
	{
		return level != null ? BuildingToolPlayerData.get(level, player) : null;
	}
	
	@Nullable
	public BuildingToolPlayerData getBuildingToolData(@Nullable MinecraftServer server, CloneRegion cloneRegion)
	{
		return server != null ? this.getBuildingToolData(server.getLevel(cloneRegion.levelKey()), cloneRegion.playerName()) : null;
	}

	public void clearRegistryKeyCache()
	{
	}
}
