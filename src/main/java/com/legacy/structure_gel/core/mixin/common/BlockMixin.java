package com.legacy.structure_gel.core.mixin.common;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.legacy.structure_gel.core.block.DataHandlerBlock;
import com.legacy.structure_gel.core.registry.SGRegistry;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(Block.class)
public class BlockMixin
{
	@Inject(at = @At("HEAD"), method = "isExceptionForConnection", cancellable = true)
	private static void isExceptionForConnectionHook(BlockState state, CallbackInfoReturnable<Boolean> callback)
	{
		if (state.is(SGRegistry.Blocks.DATA_HANDLER.get()) && !state.getValue(DataHandlerBlock.CONNECT_TO_BLOCKS))
			callback.setReturnValue(true);
	}
}
