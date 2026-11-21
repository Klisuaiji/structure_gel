package com.legacy.structure_gel.core.dynamic_spawner;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.SpawnData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class DynamicSpawner extends BaseSpawner implements Cloneable {
   public void broadcastEvent(Level level, BlockPos pos, int param) {
      level.blockEvent(pos, Blocks.SPAWNER, param, 0);
   }

   public void setNextSpawnData(Level level, BlockPos pos, SpawnData spawnData) {
      super.setNextSpawnData(level, pos, spawnData);
      if (level != null) {
         BlockState state = level.getBlockState(pos);
         level.sendBlockUpdated(pos, state, state, 4);
      }
   }

   @Nullable
   public BlockEntity getSpawnerBlockEntity() {
      return null;
   }

   protected DynamicSpawner clone() {
      DynamicSpawner clone = new DynamicSpawner();
      clone.load(null, BlockPos.ZERO, this.save(new CompoundTag()));
      return clone;
   }
}
