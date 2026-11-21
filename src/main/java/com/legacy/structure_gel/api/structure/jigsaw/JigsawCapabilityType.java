package com.legacy.structure_gel.api.structure.jigsaw;

import com.mojang.serialization.MapCodec;

public interface JigsawCapabilityType<T extends JigsawCapability>
{
	MapCodec<T> codec();
}