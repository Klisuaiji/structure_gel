package com.legacy.structure_gel.api.block_entity;

import java.util.Optional;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.util.random.SimpleWeightedRandomList.Builder;
import net.minecraft.util.random.WeightedEntry.Wrapper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.SpawnData;
import net.minecraft.world.level.SpawnData.CustomSpawnRules;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;

public class SpawnerAccessHelper {
   public static void setMinSpawnDelay(SpawnerBlockEntity tile, @Nullable Level level, BlockPos pos, int minSpawnelay) {
      modifySpawnerNBT(tile, level, pos, nbt -> nbt.putShort("MinSpawnDelay", (short)minSpawnelay));
   }

   public static void setMaxSpawnDelay(SpawnerBlockEntity tile, @Nullable Level level, BlockPos pos, int maxSpawnDelay) {
      modifySpawnerNBT(tile, level, pos, nbt -> nbt.putShort("MaxSpawnDelay", (short)maxSpawnDelay));
   }

   public static void setSpawnCount(SpawnerBlockEntity tile, @Nullable Level level, BlockPos pos, int spawnCount) {
      modifySpawnerNBT(tile, level, pos, nbt -> nbt.putShort("SpawnCount", (short)spawnCount));
   }

   public static void setMaxNearbyEntities(SpawnerBlockEntity tile, @Nullable Level level, BlockPos pos, int maxNearbyEntities) {
      modifySpawnerNBT(tile, level, pos, nbt -> nbt.putShort("MaxNearbyEntities", (short)maxNearbyEntities));
   }

   public static void setRequiredPlayerRange(SpawnerBlockEntity tile, @Nullable Level level, BlockPos pos, int requiredPlayerRange) {
      modifySpawnerNBT(tile, level, pos, nbt -> nbt.putShort("RequiredPlayerRange", (short)requiredPlayerRange));
   }

   public static void setSpawnRange(SpawnerBlockEntity tile, @Nullable Level level, BlockPos pos, int spawnRange) {
      modifySpawnerNBT(tile, level, pos, nbt -> nbt.putShort("SpawnRange", (short)spawnRange));
   }

   public static void setSpawnPotentials(SpawnerBlockEntity tile, @Nullable Level level, BlockPos pos, EntityType<?> entityType) {
      setSpawnPotentials(tile, level, pos, createSpawnerEntity(entityType));
   }

   public static void setSpawnPotentials(SpawnerBlockEntity tile, @Nullable Level level, BlockPos pos, SpawnData... spawnDatas) {
      SimpleWeightedRandomList<SpawnData> list = ((Builder)Util.make(new Builder(), builder -> {
         for (SpawnData data : spawnDatas) {
            builder.add(data, 1);
         }
      })).build();
      setSpawnPotentials(tile, level, pos, list);
   }

   public static void setSpawnPotentials(SpawnerBlockEntity tile, @Nullable Level level, BlockPos pos, SimpleWeightedRandomList<SpawnData> spawnDatas) {
      modifySpawnerNBT(
         tile,
         level,
         pos,
         nbt -> {
            if (!spawnDatas.isEmpty()) {
               nbt.put(
                  "SpawnData",
                  (Tag)SpawnData.CODEC
                     .encodeStart(
                        NbtOps.INSTANCE, (SpawnData)((Wrapper)spawnDatas.getRandom(level != null ? level.random : RandomSource.create()).get()).getData()
                     )
                     .result()
                     .orElseThrow(() -> new IllegalStateException("Invalid SpawnData"))
               );
               nbt.put("SpawnPotentials", (Tag)SpawnData.LIST_CODEC.encodeStart(NbtOps.INSTANCE, spawnDatas).result().orElseThrow());
            }
         }
      );
   }

   public static SpawnData createSpawnerEntity(EntityType<?> entity) {
      return createSpawnerEntity(entity, new CompoundTag(), Optional.empty());
   }

   public static SpawnData createSpawnerEntity(EntityType<?> entity, CompoundTag entityTag, Optional<CustomSpawnRules> customSpawnRules) {
      CompoundTag nbt = entityTag.copy();
      ResourceLocation name = BuiltInRegistries.ENTITY_TYPE.getKey(entity);
      nbt.putString("id", name != null ? BuiltInRegistries.ENTITY_TYPE.getKey(entity).toString() : "pig");
      return new SpawnData(nbt, customSpawnRules);
   }

   public static void modifySpawnerNBT(SpawnerBlockEntity spawner, @Nullable Level level, BlockPos pos, Consumer<CompoundTag> nbtConsumer) {
      CompoundTag nbt = new CompoundTag();
      spawner.getSpawner().save(nbt);
      nbtConsumer.accept(nbt);
      spawner.getSpawner().load(null, pos, nbt);
   }
}
