package com.legacy.structure_gel.api.block_entity;

import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;

public interface IRotatable {
   void rotate(Rotation var1);

   void mirror(Mirror var1);

   default void transform(Mirror mirror, Rotation rotation) {
      this.mirror(mirror);
      this.rotate(rotation);
   }
}
