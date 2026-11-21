package com.legacy.structure_gel.api.structure;

import com.legacy.structure_gel.api.data.tags.SGTags;
import com.legacy.structure_gel.api.registry.RegistryHelper;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;

public class StructureAccessHelper {
   private static final Set<ResourceKey<? extends Structure>> LAKE_PROOF = new HashSet<>();
   private static final Map<ResourceKey<? extends Structure>, Set<ResourceKey<Level>>> STRUCTURE_DIM_FILTERS = new HashMap<>();

   public static boolean isLakeProof(RegistryAccess registryAccess, Structure structure) {
      Registry<Structure> registry = registryAccess.registryOrThrow(Registries.STRUCTURE);
      Optional<ResourceKey<Structure>> key = registry.getResourceKey(structure);
      return key.isPresent() && LAKE_PROOF.contains(key.get()) ? true : RegistryHelper.isInTag(registry, SGTags.StructureTags.LAKE_PROOF, structure);
   }

   public static void setLakeProof(ResourceKey<? extends Structure> structure) {
      synchronized (LAKE_PROOF) {
         LAKE_PROOF.add(structure);
      }
   }

   public static boolean isInStructurePiece(ServerLevelAccessor levelAccessor, Structure structure, BlockPos pos) {
      if (levelAccessor.hasChunkAt(pos)) {
         StructureManager structureManager = levelAccessor instanceof ServerLevel sl
            ? sl.structureManager()
            : (levelAccessor instanceof WorldGenRegion wg ? levelAccessor.getLevel().structureManager().forWorldGenRegion(wg) : null);
         return structureManager != null && structureManager.getStructureWithPieceAt(pos, structure).isValid();
      } else {
         return false;
      }
   }

   public static boolean isInStructurePiece(ServerLevelAccessor levelAccessor, StructureType<?> type, BlockPos pos) {
      return levelAccessor.registryAccess()
         .registryOrThrow(Registries.STRUCTURE)
         .stream()
         .filter(s -> type.equals(s.type()))
         .anyMatch(s -> isInStructurePiece(levelAccessor, s, pos));
   }

   public static boolean isInStructure(ServerLevelAccessor levelAccessor, Structure structure, BlockPos pos) {
      if (levelAccessor.hasChunkAt(pos)) {
         StructureManager structureManager = levelAccessor instanceof ServerLevel sl
            ? sl.structureManager()
            : (levelAccessor instanceof WorldGenRegion wg ? levelAccessor.getLevel().structureManager().forWorldGenRegion(wg) : null);
         return structureManager != null && structureManager.getStructureAt(pos, structure).isValid();
      } else {
         return false;
      }
   }

   public static boolean isInStructure(ServerLevelAccessor levelAccessor, StructureType<?> type, BlockPos pos) {
      return levelAccessor.registryAccess()
         .registryOrThrow(Registries.STRUCTURE)
         .stream()
         .filter(s -> type.equals(s.type()))
         .anyMatch(s -> isInStructure(levelAccessor, s, pos));
   }

   public static boolean hasStructureChunkInRange(
      ChunkGeneratorStructureState chunkGenStructureState, TagKey<Structure> structureTag, RegistryAccess registryAccess, ChunkPos chunkPos, int radius
   ) {
      return hasStructureChunkInRange(
         chunkGenStructureState, registryAccess.registryOrThrow(Registries.STRUCTURE).getOrCreateTag(structureTag), chunkPos, radius
      );
   }

   public static boolean hasStructureChunkInRange(
      ChunkGeneratorStructureState chunkGenStructureState, HolderSet<Structure> structures, ChunkPos chunkPos, int radius
   ) {
      for (Holder<Structure> structure : structures) {
         if (hasStructureChunkInRange(chunkGenStructureState, structure, chunkPos, radius)) {
            return true;
         }
      }

      return false;
   }

   public static boolean hasStructureChunkInRange(
      ChunkGeneratorStructureState chunkGenStructureState, Holder<Structure> structure, ChunkPos chunkPos, int radius
   ) {
      int chunkX = chunkPos.x;
      int chunkZ = chunkPos.z;

      for (StructurePlacement placement : chunkGenStructureState.getPlacementsForStructure(structure)) {
         for (int x = chunkX - radius; x <= chunkX + radius; x++) {
            for (int z = chunkZ - radius; z <= chunkZ + radius; z++) {
               if (placement.isStructureChunk(chunkGenStructureState, x, z)) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   public static boolean isValidDimension(Optional<? extends ResourceKey<? extends Structure>> structureKey, ResourceKey<Level> levelKey) {
      if (!structureKey.isPresent()) {
         return false;
      }

      Set<ResourceKey<Level>> dimKeys = STRUCTURE_DIM_FILTERS.get(structureKey.get());
      return dimKeys == null || dimKeys.isEmpty() || dimKeys.contains(levelKey);
   }

   public static void addValidDimensions(ResourceKey<? extends Structure> structureKey, Collection<ResourceKey<Level>> levelKeys) {
      synchronized (STRUCTURE_DIM_FILTERS) {
         STRUCTURE_DIM_FILTERS.computeIfAbsent(structureKey, k -> new HashSet<>()).addAll(levelKeys);
      }
   }

   @Nullable
   public static Set<ResourceKey<Level>> setValidDimensions(ResourceKey<? extends Structure> structureKey, Collection<ResourceKey<Level>> levelKeys) {
      synchronized (STRUCTURE_DIM_FILTERS) {
         return STRUCTURE_DIM_FILTERS.put(structureKey, new HashSet<>(levelKeys));
      }
   }
}
