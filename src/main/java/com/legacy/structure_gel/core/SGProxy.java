package com.legacy.structure_gel.core;

import com.legacy.structure_gel.core.block_entity.DataHandlerBlockEntity;
import com.legacy.structure_gel.core.block_entity.DynamicSpawnerBlockEntity;
import com.legacy.structure_gel.core.capability.level.BuildingToolPlayerData;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class SGProxy {
   public void openDataHandlerScreen(DataHandlerBlockEntity dataHandler) {
   }

   public void openBuildingToolScreen(ItemStack stack, InteractionHand hand) {
   }

   public void setViewBounds(boolean shouldViewBounds) {
   }

   public boolean shouldViewBounds() {
      return false;
   }

   public void clearToolRenderCache() {
   }

   public void openDynamicSpawnerScreen(DynamicSpawnerBlockEntity spawner) {
   }

   @Nullable
   public BuildingToolPlayerData getBuildingToolData(Level level, String playerName) {
      return level instanceof ServerLevel sl ? BuildingToolPlayerData.get(sl, playerName) : null;
   }

   public void clearRegistryKeyCache() {
   }
}
