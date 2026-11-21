package com.legacy.structure_gel.api.structure;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.legacy.structure_gel.api.registry.StructureGelRegistries;
import com.legacy.structure_gel.api.structure.base.IPieceBuilderModifier;
import com.legacy.structure_gel.api.structure.jigsaw.JigsawCapability;
import com.legacy.structure_gel.core.registry.SGRegistry;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.heightproviders.ConstantHeight;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import net.minecraft.world.level.levelgen.heightproviders.UniformHeight;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.pools.DimensionPadding;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.pools.alias.PoolAliasBinding;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;

/**
 * A structure class to use with jigsaw structures.
 * <p>
 * Modifies the maxDepth range from vanilla's 0-7 to 0-32.<br>
 * Comes with additional capability support with {@link JigsawCapability}.
 * 
 * @author Silver_David
 */
public class ExtendedJigsawStructure extends JigsawStructure implements IPieceBuilderModifier
{
	public static final MapCodec<ExtendedJigsawStructure> CODEC = RecordCodecBuilder.<ExtendedJigsawStructure>mapCodec(instance ->
	{
		return instance.group(settingsCodec(instance), StructureTemplatePool.CODEC.fieldOf("start_pool").forGetter(jigsaw ->
		{
			return jigsaw.startPool;
		}), ResourceLocation.CODEC.optionalFieldOf("start_jigsaw_name").forGetter(jigsaw ->
		{
			return jigsaw.startJigsawName;
		}), Codec.intRange(0, 32).fieldOf("size").forGetter(jigsaw ->
		{
			return jigsaw.maxDepth;
		}), HeightProvider.CODEC.fieldOf("start_height").forGetter(jigsaw ->
		{
			return jigsaw.startHeight;
		}), Codec.BOOL.fieldOf("use_expansion_hack").forGetter(jigsaw ->
		{
			return jigsaw.useExpansionHack;
		}), Heightmap.Types.CODEC.optionalFieldOf("project_start_to_heightmap").forGetter(jigsaw ->
		{
			return jigsaw.projectStartToHeightmap;
		}), Codec.intRange(1, 128).fieldOf("max_distance_from_center").forGetter(jigsaw ->
		{
			return jigsaw.maxDistanceFromCenter;
		}), Codec.list(PoolAliasBinding.CODEC).optionalFieldOf("pool_aliases", List.of()).forGetter(jigsaw ->
		{
			return jigsaw.poolAliases;
		}), DimensionPadding.CODEC.optionalFieldOf("dimension_padding", DEFAULT_DIMENSION_PADDING).forGetter(jigsaw ->
		{
			return jigsaw.dimensionPadding;
		}), LiquidSettings.CODEC.optionalFieldOf("liquid_settings", DEFAULT_LIQUID_SETTINGS).forGetter(jigsaw ->
		{
			return jigsaw.liquidSettings;
		}), JigsawCapability.CODEC.optionalFieldOf("capability").forGetter(jigsaw ->
		{
			return jigsaw.capability;
		})).apply(instance, ExtendedJigsawStructure::new);
	}).flatXmap(ExtendedJigsawStructure::verifyRange, ExtendedJigsawStructure::verifyRange);

	protected DataResult<ExtendedJigsawStructure> verifyRange()
	{
		byte dist = switch (this.terrainAdaptation())
		{
		case NONE -> 0;
		case BURY, BEARD_THIN, BEARD_BOX -> 12;
		default -> throw new IncompatibleClassChangeError();
		};
		return this.maxDistanceFromCenter + dist > 128 ? DataResult.error(() -> "Structure size including terrain adaptation must not exceed 128. Terrain adaptation dist: " + dist + ", Size: " + this.maxDistanceFromCenter) : DataResult.success(this);
	}

	protected final Optional<JigsawCapability> capability;

	protected ExtendedJigsawStructure(Structure.StructureSettings settings, Holder<StructureTemplatePool> startPool, Optional<ResourceLocation> startJigsawName, int maxDepth, HeightProvider startHeight, boolean useExpansionHack, Optional<Heightmap.Types> projectStartToHeightmap, int maxDistanceFromCenter, List<PoolAliasBinding> poolAliases, DimensionPadding dimensionPadding, LiquidSettings liquidSettings, Optional<JigsawCapability> capability)
	{
		super(settings, startPool, startJigsawName, maxDepth, startHeight, useExpansionHack, projectStartToHeightmap, maxDistanceFromCenter, poolAliases, dimensionPadding, liquidSettings);
		this.capability = capability;
	}

