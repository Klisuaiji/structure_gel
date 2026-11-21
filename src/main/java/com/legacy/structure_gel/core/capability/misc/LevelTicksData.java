package com.legacy.structure_gel.core.capability.misc;

import net.minecraft.world.level.Level;

public interface LevelTicksData {
   boolean structure_gel$shouldSchedule();

   void structure_gel$setShouldSchedule(boolean var1);

   static void shouldScheduleTicks(Level level, boolean shouldSchedule) {
      ((LevelTicksData)level.getBlockTicks()).structure_gel$setShouldSchedule(shouldSchedule);
      ((LevelTicksData)level.getFluidTicks()).structure_gel$setShouldSchedule(shouldSchedule);
   }
}
