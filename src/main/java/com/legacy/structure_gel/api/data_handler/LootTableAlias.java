package com.legacy.structure_gel.api.data_handler;

import com.legacy.structure_gel.api.registry.StructureGelRegistries;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;

public class LootTableAlias {
   @Nullable
   public static ResourceLocation getLootTable(@Nullable ResourceLocation alias) {
      return alias == null ? null : StructureGelRegistries.LOOT_TABLE_ALIAS.getOptional(alias).orElse(alias);
   }
}
