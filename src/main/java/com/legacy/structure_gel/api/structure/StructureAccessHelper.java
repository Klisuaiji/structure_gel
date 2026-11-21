package com.legacy.structure_gel.api.structure;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nullable;

import com.legacy.structure_gel.api.data.tags.SGTags;
import com.legacy.structure_gel.api.registry.RegistryHelper;

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

/**
 * Contains methods to set properties for structures and things involving them.
 *
 * @author Silver_David
 */
public class StructureAccessHelper
{
	private static final Set<ResourceKey<? extends Structure>> LAKE_PROOF = new HashSet<>();

	/**
	 * @return True if lake features shouldn't generate inside the structure passed.
	 */
	public static boolean isLakeProof(RegistryAccess registryAccess, Structure structure)
	{
		Registry<Structure> registry = registryAccess.registryOrThrow(Registries.STRUCTURE);
		Optional<ResourceKey<Structure>> key = registry.getResourceKey(structure);
		if (key.isPresent() && LAKE_PROOF.contains(key.get()))
			return true;
		return RegistryHelper.isInTag(registry, SGTags.StructureTags.LAKE_PROOF, structure);
	}

	/**
	 * Sets the structure to be lake proof
	 * 
	 * @param structure
	 */
	public static void setLakeProof(ResourceKey<? extends Structure> structure)
	{
		synchronized (LAKE_PROOF)
		{
			LAKE_PROOF.add(structure);
		}
	}

	/**
	 * @return True if the pos passed is within a configured instance of the
	 *         structure passed
	 */
	@SuppressWarnings("deprecation")
	public static boolean isInStructurePiece(ServerLevelAccessor levelAccessor, Structure structure, BlockPos pos)
	{
		if (levelAccessor.hasChunkAt(pos))
		{
			StructureManager structureManager = levelAccessor instanceof ServerLevel sl ? sl.structureManager() : levelAccessor instanceof WorldGenRegion wg ? levelAccessor.getLevel().structureManager().forWorldGenRegion(wg) : null;
			return structureManager != null && structureManager.getStructureWithPieceAt(pos, structure).isValid();
		}
		return false;
	}

	/**
	 * @return True if the pos passed is within a configured instance of the
	 *         structure passed
	 */
	public static boolean isInStructurePiece(ServerLevelAccessor levelAccessor, StructureType<?> type, BlockPos pos)
	{
		return levelAccessor.registryAccess().registryOrThrow(Registries.STRUCTURE).stream().filter(s -> type.equals(s.type())).anyMatch(s -> isInStructurePiece(levelAccessor, s, pos));
	}

	/**
	 * @return True if the pos passed is within a configured instance of the
	 *         structure passed
	 */
	@SuppressWarnings("deprecation")
	public static boolean isInStructure(ServerLevelAccessor levelAccessor, Structure structure, BlockPos pos)
	{
		if (levelAccessor.hasChunkAt(pos))
		{
			StructureManager structureManager = levelAccessor instanceof ServerLevel sl ? sl.structureManager() : levelAccessor instanceof WorldGenRegion wg ? levelAccessor.getLevel().structureManager().forWorldGenRegion(wg) : null;
			return structureManager != null && structureManager.getStructureAt(pos, structure).isValid();
		}
		return false;
	}

	/**
	 * @return True if the pos passed is within a configured instance of the
	 *         structure passed
	 */
	public static boolean isInStructure(ServerLevelAccessor levelAccessor, StructureType<?> type, BlockPos pos)
	{
		return levelAccessor.registryAccess().registryOrThrow(Registries.STRUCTURE).stream().filter(s -> type.equals(s.type())).anyMatch(s -> isInStructure(levelAccessor, s, pos));
	}

