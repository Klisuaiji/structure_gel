package com.legacy.structure_gel.api.registry.registrar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.legacy.structure_gel.api.data.tags.FilterHolderSet;
import com.legacy.structure_gel.api.structure.ExtendedJigsawStructure;
import com.legacy.structure_gel.api.structure.GridStructurePlacement;
import com.legacy.structure_gel.api.structure.StructureAccessHelper;
import com.legacy.structure_gel.core.registry.SGRegistry;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings.SpawnerData;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureSpawnOverride;
import net.minecraft.world.level.levelgen.structure.StructureSpawnOverride.BoundingBoxType;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;

/**
 * Stores a structure, structure piece type, and structure feature(s) for
 * registry. Data is automatically registered at the appropriate time.
 * <p>
 * Call
 * {@linkplain RegistrarHandler#registerHandlers(String, net.neoforged.bus.api.IEventBus, RegistrarHandler...)
 * RegistrarHandler.registerHandlers} during your mod's initialization, after
 * classes containing structure registrars have been initialized.
 *
 * @param <S>
 *            The structure
 * @author Silver_David
 */
public class StructureRegistrar<S extends Structure>
{
	private final ResourceLocation registryName;
	private final Supplier<StructureType<S>> structureType;
	private final Map<String, Registrar.Pointer<S>> structures;
	private final Map<String, Registrar.Static<StructurePieceType>> pieceTypes;
	private final Registrar.Pointer<StructureSet> structureSet;

	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected StructureRegistrar(StructureRegistrar.Builder<S> builder)
	{
		this.registryName = builder.registryName;
		String modID = this.registryName.getNamespace();
		var typeHandler = RegistrarHandler.getOrCreate(Registries.STRUCTURE_TYPE, modID);
		var pieceHandler = RegistrarHandler.getOrCreate(Registries.STRUCTURE_PIECE, modID);
		var structureHandler = RegistrarHandler.getOrCreate(Registries.STRUCTURE, modID);
		var setHandler = RegistrarHandler.getOrCreate(Registries.STRUCTURE_SET, modID);

		this.structureType = !builder.existingType ? typeHandler.createStatic(ResourceKey.create(Registries.STRUCTURE_TYPE, this.registryName), builder.type) : builder.type;
		this.pieceTypes = builder.pieceTypes.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> pieceHandler.createStatic(ResourceKey.create(Registries.STRUCTURE_PIECE, createName(this.registryName, e.getKey())), e.getValue())));
		this.structures = builder.structureBuilders.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> structureHandler.createPointer(ResourceKey.create(Registries.STRUCTURE, createName(this.registryName, e.getKey())), e.getValue()::build)));
		this.structureSet = setHandler.createPointer(ResourceKey.create(Registries.STRUCTURE_SET, this.registryName), bootstrapContext ->
		{
			// Gather structure weights and handle any additional registry data from the builder
			List<StructureSet.StructureSelectionEntry> structureWeights = new ArrayList<>();
			HolderGetter<Structure> structureRegistry = bootstrapContext.lookup(Registries.STRUCTURE);
			builder.structureBuilders.forEach((name, structureBuilder) ->
			{

				Optional<Holder.Reference<S>> structure = this.structures.get(name).getHolder(structureRegistry);
				if (structure.isPresent())
				{
					Holder<S> holder = structure.get();
					structureWeights.add(StructureSet.entry((Holder) holder, structureBuilder.weight));
				}
			});

			// Structure Set
			StructurePlacement placement = builder.structurePlacement.apply(bootstrapContext);
			if (placement == null)
				throw new NullPointerException("Cannot register " + this.registryName + " without having a StructurePlacement set. If this used to be a GelStructure, GridStructurePlacement replaces that system.");

			return new StructureSet(structureWeights, placement);
		});

		// Go through builders on init
		builder.structureBuilders.forEach((name, structureBuilder) ->
		{
			// Get the registrar tied to `name`. It should already exist
			Registrar.Pointer<S> registrar = this.structures.get(name);
			if (registrar != null)
			{
				ResourceKey<S> key = registrar.getKey();
				
				if (structureBuilder.dimensions != null)
					StructureAccessHelper.addValidDimensions(key, structureBuilder.dimensions);
				if (structureBuilder.lakeProof)
					StructureAccessHelper.setLakeProof(key);
			}
		});
	}

	/**
	 * Creates a new {@link Builder} from the data passed
	 * 
	 * @param <S>
	 * @param registryName
	 * @param structureType
	 * @return
	 */
	public static <S extends Structure> StructureRegistrar.Builder<S> builder(ResourceLocation registryName, Supplier<StructureType<S>> structureType)
	{
		return new StructureRegistrar.Builder<>(registryName, structureType, false);
	}

	/**
	 * Creates a new {@link Builder} from the data passed for a structure type that
	 * already exists, such as the one used by mineshafts
	 * 
	 * @param <S>
	 * @param registryName
	 * @return
	 */
	public static <S extends Structure> StructureRegistrar.Builder<S> existingTypeBuilder(ResourceLocation registryName, Supplier<StructureType<S>> structureType)
	{
		return new StructureRegistrar.Builder<>(registryName, structureType, true);
	}

	/**
	 * Creates a new {@link Builder} from the data passed for a jigsaw structure
	 * 
	 * @param registryName
	 * @return
	 */
	public static StructureRegistrar.Builder<ExtendedJigsawStructure> jigsawBuilder(ResourceLocation registryName)
	{
		return existingTypeBuilder(registryName, SGRegistry.StructureTypes.EXTENDED_JIGSAW);
	}

	public StructureType<S> getType()
	{
		return this.structureType.get();
	}

	public Map<String, Registrar.Static<StructurePieceType>> getPieceTypes()
	{
		return this.pieceTypes;
	}

	/**
	 * @param name
	 * @return The structure piece type associated with the name passed, or null if
	 *         no object is present
	 */
	@Nullable
	public Registrar.Static<StructurePieceType> getPieceType(String name)
	{
		return this.getPieceTypes().get(name);
	}

	/**
	 * Use this if there's only one piece type for your structure
	 *
	 * @return The structure piece type held by this registrar
	 * @throws IllegalArgumentException
	 *             If more than one piece type exists for the structure
	 */
	@SuppressWarnings("unchecked")
	public Registrar.Static<StructurePieceType> getPieceType() throws IllegalArgumentException
	{
		if (this.getPieceTypes().size() == 1)
			return (Registrar.Static<StructurePieceType>) this.getPieceTypes().values().toArray()[0];
		else
			throw new IllegalArgumentException(String.format("The structure %s has more than one piece type associated with it. A String must be passed to define which one to get.", this.registryName));
	}

	/**
	 * @return The map of configured structure features held by this registrar
	 */
	public Map<String, Registrar.Pointer<S>> getStructures()
	{
		return this.structures;
	}

	/**
	 * @param name
	 *            The name that the configured structure feature was registered
	 *            under
	 * @return The structure feature associated with the name passed, or null if no
	 *         object is present
	 */
	@SuppressWarnings("unchecked")
	@Nullable
	public Registrar.Pointer<Structure> getStructure(String name)
	{
		return (Registrar.Pointer<Structure>) this.getStructureExact(name);
	}

	/**
	 * Only use this if you only have one structure feature registered for this
	 * structure
	 *
	 * @return The structure feature held by this registrar, or null if no object is
	 *         present
	 * @throws IllegalArgumentException
	 *             If more than one structure feature exists for the structure
	 */
	@SuppressWarnings("unchecked")
	@Nullable
	public Registrar.Pointer<Structure> getStructure() throws IllegalArgumentException
	{
		return (Registrar.Pointer<Structure>) this.getStructureExact();
	}

	/**
	 * @param name
	 *            The name that the configured structure feature was registered
	 *            under
	 * @return The structure feature associated with the name passed, casted to it's
	 *         appropriate types, or null if no object is present
	 */
	@Nullable
	public Registrar.Pointer<S> getStructureExact(String name)
	{
		return this.getStructures().get(name);
	}

	/**
	 * Only use this if you only have one structure feature registered for this
	 * structure
	 *
	 * @return The structure feature held by this registrar, casted to it's
	 *         appropriate types, or null if no object is present
	 * @throws IllegalArgumentException
	 *             If more than one structure feature exists for the structure
	 */
	@SuppressWarnings("unchecked")
	@Nullable
	public Registrar.Pointer<S> getStructureExact() throws IllegalArgumentException
	{
		var structures = this.getStructures();
		int size = structures.size();
		if (size == 1)
			return (Registrar.Pointer<S>) structures.values().toArray()[0];
		else if (size < 1)
			return null;
		else
			throw new IllegalArgumentException(String.format("The structure %s has more than one configured structure feature associated with it. A String must be passed to define which one to get.", this.registryName));
	}

	@Nullable
	public Registrar.Pointer<StructureSet> getStructureSet()
	{
		return this.structureSet;
	}

	public ResourceLocation getRegistryName()
	{
		return this.registryName;
	}

	private static ResourceLocation createName(ResourceLocation name, String suffix)
	{
		return suffix.isEmpty() ? name : ResourceLocation.fromNamespaceAndPath(name.getNamespace(), name.getPath() + "_" + suffix);
	}

	/**
	 * Builder to create a {@link StructureRegistrar}
	 * 
	 * @author Silver_David
	 *
	 * @param <S>
	 */
	public static class Builder<S extends Structure>
	{
		private final ResourceLocation registryName;
		@Nullable
		private final Supplier<StructureType<S>> type;
		private final boolean existingType;
		private final Map<String, StructureBuilder<S>> structureBuilders = new HashMap<>();
		private final Map<String, Supplier<StructurePieceType>> pieceTypes = new HashMap<>();
		private Function<BootstrapContext<?>, StructurePlacement> structurePlacement;

		private Builder(ResourceLocation registryName, @Nullable Supplier<StructureType<S>> type, boolean existingType)
		{
			this.registryName = registryName;
			this.type = type;
			this.existingType = existingType;
			this.structurePlacement = c -> GridStructurePlacement.builder(16, 0.8F).build(this.registryName);
		}

		/**
		 * @param structureFactory
		 * @return A builder for a Structure with the passed config
		 */
		public StructureBuilder<S> pushStructure(BiFunction<BootstrapContext<?>, Structure.StructureSettings, S> structureFactory)
		{
			return this.pushStructure("", structureFactory);
		}

		/**
		 * @param name
		 * @param structureFactory
		 * @return A builder for a Structure with the passed name suffix and config
		 */
		public StructureBuilder<S> pushStructure(String name, BiFunction<BootstrapContext<?>, Structure.StructureSettings, S> structureFactory)
		{
			return new StructureBuilder<>(this, structureFactory, name);
		}

		public StructureBuilder<S> pushStructure(BiFunction<BootstrapContext<?>, Structure.StructureSettings, S> structureFactory, Consumer<StructureBuilder<S>> builderConsumer)
		{
			return this.pushStructure("", structureFactory, builderConsumer);
		}

		public StructureBuilder<S> pushStructure(String name, BiFunction<BootstrapContext<?>, Structure.StructureSettings, S> structureFactory, Consumer<StructureBuilder<S>> builderConsumer)
		{
			var structureBuilder = this.pushStructure(name, structureFactory);
			builderConsumer.accept(structureBuilder);
			return structureBuilder;
		}

		/**
		 * @param structureFactory
		 * @return A builder for a Structure with the passed config
		 */
		public StructureBuilder<S> pushStructure(Function<Structure.StructureSettings, S> structureFactory)
		{
			return this.pushStructure("", (c, s) -> structureFactory.apply(s));
		}

		/**
		 * @param name
		 * @param structureFactory
		 * @return A builder for a Structure with the passed name suffix and config
		 */
		public StructureBuilder<S> pushStructure(String name, Function<Structure.StructureSettings, S> structureFactory)
		{
			return this.pushStructure(name, (c, s) -> structureFactory.apply(s));
		}

		public StructureBuilder<S> pushStructure(Function<Structure.StructureSettings, S> structureFactory, Consumer<StructureBuilder<S>> builderConsumer)
		{
			return this.pushStructure("", (c, s) -> structureFactory.apply(s), builderConsumer);
		}

		public StructureBuilder<S> pushStructure(String name, Function<Structure.StructureSettings, S> structureFactory, Consumer<StructureBuilder<S>> builderConsumer)
		{
			return this.pushStructure(name, (c, s) -> structureFactory.apply(s));
		}

		/**
		 * Creates and adds a {@link Structure} and stores it to the data map
		 * 
		 * @param structureBuilder
		 * @return This builder
		 * @throws IllegalArgumentException
		 *             If data already exists for the name passed
		 */
		private Builder<S> addStructureBuilder(StructureBuilder<S> structureBuilder) throws IllegalArgumentException
		{
			if (this.structureBuilders.put(structureBuilder.name, structureBuilder) != null)
				throw new IllegalArgumentException(this.errMsg("added a Structure under an existing name \"" + structureBuilder.name + "\""));
			return this;
		}

		/**
		 * Adds the {@link StructurePieceType} and stores it to the data map
		 * 
		 * @param name
		 * @param pieceType
		 * @return This builder
		 * @throws IllegalArgumentException
		 *             If data already exists for the name passed
		 */
		public Builder<S> addPiece(String name, Supplier<StructurePieceType> pieceType) throws IllegalArgumentException
		{
			if (this.pieceTypes.put(name, pieceType) != null)
				throw new IllegalArgumentException(this.errMsg("added a StructurePieceType under an existing name \"" + name + "\""));
			return this;
		}

		/**
		 * Adds the {@link StructurePieceType} and stores it to the data map. Only use
		 * this if only one {@link StructurePieceType} exists for this
		 * 
		 * @param pieceType
		 * @return This builder
		 * @throws IllegalArgumentException
		 *             If data already exists for the name passed
		 */
		public Builder<S> addPiece(Supplier<StructurePieceType> pieceType) throws IllegalArgumentException
		{
			return this.addPiece("", pieceType);
		}

		/**
		 * Sets what placement this should use to generate. A placement must be set.
		 * 
		 * @param structurePlacement
		 * @return This builder
		 */
		public Builder<S> placement(Supplier<StructurePlacement> structurePlacement)
		{
			return this.placement(c -> structurePlacement.get());
		}

		/**
		 * Sets what placement this should use to generate. A placement must be set.
		 * 
		 * @param structurePlacement
		 * @return This builder
		 */
		public Builder<S> placement(Function<BootstrapContext<?>, StructurePlacement> structurePlacement)
		{
			this.structurePlacement = structurePlacement;
			return this;
		}

		/**
		 * Constructs a {@link StructureRegistrar} from the data contained
		 * 
		 * @return A new {@link StructureRegistrar}
		 * @throws IllegalStateException
		 *             If no {@link Structure} or {@link StructurePieceType} was defined
		 */
		public StructureRegistrar<S> build() throws IllegalStateException
		{
			if (this.structureBuilders.isEmpty())
				throw new IllegalStateException(this.errMsg("has no Structure"));
			return new StructureRegistrar<>(this);
		}

		private String errMsg(String msg)
		{
			return "The StructureRegistrar.Builder \"" + this.registryName + "\" " + msg;
		}
	}

	public static class StructureBuilder<S extends Structure>
	{
		private final Builder<S> parentBuilder;
		private final String name;
		private final BiFunction<BootstrapContext<?>, Structure.StructureSettings, S> structureFactory;
		private Function<BootstrapContext<?>, HolderSet<Biome>> biomes = c -> c.lookup(Registries.BIOME).getOrThrow(BiomeTags.IS_OVERWORLD);
		private TerrainAdjustment terrainAdjustment = TerrainAdjustment.NONE;
		private GenerationStep.Decoration generationStep = GenerationStep.Decoration.SURFACE_STRUCTURES;
		private Map<MobCategory, Supplier<StructureSpawnOverride>> spawns = new HashMap<>();
		private int weight = 1;
		@Nullable
		private Collection<ResourceKey<Level>> dimensions = null;
		private boolean lakeProof = true;

		private StructureBuilder(Builder<S> parentBuilder, BiFunction<BootstrapContext<?>, Structure.StructureSettings, S> structureFactory, String name)
		{
			this.parentBuilder = parentBuilder;
			this.structureFactory = structureFactory;
			this.name = name;
		}

		/**
		 * Sets the biomes this should generate in to the values currently in the
		 * HolderSets passed.
		 * 
		 * @param biomes
		 * @return This builder
		 */
		public final StructureBuilder<S> biomes(Function<BootstrapContext<?>, HolderSet<Biome>> biomes)
		{
			this.biomes = biomes;
			return this;
		}

		public final StructureBuilder<S> biomes(Supplier<FilterHolderSet.Builder<Biome>> biomes)
		{
			return this.biomes(c -> biomes.get().build(c.lookup(Registries.BIOME)));
		}

		/**
		 * Sets the biomes this should generate in to the names passed
		 * 
		 * @param biomes
		 * @return This builder
		 */
		@SafeVarargs
		public final StructureBuilder<S> biomes(ResourceKey<Biome>... biomes)
		{
			return this.biomes(c -> HolderSet.direct(Arrays.stream(biomes).map(c.lookup(Registries.BIOME)::getOrThrow).toList()));
		}

		/**
		 * Sets the biomes this should generate in to the tag passed
		 * 
		 * @param tagKey
		 * @return This builder
		 */
		public final StructureBuilder<S> biomes(TagKey<Biome> tagKey)
		{
			return this.biomes(c -> c.lookup(Registries.BIOME).getOrThrow(tagKey));
		}

		/**
		 * Sets the mobs that can spawn in this structure for the category passed
		 * 
		 * @param category
		 * @param spawnPool
		 * @return This builder
		 */
		public final StructureBuilder<S> spawns(MobCategory category, Supplier<StructureSpawnOverride> spawnPool)
		{
			this.spawns.put(category, spawnPool);
			return this;
		}

		/**
		 * Sets the mobs that can spawn in this structure for the category passed
		 * 
		 * @param category
		 * @param boundingBoxType
		 * @param spawns
		 * @return This builder
		 */
		public final StructureBuilder<S> spawns(MobCategory category, BoundingBoxType boundingBoxType, Supplier<List<SpawnerData>> spawns)
		{
			return this.spawns(category, () -> new StructureSpawnOverride(boundingBoxType, WeightedRandomList.create(spawns.get())));
		}

		/**
		 * Sets this structure to spawn no mobs
		 * 
		 * @param boundingBoxType
		 * @param categories
		 *            What categories to have no mob spawns for. Leave this empty to
		 *            include all categories
		 * @return this builder
		 */
		public final StructureBuilder<S> noSpawns(BoundingBoxType boundingBoxType, MobCategory... categories)
		{
			for (MobCategory category : categories.length == 0 ? MobCategory.values() : categories)
				this.spawns(category, boundingBoxType, () -> Collections.emptyList());
			return this;
		}

		/**
		 * Makes this structure utilize the beardifier
		 * 
		 * @return This builder
		 */
		public final StructureBuilder<S> terrainAdjustment(TerrainAdjustment terrainAdjustment)
		{
			this.terrainAdjustment = terrainAdjustment;
			return this;
		}

		/**
		 * Determines which generation step this structure generates during
		 * 
		 * @return This builder
		 */
		public final StructureBuilder<S> generationStep(GenerationStep.Decoration generationStep)
		{
			this.generationStep = generationStep;
			return this;
		}

		/**
		 * Sets the weight of this structure generating for the {@link StructureSet}
		 * 
		 * @param weight
		 * @return This builder
		 */
		public final StructureBuilder<S> weight(int weight)
		{
			this.weight = weight;
			return this;
		}

		/**
		 * Adds the dimensions passed to this structure to determine where it's allowed
		 * to generate
		 * 
		 * @param dimensions
		 * @return This builder
		 */
		@SafeVarargs
		public final StructureBuilder<S> dimensions(ResourceKey<Level>... dimensions)
		{
			this.dimensions = List.of(dimensions);
			return this;
		}

		/**
		 * Sets whether or not lakes are prevented from generating in this structure.
		 * 
		 * @param lakeProof
		 * @return This builder
		 */
		public final StructureBuilder<S> lakeProof(boolean lakeProof)
		{
			this.lakeProof = lakeProof;
			return this;
		}

		/**
		 * Finalizes this configured structure feature builder
		 * 
		 * @return The original {@link StructureRegistrar.Builder}
		 * @throws IllegalStateException
		 *             If biomes were not set
		 */
		public final Builder<S> popStructure() throws IllegalStateException
		{
			this.parentBuilder.addStructureBuilder(this);
			return this.parentBuilder;
		}

		private S build(BootstrapContext<?> context)
		{
			return this.structureFactory.apply(context, new Structure.StructureSettings(this.biomes.apply(context), this.spawns.entrySet().stream().filter(e -> e.getValue().get() != null).collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get())), this.generationStep, this.terrainAdjustment));
		}
	}
}