	/**
	 * Creates a builder to create your jigsaw structure.
	 * 
	 * @param settings
	 * @param startPool
	 */
	public static Builder builder(Structure.StructureSettings settings, Holder<StructureTemplatePool> startPool)
	{
		return new Builder(settings, startPool);
	}

	@Override
	public Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext context)
	{
		// No longer required since this is handled via JigsawMixins now. Left here for documentation.
		return super.findGenerationPoint(context);
	}

	@Override
	public StructureType<?> type()
	{
		return SGRegistry.StructureTypes.EXTENDED_JIGSAW.get();
	}

	@Override
	public void modifyPieceBuilder(StructurePiecesBuilder pieceBuilder, GenerationContext context)
	{
		if (this.capability.isPresent())
			this.capability.get().modifyPieceBuilder(pieceBuilder, context);
	}

	/**
	 * @return An Optional for the capability
	 */
	public Optional<JigsawCapability> getCapability()
	{
		return this.capability;
	}

	/**
	 * @return An Optional for the capability, casted to the class passed, or null
	 *         if not applicable.
	 */
	public <T extends JigsawCapability> Optional<T> getCapability(Class<T> expectedClass)
	{
		return this.getCapability().map(cap -> expectedClass.isInstance(cap) ? expectedClass.cast(cap) : null);
	}

	/**
	 * Stores values from the jigsaw structure that get used to place the structure.
	 * 
	 * @author Silver_David
	 *
	 */
	public static record PlaceContext(Holder<StructureTemplatePool> startPool, Optional<ResourceLocation> startJigsawName, int maxDepth, boolean useExpansionHack, Optional<Heightmap.Types> projectStartToHeightmap, int maxDistanceFromCenter, DimensionPadding padding, LiquidSettings liquidSettings, Optional<JigsawCapability> capability)
	{

	}

	public static class Builder
	{
		final Structure.StructureSettings settings;
		final Holder<StructureTemplatePool> startPool;

		Optional<ResourceLocation> startJigsawName = Optional.empty();
		int maxDepth = 7;
		HeightProvider startHeight = ConstantHeight.ZERO;
		boolean useExpansionHack = false;
		Optional<Heightmap.Types> projectStartToHeightmap = Optional.empty();
		int maxDistanceFromCenter = 80;
		List<PoolAliasBinding> poolAliases = new ArrayList<>();
		DimensionPadding dimensionPadding = JigsawStructure.DEFAULT_DIMENSION_PADDING;
		LiquidSettings liquidSettings = JigsawStructure.DEFAULT_LIQUID_SETTINGS;
		Optional<JigsawCapability> capability = Optional.empty();

		private Builder(Structure.StructureSettings settings, Holder<StructureTemplatePool> startPool)
		{
			this.settings = settings;
			this.startPool = startPool;
		}

		/**
		 * Used to determine where the center of the start piece is for rotating said
		 * piece.
		 * <p>
		 * The ancient city uses this to align the start piece such that it's centered
		 * on the middle of the "portal" frame.
		 * 
		 * @param startJigsawName
		 *            The value in the "Name" field of the jigsaw block that centers
		 *            this piece.
		 */
		public Builder startJigsaw(ResourceLocation startJigsawName)
		{
			this.startJigsawName = Optional.ofNullable(startJigsawName);
			return this;
		}

		/**
		 * Max depth determines how many times the structure can branch out from the
		 * start.
		 * <p>
		 * For controlling the size of your structure, setting the
		 * {@linkplain #maxDistanceFromCenter(int) max distance from center} may be more
		 * accurate.
		 * 
		 * @param maxDepth
		 *            Range: 0-32, Default: 7
		 */
		public Builder maxDepth(int maxDepth)
		{
			this.maxDepth = maxDepth;
			return this;
		}

		/**
		 * Sets the y level of your structure. This field can work one of two ways
		 * depending on if you set a
		 * {@linkplain #heightmap(net.minecraft.world.level.levelgen.Heightmap.Types)
		 * heightmap}.
		 * <p>
		 * No heightmap: The value given by the height provider will be the y level of
		 * the structure.
		 * <p>
		 * With heightmap: The value given by the height provider will be applied as an
		 * offset to the heightmap.
		 * 
		 * @param startHeight
		 *            Defaults to a constant value of 0.
		 */
		public Builder startHeight(HeightProvider startHeight)
		{
			this.startHeight = startHeight;
			return this;
		}

		/**
		 * Uses the value passed as a constant value.
		 * 
		 * @see #startHeight(HeightProvider)
		 * @param startHeight
		 */
		public Builder startHeight(int startHeight)
		{
			return this.startHeight(ConstantHeight.of(VerticalAnchor.absolute(startHeight)));
		}

		/**
		 * Uses the values passed to randomly place the structure between them.
		 * 
		 * @see #startHeight(HeightProvider)
		 * @param minHeight
		 * @param maxHeight
		 */
		public Builder startHeight(int minHeight, int maxHeight)
		{
			return this.startHeight(UniformHeight.of(VerticalAnchor.absolute(minHeight), VerticalAnchor.absolute(maxHeight)));
		}

		/**
		 * When set, structures will be far more lenient when placing pieces.
		 * <p>
		 * Normally, a jigsaw is only able to place a piece if the placed piece is
		 * either not inside of an existing piece (the roads in an ancient city) or
		 * completely contained within an existing piece (villagers in houses). If the
		 * piece being placed is only partially contained within an existing jigsaw, it
		 * will not be allowed to place. Enabling the expansion hack will expand the
		 * bounding box to allow pieces to place in situations where they'd normally
		 * overlap. This is mainly used for the houses in villages to ensure that they
		 * actually place next to the paths.
		 */
		public Builder useExpansionHack()
		{
			this.useExpansionHack = true;
			return this;
		}

		/**
		 * Sets which heightmap your structure will place on when generating. This can
		 * be offset with the {@linkplain #startHeight start height}.
		 * 
		 * @param heightmap
		 *            Defaults to no heightmap.
		 */
		public Builder heightmap(Heightmap.Types heightmap)
		{
			this.projectStartToHeightmap = Optional.ofNullable(heightmap);
			return this;
		}

		/**
		 * Sets the heightmap to use the world surface.
		 */
		public Builder onSurface()
		{
			return this.heightmap(Heightmap.Types.WORLD_SURFACE_WG);
		}

		/**
		 * Sets the heightmap to use the ocean floor.
		 */
		public Builder onOceanFloor()
		{
			return this.heightmap(Heightmap.Types.OCEAN_FLOOR_WG);
		}

		/**
		 * Sets the maximum distance that a piece can be from the starting point of the
		 * structure. Useful for ensuring that a structure doesn't get too big.
		 * 
		 * @param maxDistanceFromCenter
		 *            Range: 1-128, Default: 80
		 */
		public Builder maxDistanceFromCenter(int maxDistanceFromCenter)
		{
			this.maxDistanceFromCenter = maxDistanceFromCenter;
			return this;
		}

		public Builder poolAlias(PoolAliasBinding alias)
		{
			this.poolAliases.add(alias);
			return this;
		}

		public Builder poolAlias(List<PoolAliasBinding> aliases)
		{
			this.poolAliases.addAll(aliases);
			return this;
		}

		public Builder dimensionPadding(DimensionPadding padding)
		{
			this.dimensionPadding = padding;
			return this;
		}
		
		public Builder liquidSettings(LiquidSettings liquidSettings)
		{
			this.liquidSettings = liquidSettings;
			return this;
		}
		
		/**
		 * Enables storing additional data for your structure to be used with custom
		 * pieces and custom generation logic.
		 * 
		 * @see JigsawCapability
		 * @param capability
		 *            Nothing by default
		 */
		public Builder capability(JigsawCapability capability)
		{
			this.capability = Optional.ofNullable(capability);
			return this;
		}

		public ExtendedJigsawStructure build()
		{
			if (this.capability.isPresent() && !StructureGelRegistries.JIGSAW_TYPE.containsValue(this.capability.get().getType()))
				throw new IllegalStateException("JigsawCapabilityType not registered for " + this.capability.get() + ". Be sure to register it to StructureGelRegistries.Keys.JIGSAW_TYPE");
			return new ExtendedJigsawStructure(settings, startPool, startJigsawName, maxDepth, startHeight, useExpansionHack, projectStartToHeightmap, maxDistanceFromCenter, poolAliases, dimensionPadding, liquidSettings, capability);
		}
	}
}
