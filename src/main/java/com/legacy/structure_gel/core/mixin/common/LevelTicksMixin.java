package com.legacy.structure_gel.core.mixin.common;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.legacy.structure_gel.core.capability.misc.LevelTicksData;

import net.minecraft.world.ticks.LevelTicks;
import net.minecraft.world.ticks.ScheduledTick;

@Mixin(LevelTicks.class)
public class LevelTicksMixin implements LevelTicksData
{
	// Prevents the building tool from scheduling ticks if block updates are disabled 

	private boolean structure_gel$shouldSchedule = true;

	@Override
	public boolean structure_gel$shouldSchedule()
	{
		return structure_gel$shouldSchedule;
	}

	@Override
	public void structure_gel$setShouldSchedule(boolean shouldSchedule)
	{
		structure_gel$shouldSchedule = shouldSchedule;
	}

	@Inject(at = @At("HEAD"), method = "schedule", cancellable = true)
	private void cancelSchedule(ScheduledTick<?> tick, CallbackInfo callback)
	{
		if (!structure_gel$shouldSchedule)
			callback.cancel();
	}
}
