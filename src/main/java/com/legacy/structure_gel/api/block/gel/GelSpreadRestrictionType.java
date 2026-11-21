package com.legacy.structure_gel.api.block.gel;

import com.mojang.serialization.MapCodec;

public interface GelSpreadRestrictionType
{
	MapCodec<? extends GelSpreadRestriction> codec();
}
