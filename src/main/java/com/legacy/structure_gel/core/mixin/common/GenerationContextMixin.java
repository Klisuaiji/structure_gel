package com.legacy.structure_gel.core.mixin.common;

import javax.annotation.Nullable;

import org.spongepowered.asm.mixin.Mixin;

import com.legacy.structure_gel.core.capability.misc.GenerationContextData;

import net.minecraft.world.level.levelgen.structure.Structure;

// Even though GenerationContext is a record, we seem to be able to add an interface and a non-final field to it since the underlying behavior is that of a normal Object.
// Casting must be done as (GenerationContextData) (Object) context since Java understands that Records can't normally be extended, but adding (Object) gets around it.
@Mixin(Structure.GenerationContext.class)
public class GenerationContextMixin implements GenerationContextData
{
	@Nullable
	private Structure structure_gel$structure;

	@Nullable
	@Override
	public Structure structure_gel$getStructure()
	{
		return structure_gel$structure;
	}

	@Override
	public void structure_gel$setStructure(@Nullable Structure structure)
	{
		structure_gel$structure = structure;
	}
}