	/**
	 * Returns true if any of the structures in the structureTag passed has a valid
	 * feature chunk within the radius passed. Variation of vanilla's
	 * {@link ChunkGeneratorStructureState#hasStructureChunkInRange}
	 * 
	 * @return True if a structure chunk was found
	 */
	public static boolean hasStructureChunkInRange(ChunkGeneratorStructureState chunkGenStructureState, TagKey<Structure> structureTag, RegistryAccess registryAccess, ChunkPos chunkPos, int radius)
	{
		return hasStructureChunkInRange(chunkGenStructureState, registryAccess.lookupOrThrow(Registries.STRUCTURE).getOrThrow(structureTag), chunkPos, radius);
	}

	/**
	 * Returns true if any of the structures in "structures" has a valid feature
	 * chunk within the radius passed. Variation of vanilla's
	 * {@link ChunkGeneratorStructureState#hasStructureChunkInRange}
	 * 
	 * @return True if a structure chunk was found
	 */
	public static boolean hasStructureChunkInRange(ChunkGeneratorStructureState chunkGenStructureState, HolderSet<Structure> structures, ChunkPos chunkPos, int radius)
	{
		for (var structure : structures)
		{
			if (hasStructureChunkInRange(chunkGenStructureState, structure, chunkPos, radius))
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns true if the structure passed has a valid feature chunk within the
	 * radius passed. Variation of vanilla's way of handling this.
	 * 
	 * @return True if a structure chunk was found
	 */
	public static boolean hasStructureChunkInRange(ChunkGeneratorStructureState chunkGenStructureState, Holder<Structure> structure, ChunkPos chunkPos, int radius)
	{
		int chunkX = chunkPos.x;
		int chunkZ = chunkPos.z;
		for (var placement : chunkGenStructureState.getPlacementsForStructure(structure))
		{
			for (int x = chunkX - radius; x <= chunkX + radius; x++)
			{
				for (int z = chunkZ - radius; z <= chunkZ + radius; z++)
				{
					if (placement.isStructureChunk(chunkGenStructureState, x, z))
					{
						return true;
					}
				}
			}
		}
		return false;
	}

	private static final Map<ResourceKey<? extends Structure>, Set<ResourceKey<Level>>> STRUCTURE_DIM_FILTERS = new HashMap<>();

	/**
	 * Checks if the structure key passed is allowed to generate in the level
	 * passed.
	 * 
	 * @return True if the structure key passed has no filters, or the filter is
	 *         empty, or if the filter contains the level key passed.
	 */
	public static boolean isValidDimension(Optional<? extends ResourceKey<? extends Structure>> structureKey, ResourceKey<Level> levelKey)
	{
		if (structureKey.isPresent())
		{
			Set<ResourceKey<Level>> dimKeys = STRUCTURE_DIM_FILTERS.get(structureKey.get());
			return dimKeys == null || dimKeys.isEmpty() || dimKeys.contains(levelKey);
		}
		return false;
	}

	/**
	 * Allows the structure to generate in dimensions represented by the passed
	 * level keys. This appends the previous values.
	 */
	public static void addValidDimensions(ResourceKey<? extends Structure> structureKey, Collection<ResourceKey<Level>> levelKeys)
	{
		synchronized (STRUCTURE_DIM_FILTERS)
		{
			STRUCTURE_DIM_FILTERS.computeIfAbsent(structureKey, k -> new HashSet<>()).addAll(levelKeys);
		}
	}

	/**
	 * Allows the structure to generate in dimensions represented by the passed
	 * level keys. This overrides previous values.
	 * 
	 * @return The previous level keys associated with the structure, or null if
	 *         none existed.
	 */
	@Nullable
	public static Set<ResourceKey<Level>> setValidDimensions(ResourceKey<? extends Structure> structureKey, Collection<ResourceKey<Level>> levelKeys)
	{
		synchronized (STRUCTURE_DIM_FILTERS)
		{
			return STRUCTURE_DIM_FILTERS.put(structureKey, new HashSet<>(levelKeys));
		}
	}
}
