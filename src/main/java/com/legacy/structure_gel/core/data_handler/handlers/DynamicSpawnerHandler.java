package com.legacy.structure_gel.core.data_handler.handlers;

import com.legacy.structure_gel.api.data_handler.handlers.DataHandler;
import com.legacy.structure_gel.api.data_handler.handlers.ProbabilityDataHandler;
import com.legacy.structure_gel.api.data_handler.parsing.DataMap;
import com.legacy.structure_gel.api.data_handler.parsing.DataParser;
import com.legacy.structure_gel.api.dynamic_spawner.DynamicSpawnerType;
import com.legacy.structure_gel.api.registry.StructureGelRegistries;
import com.legacy.structure_gel.core.block.DynamicSpawnerBlock;
import com.legacy.structure_gel.core.block_entity.DynamicSpawnerBlockEntity;
import com.legacy.structure_gel.core.registry.SGRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;

public class DynamicSpawnerHandler extends ProbabilityDataHandler<DynamicSpawnerHandler> {
   private static final DataParser PARSER = DataParser.of(SGRegistry.DataHandlerTypes.DYNAMIC_SPAWNER, parser -> {
      parser.add("dynamic_spawner_type", StructureGelRegistries.Keys.DYNAMIC_SPAWNER_TYPE, SGRegistry.DynamicSpawnerTypes.DEFAULT.get());
      parser.add("generation_chance", 1.0F, 0.0F, 1.0F);
      parser.add("min_exp", DynamicSpawnerBlockEntity.ExpRange.VANILLA.min, 0, Integer.MAX_VALUE);
      parser.add("max_exp", DynamicSpawnerBlockEntity.ExpRange.VANILLA.max, 0, Integer.MAX_VALUE);
   });
   private ResourceKey<DynamicSpawnerType> dynamicSpawnerType;
   private DynamicSpawnerBlockEntity.ExpRange expRange;

   public DynamicSpawnerHandler(DataMap data) {
      super(data, "generation_chance");
      this.dynamicSpawnerType = data.get("dynamic_spawner_type", StructureGelRegistries.Keys.DYNAMIC_SPAWNER_TYPE);
      this.expRange = new DynamicSpawnerBlockEntity.ExpRange(data.getInt("min_exp"), data.getInt("max_exp"));
   }

   public static DataParser parser() {
      return PARSER;
   }

   @Override
   protected void handle(DataHandler.Context context) {
      WorldGenLevel level = context.getLevel();
      BlockPos pos = context.getBlockPos();
      level.setBlock(pos, ((DynamicSpawnerBlock)SGRegistry.Blocks.DYNAMIC_SPAWNER.get()).defaultBlockState(), 2);
      if (level.getBlockEntity(pos) instanceof DynamicSpawnerBlockEntity dynamicSpawner) {
         dynamicSpawner.setSpawner((DynamicSpawnerType)StructureGelRegistries.DYNAMIC_SPAWNER_TYPE.get(this.dynamicSpawnerType));
         dynamicSpawner.setExpRange(this.expRange);
         dynamicSpawner.setChanged();
      }

      if (level instanceof ServerLevel) {
         BlockState updatedState = level.getBlockState(pos);
         ((ServerLevel)level).sendBlockUpdated(pos, updatedState, updatedState, 3);
      }
   }
}
