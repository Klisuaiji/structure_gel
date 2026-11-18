package com.legacy.structure_gel.core.mixin.common;

import com.legacy.structure_gel.core.asm_hooks.EntityHooks;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Entity.class})
public class EntityMixin {
   @Inject(
      at = {@At("HEAD")},
      method = {"handleNetherPortal"},
      cancellable = true
   )
   private void updatePortal(CallbackInfo callback) {
      if (EntityHooks.handleGelPortal((Entity)this)) {
         callback.cancel();
      }
   }
}
