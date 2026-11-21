package com.legacy.structure_gel.api.util;

import java.util.UUID;

import com.mojang.serialization.Codec;

public class SGCodecs
{
	public static final Codec<UUID> UUID = Codec.STRING.xmap(java.util.UUID::fromString, java.util.UUID::toString);
}
