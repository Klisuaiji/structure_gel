package com.legacy.structure_gel.core.mixin.common;

import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

public class ChunkAccessMixin {
   @Mixin({ChunkAccess.class})
   public interface Access {
      @Accessor("levelHeightAccessor")
      LevelHeightAccessor getHeightAccessor();
   }
}
