package com.legacy.structure_gel.api.structure;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.legacy.structure_gel.api.registry.registrar.StructureRegistrar;
import com.legacy.structure_gel.core.registry.SGRegistry;
import com.legacy.structure_gel.core.util.Internal;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureCheckResult;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;

/**
 * A type of structure placement that uses a grid-like system for placing
 * things.
 * <p>
 * 1.18 to 1.19: If you used to use GelStructure, this placement was used by
 * default. Since GelStructure no longer exists, you'll need to set your
 * placement manually. Any {@link StructureRegistrar} that relied on this being
 * the default will throw an exception since it needs to be set.
 * <p>
 * 1.19.2 to 1.19.3: Structures no longer can have placement set with configs.
 * All values must be defined manually. Users can modify them with datapacks if
 * they wish.
 * 
 * @author Silver_David
 *
 */
public class GridStructurePlacement extends StructurePlacement
{
	// @formatter:off
	public static final MapCodec<GridStructurePlacement> CODEC = RecordCodecBuilder.mapCodec(
		instance -> 
		{
			var placement = placementCodec(instance);
			return instance.group(
					ResourceLocation.CODEC.fieldOf("structure").forGetter(o -> o.structureName), 
					placement.t1(), 
					TaggedExclusionZone.CODEC.optionalFieldOf("exclusion_zone").forGetter(o -> o.taggedExclusionZone), // Using custom for tags since that's more useful
					Codec.intRange(0, 4096).fieldOf("spacing").forGetter(GridStructurePlacement::spacing), 
					Codec.intRange(0, 4096).fieldOf("offset").forGetter(GridStructurePlacement::offset), 
					placement.t3(), 
					placement.t4(), 
					Codec.BOOL.fieldOf("allowed_near_spawn").forGetter(GridStructurePlacement::allowedNearSpawn)
					).apply(instance, GridStructurePlacement::new);
		});
	// @formatter:on

	private final ResourceLocation structureName;
	private final float probability;
	private final int seed;
	private final int spacing;
	private final int offset;
	private final boolean allowedNearSpawn;
	private final Optional<TaggedExclusionZone> taggedExclusionZone;

