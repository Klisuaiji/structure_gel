package com.legacy.structure_gel.core.mixin.client;

import com.legacy.structure_gel.core.asm_hooks.EndPortalBlockHooks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EndPortalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@OnlyIn(Dist.CLIENT)
@Mixin({EndPortalBlock.class})
public class EndPortalBlockMixin {
   @Inject(
      at = {@At("HEAD")},
      method = {"entityInside"}
   )
   private void onCollideHook(BlockState state, Level level, BlockPos pos, Entity entity, CallbackInfo callback) {
      EndPortalBlockHooks.onCollide(entity);
   }
}
