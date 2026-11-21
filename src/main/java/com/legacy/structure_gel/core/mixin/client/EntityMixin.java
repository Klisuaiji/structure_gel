package com.legacy.structure_gel.core.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.legacy.structure_gel.api.block.GelPortalBlock;
import com.legacy.structure_gel.core.client.ClientProxy;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Portal;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
@Mixin(Entity.class)
public class EntityMixin
{
	@Inject(at = @At(value = "NEW", target = "net/minecraft/world/entity/PortalProcessor"), method = "setAsInsidePortal(Lnet/minecraft/world/level/block/Portal;Lnet/minecraft/core/BlockPos;)V")
	private void setAsInsidePortal(Portal portal, BlockPos pos, CallbackInfo callback)
	{
		if (((Entity) (Object) this).equals(Minecraft.getInstance().player))
			ClientProxy.lastPortal = portal instanceof GelPortalBlock gelPortal ? gelPortal : null;
	}
}