	private GridStructurePlacement(ResourceLocation structureName, Vec3i locateOffset, Optional<TaggedExclusionZone> exclusionZone, int spacing, int offset, float probability, int seed, boolean allowedNearSpawn)
	{
		super(locateOffset, FrequencyReductionMethod.DEFAULT, probability, seed, Optional.empty());
		this.structureName = structureName;
		this.probability = probability;
		this.seed = seed;
		this.spacing = spacing;
		this.offset = Mth.clamp(offset, 0, spacing);
		this.allowedNearSpawn = allowedNearSpawn;
		this.taggedExclusionZone = exclusionZone;
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static Builder builder(int spacing, float probability)
	{
		return builder().spacing(spacing).offset(spacing - 1).probability(probability);
	}

	public static Builder builder(int spacing, int offset, float probability)
	{
		return builder(spacing, probability).offset(offset);
	}

	@Override
	public boolean isStructureChunk(ChunkGeneratorStructureState chunkGenState, int chunkX, int chunkZ)
	{
		return this.isPlacementChunk(chunkGenState, chunkX, chunkZ) && (!this.taggedExclusionZone.isPresent() || !this.taggedExclusionZone.get().isPlacementForbidden(chunkGenState, chunkX, chunkZ));
	}

	@Override
	protected boolean isPlacementChunk(ChunkGeneratorStructureState chunkGenState, int chunkX, int chunkZ)
	{
		Optional<ChunkPos> opGenPos = this.getFeatureChunk(chunkGenState, chunkX, chunkZ);
		if (opGenPos.isPresent())
		{
			ChunkPos genPos = opGenPos.get();
			return genPos.x == chunkX && genPos.z == chunkZ;
		}
		return false;
	}

	/**
	 * Converts the data passed into a ChunkPos that the structure can generate in.
	 * 
	 * @param chunkGenState
	 * @param chunkX
	 * @param chunkZ
	 * @return A potential ChunkPos to generate in
	 */
	public Optional<ChunkPos> getFeatureChunk(ChunkGeneratorStructureState chunkGenState, int chunkX, int chunkZ)
	{
		if (this.frequency() > 0.0F)
		{
			long levelSeed = chunkGenState.getLevelSeed();
			int spacing = this.spacing();
			int gridX = ((int) Math.floor(chunkX / (float) spacing)) * spacing;
			int gridZ = ((int) Math.floor(chunkZ / (float) spacing)) * spacing;

			int offset = this.offset() + 1;
			WorldgenRandom rand = new WorldgenRandom(new LegacyRandomSource(levelSeed));
			rand.setLargeFeatureWithSalt(levelSeed, gridX, gridZ, this.salt());
			ChunkPos genPos = new ChunkPos(gridX + rand.nextInt(offset), gridZ + rand.nextInt(offset));

			if (this.allowedNearSpawn() || !this.isNearSpawn(genPos))
				if (this.frequency() >= 1.0F || this.probabilityTest(genPos, levelSeed))
					return Optional.of(genPos);
		}
		return Optional.empty();
	}

	/**
	 * @param genPos
	 * @return True if genPos is close to spawn (spawn defined as
	 *         {@link ChunkPos#ZERO})
	 */
	protected boolean isNearSpawn(ChunkPos genPos)
	{
		int range = 12;
		return genPos.x < range && genPos.x > -range && genPos.z < range && genPos.z > -range;
	}

	/**
	 * @param genPos
	 * @param levelSeed
	 * @return True if the placement probability passes a random
	 */
	protected boolean probabilityTest(ChunkPos genPos, long levelSeed)
	{
		WorldgenRandom rand = new WorldgenRandom(new LegacyRandomSource(levelSeed));
		rand.setLargeFeatureWithSalt(levelSeed, genPos.x, genPos.z, this.salt());
		return rand.nextFloat() < this.frequency();
	}

	@Override
	protected float frequency()
	{
		return this.probability;
	}

	@Override
	protected int salt()
	{
		return this.seed;
	}

	protected int spacing()
	{
		return this.spacing;
	}

	protected int offset()
	{
		return this.offset;
	}

	protected boolean allowedNearSpawn()
	{
		return this.allowedNearSpawn;
	}

	@Override
	public StructurePlacementType<?> type()
	{
		return SGRegistry.StructurePlacementTypes.GRID_PLACEMENT.get();
	}

	public static class Builder
	{
		private Vec3i locateOffset = Vec3i.ZERO;
		private float probability = 1.0F;
		private Optional<TaggedExclusionZone> exclusionZone = Optional.empty();
		private int spacing = 16;
		private int offset = 16;
		private boolean allowedNearSpawn = false;

		private Builder()
		{
		}

		public Builder locateOffset(Vec3i locateOffset)
		{
			this.locateOffset = locateOffset;
			return this;
		}

		public Builder exclusionZone(TaggedExclusionZone exclusionZone)
		{
			this.exclusionZone = Optional.of(exclusionZone);
			return this;
		}

		public Builder spacing(int spacing)
		{
			this.spacing = spacing;
			return this;
		}

		public Builder offset(int offset)
		{
			this.offset = offset;
			return this;
		}

		public Builder probability(float probability)
		{
			this.probability = probability;
			return this;
		}

		public Builder allowedNearSpawn(boolean allowedNearSpawn)
		{
			this.allowedNearSpawn = allowedNearSpawn;
			return this;
		}

		public GridStructurePlacement build(ResourceLocation structureName)
		{
			return new GridStructurePlacement(structureName, locateOffset, exclusionZone, spacing, offset, probability, Math.abs(structureName.toString().hashCode()), allowedNearSpawn);
		}

		public GridStructurePlacement build(StructureRegistrar<?> structureRegistrar)
		{
			return this.build(structureRegistrar.getRegistryName());
		}
	}

	public static record TaggedExclusionZone(HolderSet<Structure> structures, int radius)
	{
		private static final Codec<TaggedExclusionZone> CODEC = RecordCodecBuilder.create(instance ->
		{
			return instance.group(RegistryCodecs.homogeneousList(Registries.STRUCTURE).fieldOf("structures").forGetter(TaggedExclusionZone::structures), Codec.intRange(1, 16).fieldOf("radius").forGetter(TaggedExclusionZone::radius)).apply(instance, TaggedExclusionZone::new);
		});

		boolean isPlacementForbidden(ChunkGeneratorStructureState chunkGenState, int chunkX, int chunkZ)
		{
			return StructureAccessHelper.hasStructureChunkInRange(chunkGenState, this.structures, new ChunkPos(chunkX, chunkZ), this.radius);
		}
	}

	@Internal
	@Nullable
	private Pair<BlockPos, Holder<Structure>> findNearest(Set<Holder<Structure>> structures, ServerLevel level, ChunkGeneratorStructureState chunkGenState, StructureManager structureManager, int x, int z, int radius, boolean skipKnown)
	{
		int spacing = this.spacing();

		for (int dx = -radius; dx <= radius; ++dx)
		{
			boolean flag = dx == -radius || dx == radius;

			for (int dz = -radius; dz <= radius; ++dz)
			{
				boolean flag1 = dz == -radius || dz == radius;
				if (flag || flag1)
				{
					int cx = x + spacing * dx;
					int cz = z + spacing * dz;
					Optional<ChunkPos> opGenPos = this.getFeatureChunk(chunkGenState, cx, cz);
					if (opGenPos.isPresent())
					{
						ChunkPos genPos = opGenPos.get();

						for (Holder<Structure> holder : structures)
						{
							StructureCheckResult result = structureManager.checkStructurePresence(genPos, holder.value(), this, skipKnown);
							if (result != StructureCheckResult.START_NOT_PRESENT)
							{
								if (!skipKnown && result == StructureCheckResult.START_PRESENT)
								{
									return Pair.of(this.getLocatePos(genPos), holder);
								}

								ChunkAccess chunkAccess = level.getChunk(genPos.x, genPos.z, ChunkStatus.STRUCTURE_STARTS);
								StructureStart start = structureManager.getStartForStructure(SectionPos.bottomOf(chunkAccess), holder.value(), chunkAccess);
								if (start != null && start.isValid())
								{
									if (skipKnown)
									{
										if (start.canBeReferenced())
										{
											structureManager.addReference(start);
											return Pair.of(this.getLocatePos(start.getChunkPos()), holder);
										}
									}
									else
									{
										return Pair.of(this.getLocatePos(start.getChunkPos()), holder);
									}
								}
							}
						}
					}
				}
			}
		}

		return null;
	}

	// Needed to make locate command work
	@Internal
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Nullable
	public static Pair<BlockPos, Holder<Structure>> findNearesStructure(ServerLevel level, HolderSet<Structure> structureHolders, BlockPos origin, int radius, boolean skipKnown, ChunkGenerator chunkGen, @Nullable Pair<BlockPos, Holder<Structure>> oldNearest)
	{
		Set<Holder<Biome>> structureBiomes = structureHolders.stream().flatMap(csf -> csf.value().biomes().stream()).collect(Collectors.toSet());
		if (!structureBiomes.isEmpty())
		{
			Set<Holder<Biome>> biomeSourceBiomes = chunkGen.getBiomeSource().possibleBiomes();
			if (!Collections.disjoint(biomeSourceBiomes, structureBiomes))
			{
				@Nullable
				Pair<BlockPos, Holder<Structure>> newNearest = null;
				Map<StructurePlacement, Set<Holder<Structure>>> structuresByPlacement = new Object2ObjectArrayMap<>();

				ChunkGeneratorStructureState chunkGenState = level.getChunkSource().getGeneratorState();

				for (Holder<Structure> holder : structureHolders)
					if (!biomeSourceBiomes.stream().noneMatch(holder.value().biomes()::contains))
						for (StructurePlacement structureplacement : chunkGenState.getPlacementsForStructure(holder))
							structuresByPlacement.computeIfAbsent(structureplacement, placement -> new ObjectArraySet()).add(holder);

				for (Map.Entry<StructurePlacement, Set<Holder<Structure>>> entry : structuresByPlacement.entrySet())
				{
					StructurePlacement placement = entry.getKey();
					if (placement instanceof GridStructurePlacement)
					{
						GridStructurePlacement gelPlacement = (GridStructurePlacement) placement;
						int originCX = SectionPos.blockToSectionCoord(origin.getX());
						int originCZ = SectionPos.blockToSectionCoord(origin.getZ());
						for (int r = 0; r <= radius; ++r)
						{
							var found = gelPlacement.findNearest(entry.getValue(), level, chunkGenState, level.structureManager(), originCX, originCZ, r, skipKnown);
							if (found != null)
							{
								newNearest = found;
								break;
							}
						}
					}
				}

				if (oldNearest == null)
				{
					return newNearest;
				}
				else if (newNearest != null)
				{
					double oldDist = origin.distSqr(oldNearest.getFirst());
					double newDist = origin.distSqr(newNearest.getFirst());
					if (newDist < oldDist)
						return newNearest;
				}
			}
		}

		return oldNearest;
	}
}
