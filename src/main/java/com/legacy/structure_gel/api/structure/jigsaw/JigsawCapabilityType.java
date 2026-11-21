package com.legacy.structure_gel.api.structure.jigsaw;

import com.mojang.serialization.Codec;

public interface JigsawCapabilityType<T extends JigsawCapability> {
   Codec<T> codec();
}
