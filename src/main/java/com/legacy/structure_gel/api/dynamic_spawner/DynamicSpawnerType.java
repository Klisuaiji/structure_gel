package com.legacy.structure_gel.api.dynamic_spawner;

import com.legacy.structure_gel.core.block.DynamicSpawnerBlock;
import com.legacy.structure_gel.core.dynamic_spawner.DynamicSpawner;
import com.legacy.structure_gel.core.registry.SGRegistry;
import com.legacy.structure_gel.core.util.Internal;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class DynamicSpawnerType {
   private final DynamicSpawnerType.SpawnerMaker spawnerMaker;

   public DynamicSpawnerType(DynamicSpawnerType.SpawnerModifier spawnerModifier) {
      this.spawnerMaker = (level, pos) -> DynamicSpawnerType.SpawnerMaker.makeSpawner(spawnerModifier, level, pos);
   }

   @Internal
   public DynamicSpawner createSpawner(@Nullable Level level, BlockPos pos) {
      return this.spawnerMaker.make(level, pos);
   }

   private interface SpawnerMaker {
      DynamicSpawner make(@Nullable Level var1, BlockPos var2);

      private static DynamicSpawner makeSpawner(DynamicSpawnerType.SpawnerModifier modifier, @Nullable Level level, BlockPos pos) {
         BlockState state = level != null ? level.getBlockState(pos) : ((DynamicSpawnerBlock)SGRegistry.Blocks.DYNAMIC_SPAWNER.get()).defaultBlockState();
         if (!state.is((Block)SGRegistry.Blocks.DYNAMIC_SPAWNER.get())) {
            state = ((DynamicSpawnerBlock)SGRegistry.Blocks.DYNAMIC_SPAWNER.get()).defaultBlockState();
         }

         SpawnerBlockEntity spawnerBE = new SpawnerBlockEntity(pos, state);
         modifier.modify(spawnerBE, level, pos);
         DynamicSpawner spawner = new DynamicSpawner();
         spawner.load(level, pos, spawnerBE.getSpawner().save(new CompoundTag()));
         return spawner;
      }
   }

   public interface SpawnerModifier {
      void modify(SpawnerBlockEntity var1, @Nullable Level var2, BlockPos var3);
   }
}
