package com.legacy.structure_gel.api.item;

import com.mojang.datafixers.util.Pair;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Holder.Reference;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;

public interface StructureFinderItem {
   String STRUCTURE_KEY = "structure";
   String POS_KEY = "pos";
   String DIMENSION_KEY = "dimension";

   default int searchRange() {
      return 100;
   }

   @Nullable
   default BlockPos findAndStoreStructure(ItemStack stack, ServerLevel level, BlockPos searchPos) {
      ResourceKey<Structure> structure = this.getStructure(stack);
      if (structure != null) {
         Optional<Reference<Structure>> holder = level.registryAccess().registryOrThrow(Registries.STRUCTURE).getHolder(structure);
         if (holder.isPresent()) {
            Pair<BlockPos, Holder<Structure>> nearest = level.getChunkSource()
               .getGenerator()
               .findNearestMapStructure(level, HolderSet.direct(new Holder[]{(Holder)holder.get()}), searchPos, this.searchRange(), false);
            if (nearest != null) {
               BlockPos pos = (BlockPos)nearest.getFirst();
               this.setStructurePos(stack, pos);
               this.setStructureDimension(stack, level.dimension());
               return pos;
            }
         }
      }

      return null;
   }

   @Nullable
   default ResourceKey<Structure> getStructure(ItemStack stack) {
      CompoundTag tag = stack.getTag();
      if (tag != null && tag.contains("structure", 8)) {
         String structureS = tag.getString("structure");
         if (ResourceLocation.isValidResourceLocation(structureS)) {
            return ResourceKey.create(Registries.STRUCTURE, new ResourceLocation(structureS));
         }
      }

      return null;
   }

   default void setStructure(ItemStack stack, @Nullable ResourceKey<Structure> structure) {
      CompoundTag tag = stack.getOrCreateTag();
      if (structure == null) {
         tag.remove("structure");
      } else {
         tag.putString("structure", structure.location().toString());
      }
   }

   @Nullable
   default BlockPos getStructurePos(ItemStack stack) {
      CompoundTag tag = stack.getTag();
      return tag != null && tag.contains("pos", 4) ? BlockPos.of(tag.getLong("pos")) : null;
   }

   default void setStructurePos(ItemStack stack, @Nullable BlockPos structurePos) {
      CompoundTag tag = stack.getOrCreateTag();
      if (structurePos == null) {
         tag.remove("pos");
      } else {
         tag.putLong("pos", structurePos.asLong());
      }
   }

   @Nullable
   default ResourceKey<Level> getStructureDimension(ItemStack stack) {
      CompoundTag tag = stack.getTag();
      if (tag != null && tag.contains("dimension", 8)) {
         String dimensionS = tag.getString("dimension");
         if (ResourceLocation.isValidResourceLocation(dimensionS)) {
            return ResourceKey.create(Registries.DIMENSION, new ResourceLocation(dimensionS));
         }
      }

      return null;
   }

   default void setStructureDimension(ItemStack stack, @Nullable ResourceKey<Level> dimension) {
      CompoundTag tag = stack.getOrCreateTag();
      if (dimension == null) {
         tag.remove("dimension");
      } else {
         tag.putString("dimension", dimension.location().toString());
      }
   }
}
