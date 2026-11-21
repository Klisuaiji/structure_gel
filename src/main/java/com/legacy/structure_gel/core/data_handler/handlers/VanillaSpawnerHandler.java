package com.legacy.structure_gel.core.data_handler.handlers;

import com.legacy.structure_gel.api.block_entity.SpawnerAccessHelper;
import com.legacy.structure_gel.api.data_handler.handlers.DataHandler;
import com.legacy.structure_gel.api.data_handler.handlers.ProbabilityDataHandler;
import com.legacy.structure_gel.api.data_handler.parsing.DataMap;
import com.legacy.structure_gel.api.data_handler.parsing.DataParser;
import com.legacy.structure_gel.core.registry.SGRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class VanillaSpawnerHandler extends ProbabilityDataHandler<VanillaSpawnerHandler> {
   private static final DataParser PARSER = DataParser.of(SGRegistry.DataHandlerTypes.SPAWNER, parser -> {
      parser.add("entity", Registries.ENTITY_TYPE, EntityType.PIG);
      parser.add("generation_chance", 1.0F, 0.0F, 1.0F);
      parser.addShort("min_spawn_delay", (short)200, (short)0, (short)32767);
      parser.addShort("max_spawn_delay", (short)800, (short)0, (short)32767);
      parser.addShort("spawn_count", (short)4, (short)0, (short)32767);
      parser.addShort("max_nearby_entities", (short)6, (short)0, (short)32767);
      parser.addShort("required_player_range", (short)16, (short)0, (short)32767);
      parser.addShort("spawn_range", (short)4, (short)0, (short)32767);
      parser.add("tag", new CompoundTag());
   });
   private final ResourceKey<EntityType<?>> entityType;
   private final short minSpawnDelay;
   private final short maxSpawnDelay;
   private final short spawnCount;
   private final short maxNearbyEntities;
   private final short requiredPlayerRange;
   private final short spawnRange;
   private final CompoundTag tag;

   public VanillaSpawnerHandler(DataMap data) {
      super(data, "generation_chance");
      this.entityType = data.get("entity", Registries.ENTITY_TYPE);
      this.minSpawnDelay = data.getShort("min_spawn_delay");
      this.maxSpawnDelay = (short)Math.max(this.minSpawnDelay, data.getShort("max_spawn_delay"));
      this.spawnCount = data.getShort("spawn_count");
      this.maxNearbyEntities = data.getShort("max_nearby_entities");
      this.requiredPlayerRange = data.getShort("required_player_range");
      this.spawnRange = data.getShort("spawn_range");
      this.tag = data.get("tag", CompoundTag.class);
   }

   public static DataParser parser() {
      return PARSER;
   }

   @Override
   protected void handle(DataHandler.Context context) {
      WorldGenLevel level = context.getLevel();
      BlockPos pos = context.getBlockPos();
      RandomSource rand = context.getRandom();
      level.setBlock(pos, Blocks.SPAWNER.defaultBlockState(), 2);
      if (level.getBlockEntity(pos) instanceof SpawnerBlockEntity spawner) {
         EntityType<?> entity = (EntityType<?>)level.registryAccess().registryOrThrow(Registries.ENTITY_TYPE).get(this.entityType);
         if (entity != null) {
            spawner.setEntityId(entity, rand);
         }

         SpawnerAccessHelper.setMinSpawnDelay(spawner, null, pos, this.minSpawnDelay);
         SpawnerAccessHelper.setMaxSpawnDelay(spawner, null, pos, this.maxSpawnDelay);
         SpawnerAccessHelper.setSpawnCount(spawner, null, pos, this.spawnCount);
         SpawnerAccessHelper.setMaxNearbyEntities(spawner, null, pos, this.maxNearbyEntities);
         SpawnerAccessHelper.setRequiredPlayerRange(spawner, null, pos, this.requiredPlayerRange);
         SpawnerAccessHelper.setSpawnRange(spawner, null, pos, this.spawnRange);
         spawner.load(this.tag);
         spawner.setChanged();
      }

      if (level instanceof ServerLevel) {
         BlockState updatedState = level.getBlockState(pos);
         ((ServerLevel)level).sendBlockUpdated(pos, updatedState, updatedState, 3);
      }
   }
}
