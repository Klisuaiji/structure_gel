package com.legacy.structure_gel.core.mixin.common;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.ChunkAccess;

public class ChunkAccessMixin
{
	@Mixin(ChunkAccess.class)
	public static interface Access
	{
		@Accessor(value = "levelHeightAccessor")
		public LevelHeightAccessor getHeightAccessor();
	}
}
