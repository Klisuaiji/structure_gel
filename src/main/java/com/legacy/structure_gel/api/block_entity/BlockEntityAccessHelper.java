package com.legacy.structure_gel.api.block_entity;

import com.legacy.structure_gel.api.dynamic_spawner.DynamicSpawnerType;
import com.legacy.structure_gel.core.block.DynamicSpawnerBlock;
import com.legacy.structure_gel.core.block_entity.DynamicSpawnerBlockEntity;
import com.legacy.structure_gel.core.registry.SGRegistry;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;

public class BlockEntityAccessHelper {
   public static void modifyNBT(BlockEntity blockEntity, Consumer<CompoundTag> tagConsumer) {
      CompoundTag tag = blockEntity.saveWithoutMetadata();
      tagConsumer.accept(tag);
      blockEntity.load(tag);
   }

   public static void placeDynamicSpawner(ServerLevelAccessor level, BlockPos pos, DynamicSpawnerType type) {
      level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
      level.setBlock(pos, ((DynamicSpawnerBlock)SGRegistry.Blocks.DYNAMIC_SPAWNER.get()).defaultBlockState(), 2);
      setDynamicSpawnerType(level, pos, type);
   }

   public static void setDynamicSpawnerType(ServerLevelAccessor level, BlockPos pos, DynamicSpawnerType type) {
      if (level.getBlockEntity(pos) instanceof DynamicSpawnerBlockEntity dynSpawner) {
         dynSpawner.setSpawner(type);
      }
   }

   public static void convertDynamicSpawnerToVanilla(ServerLevelAccessor level, BlockPos pos) {
      if (level.getBlockState(pos).is((Block)SGRegistry.Blocks.DYNAMIC_SPAWNER.get())
         && level.getBlockEntity(pos) instanceof DynamicSpawnerBlockEntity dynSpawner) {
         CompoundTag dynSpawnerTag = dynSpawner.getSpawner().save(new CompoundTag());
         level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
         level.setBlock(pos, Blocks.SPAWNER.defaultBlockState(), 2);
         if (level.getBlockEntity(pos) instanceof SpawnerBlockEntity spawnerBE) {
            spawnerBE.getSpawner().load(null, pos, dynSpawnerTag);
         }
      }
   }
}
