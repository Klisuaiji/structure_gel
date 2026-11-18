package com.legacy.structure_gel.api.structure.base;

import com.google.common.collect.UnmodifiableIterator;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

public interface IModifyState {
   IModifyState NOOP = (level, rand, pos, originalState) -> originalState;

   @Nullable
   BlockState modifyState(ServerLevelAccessor var1, RandomSource var2, BlockPos var3, BlockState var4);

   static BlockState mergeStates(BlockState to, BlockState from) {
      BlockState state = to;
      UnmodifiableIterator var3 = from.getValues().entrySet().iterator();

      while (var3.hasNext()) {
         Entry<Property<?>, Comparable<?>> entry = (Entry<Property<?>, Comparable<?>>)var3.next();
         Property property = entry.getKey();
         if (state.hasProperty(property)) {
            state = (BlockState)state.setValue(property, entry.getValue());
         }
      }

      return state;
   }
}
