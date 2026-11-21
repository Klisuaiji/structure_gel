package com.legacy.structure_gel.api.entity;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;

public class EntityAccessHelper {
   public static ResourceLocation getDeathLootTable(Mob entity) {
      return entity.lootTable;
   }

   public static void setDeathLootTable(Mob entity, ResourceLocation lootTable) {
      entity.lootTable = lootTable;
   }
}
