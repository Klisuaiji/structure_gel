package com.legacy.structure_gel.api.util;

import com.mojang.serialization.Codec;
import java.util.UUID;

public class SGCodecs {
   public static final Codec<UUID> UUID = Codec.STRING.xmap(UUID::fromString, UUID::toString);
}
