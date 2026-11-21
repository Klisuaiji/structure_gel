package com.legacy.structure_gel.core.capability.misc;

import java.util.Optional;

import javax.annotation.Nullable;

import com.legacy.structure_gel.api.structure.ExtendedJigsawStructure;
import com.legacy.structure_gel.api.structure.jigsaw.JigsawCapability;

import net.minecraft.world.level.levelgen.structure.Structure;

public interface GenerationContextData
{
	@Nullable
	Structure structure_gel$getStructure();

	void structure_gel$setStructure(@Nullable Structure structure);

	default Optional<JigsawCapability> structure_gel$getJigsawCapability()
	{
		if (structure_gel$getStructure() instanceof ExtendedJigsawStructure extended)
			return extended.getCapability();
		return Optional.empty();
	}
}
