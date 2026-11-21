package com.legacy.structure_gel.core.data_handler.handlers;

import com.legacy.structure_gel.api.data_handler.LootTableAlias;
import com.legacy.structure_gel.api.data_handler.handlers.DataHandler;
import com.legacy.structure_gel.api.data_handler.handlers.ProbabilityDataHandler;
import com.legacy.structure_gel.api.data_handler.parsing.DataMap;
import com.legacy.structure_gel.api.data_handler.parsing.DataParser;
import com.legacy.structure_gel.api.entity.EntityAccessHelper;
import com.legacy.structure_gel.api.registry.StructureGelRegistries;
import com.legacy.structure_gel.core.SGAccessor;
import com.legacy.structure_gel.core.registry.SGRegistry;
import java.util.EnumMap;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;

public class EntityHandler extends ProbabilityDataHandler<EntityHandler> {
   private static final DataParser PARSER = DataParser.of(SGRegistry.DataHandlerTypes.ENTITY, parser -> {
      parser.add("entity", Registries.ENTITY_TYPE, EntityType.PIG);
      parser.add("helmet", (ItemStack)null);
      parser.add("chestplate", (ItemStack)null);
      parser.add("leggings", (ItemStack)null);
      parser.add("boots", (ItemStack)null);
      parser.add("main_hand", (ItemStack)null);
      parser.add("off_hand", (ItemStack)null);
      parser.add("loot", (ResourceLocation)null).setSuggestions(StructureGelRegistries.LOOT_TABLE_ALIAS::keySet, ResourceLocation::toString);
      parser.add("persistent", true);
      parser.add("spawn_chance", 1.0F, 0.0F, 1.0F);
      parser.add("tag", new CompoundTag());
   });
   private final ResourceKey<EntityType<?>> entityType;
   private final EnumMap<EquipmentSlot, ItemStack> equipment = new EnumMap<>(EquipmentSlot.class);
   @Nullable
   private final ResourceLocation loot;
   private final boolean persistent;
   private final CompoundTag tag;

   public EntityHandler(DataMap data) {
      super(data, "spawn_chance");
      this.entityType = data.get("entity", Registries.ENTITY_TYPE);
      this.equipment.put(EquipmentSlot.HEAD, data.get("helmet", ItemStack.class));
      this.equipment.put(EquipmentSlot.CHEST, data.get("chestplate", ItemStack.class));
      this.equipment.put(EquipmentSlot.LEGS, data.get("leggings", ItemStack.class));
      this.equipment.put(EquipmentSlot.FEET, data.get("boots", ItemStack.class));
      this.equipment.put(EquipmentSlot.MAINHAND, data.get("main_hand", ItemStack.class));
      this.equipment.put(EquipmentSlot.OFFHAND, data.get("off_hand", ItemStack.class));
      this.loot = data.get("loot", ResourceLocation.class);
      this.persistent = data.get("persistent", Boolean.class);
      this.tag = data.get("tag", CompoundTag.class);
   }

   public static DataParser parser() {
      return PARSER;
   }

   @Override
   protected void handle(DataHandler.Context context) {
      WorldGenLevel level = context.getLevel();
      BlockState dataHandler = context.getDataHandlerState();
      Vec3 pos = context.getPos();
      CompoundTag entityTag = this.tag.copy();
      entityTag.putString("id", this.entityType.location().toString());
      Entity entity = EntityType.loadEntityRecursive(
         entityTag,
         level.getLevel(),
         e -> {
            Direction facing = dataHandler.hasProperty(BlockStateProperties.FACING)
               ? (Direction)dataHandler.getValue(BlockStateProperties.FACING)
               : Direction.SOUTH;
            float yRot = facing.getAxis() != Axis.Y ? facing.toYRot() : e.getYRot();
            e.moveTo(pos.x(), pos.y(), pos.z(), yRot, e.getXRot());
            if (e instanceof HangingEntity) {
               SGAccessor.HANGING_ENTITY_SET_DIRECTION.invoke((HangingEntity)e, facing);
            }

            return e;
         }
      );
      if (entity != null) {
         if (entity instanceof Mob mobEntity) {
            EventHooks.onFinalizeSpawn(mobEntity, level, level.getCurrentDifficultyAt(BlockPos.containing(pos)), MobSpawnType.STRUCTURE, null, null);
            if (this.loot != null) {
               EntityAccessHelper.setDeathLootTable(mobEntity, LootTableAlias.getLootTable(this.loot));
            }

            if (this.persistent) {
               mobEntity.setPersistenceRequired();
            }
         }

         for (Entry<EquipmentSlot, ItemStack> entry : this.equipment.entrySet()) {
            ItemStack stack = entry.getValue();
            if (stack != null) {
               entity.setItemSlot(entry.getKey(), stack);
               if (entity instanceof ItemFrame itemFrame) {
                  itemFrame.setItem(stack, false);
               }
            }
         }

         level.addFreshEntityWithPassengers(entity);
      }
   }
}
