package com.legacy.structure_gel.core.block_entity;

import com.legacy.structure_gel.api.dynamic_spawner.DynamicSpawnerType;
import com.legacy.structure_gel.api.registry.StructureGelRegistries;
import com.legacy.structure_gel.core.dynamic_spawner.DynamicSpawner;
import com.legacy.structure_gel.core.registry.SGRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class DynamicSpawnerBlockEntity extends SGBlockEntity {
   public static final String SPAWNER_ID_KEY = "SpawnerID";
   public static final String SPAWNER_KEY = "Spawner";
   public static final String EXP_RANGE_KEY = "exp_range";
   private ResourceLocation spawnerID = SGRegistry.DynamicSpawnerTypes.DEFAULT.getName();
   private DynamicSpawner spawner = SGRegistry.DynamicSpawnerTypes.DEFAULT.get().createSpawner(this.level, this.worldPosition);
   private DynamicSpawnerBlockEntity.ExpRange expRange = DynamicSpawnerBlockEntity.ExpRange.VANILLA;

   public DynamicSpawnerBlockEntity(BlockPos pos, BlockState state) {
      super(SGRegistry.BlockEntities.DYNAMIC_SPAWNER.get(), pos, state);
   }

   public void setSpawner(DynamicSpawnerType type) {
      ResourceLocation newName = StructureGelRegistries.DYNAMIC_SPAWNER_TYPE.getKey(type);
      if (newName != null && !newName.equals(this.spawnerID)) {
         this.spawnerID = newName;
         this.spawner = type.createSpawner(this.level, this.worldPosition);
      }
   }

   public ResourceLocation getSpawnerID() {
      return this.spawnerID;
   }

   public void setExpRange(DynamicSpawnerBlockEntity.ExpRange maxInclusive) {
      this.expRange = maxInclusive;
      this.setChanged();
   }

   public DynamicSpawnerBlockEntity.ExpRange getExpRange() {
      return this.expRange;
   }

   public void load(CompoundTag tag) {
      super.load(tag);
      if (tag.contains("SpawnerID", 8)) {
         ResourceLocation typeName = new ResourceLocation(tag.getString("SpawnerID"));
         if (tag.contains("Spawner", 10)) {
            this.spawnerID = typeName;
            this.spawner.load(this.level, this.worldPosition, tag.getCompound("Spawner"));
         } else {
            this.setSpawner((DynamicSpawnerType)StructureGelRegistries.DYNAMIC_SPAWNER_TYPE.get(typeName));
         }
      }

      if (tag.contains("exp_range", 10)) {
         this.expRange = DynamicSpawnerBlockEntity.ExpRange.load(tag.getCompound("exp_range"));
      }
   }

   protected void saveAdditional(CompoundTag tag) {
      super.saveAdditional(tag);
      if (this.spawnerID != null) {
         tag.putString("SpawnerID", this.spawnerID.toString());
      }

      tag.put("exp_range", this.expRange.save());
   }

   @Override
   public ClientboundBlockEntityDataPacket getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this, b -> {
         CompoundTag tag = this.serializeNBT();
         this.saveAdditional(tag);
         CompoundTag spawnerTag = this.spawner.save(new CompoundTag());
         tag.put("Spawner", spawnerTag);
         return tag;
      });
   }

   public static void clientTick(Level level, BlockPos pos, BlockState state, DynamicSpawnerBlockEntity blockEntity) {
      blockEntity.spawner.clientTick(level, pos);
   }

   public static void serverTick(Level level, BlockPos pos, BlockState state, DynamicSpawnerBlockEntity blockEntity) {
      blockEntity.spawner.serverTick((ServerLevel)level, pos);
   }

   public boolean triggerEvent(int paramA, int paramB) {
      return this.spawner.onEventTriggered(this.level, paramA) ? true : super.triggerEvent(paramA, paramB);
   }

   public boolean onlyOpCanSetNbt() {
      return true;
   }

   public DynamicSpawner getSpawner() {
      return this.spawner;
   }

   public static class ExpRange {
      public static final String MIN_KEY = "min";
      public static final String MAX_KEY = "max";
      public static final DynamicSpawnerBlockEntity.ExpRange VANILLA = new DynamicSpawnerBlockEntity.ExpRange(15, 43);
      public final int min;
      public final int max;

      public ExpRange(int minInclusive, int maxInclusive) {
         this.min = Math.max(0, minInclusive);
         this.max = Math.max(this.min, maxInclusive);
      }

      public static DynamicSpawnerBlockEntity.ExpRange load(CompoundTag tag) {
         return tag.contains("min", 3) && tag.contains("max", 3) ? new DynamicSpawnerBlockEntity.ExpRange(tag.getInt("min"), tag.getInt("max")) : VANILLA;
      }

      public CompoundTag save() {
         CompoundTag tag = new CompoundTag();
         tag.putInt("min", this.min);
         tag.putInt("max", this.max);
         return tag;
      }
   }
}
