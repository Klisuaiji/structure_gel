package com.legacy.structure_gel.core.mixin.common;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.StructureMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({StructureBlockEntity.class})
public class StructureBlockEntityMixin {
   @Inject(
      at = {@At("RETURN")},
      method = {"detectSize"},
      cancellable = true
   )
   private void detectSizeHook(CallbackInfoReturnable<Boolean> callback) {
      if (!(Boolean)callback.getReturnValue()) {
         StructureBlockEntity me = (StructureBlockEntity)this;
         if (me.getMode() == StructureMode.SAVE) {
            BlockPos pos = me.getBlockPos();
            BlockPos originPos = pos.offset(1, 1, 1);
            Level level = me.getLevel();
            int maxSize = 48;
            Vec3i bounds = createBounds(level, originPos, new Vec3i(maxSize, maxSize, maxSize));
            bounds = createBounds(level, originPos, bounds);
            if (bounds.getX() > 0 && bounds.getY() > 0 && bounds.getZ() > 0) {
               me.setStructurePos(new BlockPos(1, 1, 1));
               me.setStructureSize(bounds);
               me.setChanged();
               BlockState blockstate = level.getBlockState(pos);
               level.sendBlockUpdated(pos, blockstate, blockstate, 3);
               callback.setReturnValue(true);
            }
         }
      }
   }

   private static Vec3i createBounds(Level level, BlockPos origin, Vec3i searchBounds) {
      int maxX = 0;

      int px;
      label92:
      for (px = 0; px < searchBounds.getX(); px++) {
         for (int y = 0; y < searchBounds.getY(); y++) {
            for (int z = 0; z < searchBounds.getZ(); z++) {
               BlockPos searchPos = origin.offset(px, y, z);
               if (!level.getBlockState(searchPos).isAir()) {
                  continue label92;
               }
            }
         }
         break;
      }

      maxX = px;
      int maxY = 0;

      int py;
      label74:
      for (py = 0; py < searchBounds.getY(); py++) {
         for (int x = 0; x < maxX; x++) {
            for (int zx = 0; zx < searchBounds.getZ(); zx++) {
               BlockPos searchPos = origin.offset(x, py, zx);
               if (!level.getBlockState(searchPos).isAir()) {
                  continue label74;
               }
            }
         }
         break;
      }

      maxY = py;
      int maxZ = 0;

      int pz;
      label56:
      for (pz = 0; pz < searchBounds.getZ(); pz++) {
         for (int x = 0; x < maxX; x++) {
            for (int y = 0; y < maxY; y++) {
               BlockPos searchPos = origin.offset(x, y, pz);
               if (!level.getBlockState(searchPos).isAir()) {
                  continue label56;
               }
            }
         }
         break;
      }

      return new Vec3i(maxX, maxY, pz);
   }
}
