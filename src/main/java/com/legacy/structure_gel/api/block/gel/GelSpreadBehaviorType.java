package com.legacy.structure_gel.api.block.gel;

import com.mojang.serialization.MapCodec;

public interface GelSpreadBehaviorType
{
	MapCodec<? extends GelSpreadBehavior> codec();
}
