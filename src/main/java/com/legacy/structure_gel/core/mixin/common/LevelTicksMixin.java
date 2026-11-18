package com.legacy.structure_gel.core.mixin.common;

import com.legacy.structure_gel.core.capability.misc.LevelTicksData;
import net.minecraft.world.ticks.LevelTicks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({LevelTicks.class})
public class LevelTicksMixin implements LevelTicksData {
   private boolean structure_gel$shouldSchedule = true;

   @Override
   public boolean structure_gel$shouldSchedule() {
      return this.structure_gel$shouldSchedule;
   }

   @Override
   public void structure_gel$setShouldSchedule(boolean shouldSchedule) {
      this.structure_gel$shouldSchedule = shouldSchedule;
   }

   @Inject(
      at = {@At("HEAD")},
      method = {"schedule"},
      cancellable = true
   )
   private void cancelSchedule(CallbackInfo callback) {
      if (!this.structure_gel$shouldSchedule) {
         callback.cancel();
      }
   }
}
