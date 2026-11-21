package com.legacy.structure_gel.api.structure.jigsaw;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.legacy.structure_gel.core.structure.jigsaw.GelSinglePoolElement;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;

/**
 * Assists in the creation of jigsaw pools, allowing multiple pools to be
 * created using the same settings.
 *
 * @author Silver_David
 */
public class JigsawPoolBuilder
{
	private final JigsawRegistryHelper jigsawRegistryHelper;
	private Map<ResourceLocation, Integer> names = ImmutableMap.of();
	private int weight = 1;
	private ResourceKey<StructureProcessorList> processors = ResourceKey.create(Registries.PROCESSOR_LIST, ResourceLocation.parse("empty")); // Found in ProcessorLists. Private for no reason.
	private Optional<LiquidSettings> overrideLiquidSettings = Optional.empty();

	/**
	 * @param jigsawRegistryHelper
	 * @see JigsawRegistryHelper#poolBuilder()
	 */
	public JigsawPoolBuilder(JigsawRegistryHelper jigsawRegistryHelper)
	{
		this.jigsawRegistryHelper = jigsawRegistryHelper;
	}

	/**
	 * Set a list of names that the builder uses as structure resource locations
	 * with the weights in the map
	 *
	 * @param nameMap
	 *            Names are converted to resource locations using
	 *            {@link JigsawRegistryHelper#locatePiece(String)} from the
	 *            contained jigsaw registry helper. Piece weights are set in the map
	 * @return This instance
	 */
	public JigsawPoolBuilder names(Map<String, Integer> nameMap)
	{
		return this.namesR(nameMap.entrySet().stream().collect(Collectors.toMap(k -> this.jigsawRegistryHelper.locatePiece(k.getKey()), v -> v.getValue())));
	}

	/**
	 * Set a list of names that the builder uses as structure resource locations
	 * with the weights in the map
	 *
	 * @param nameMap
	 *            Names are left as is with no conversion
	 * @return This instance
	 */
	public JigsawPoolBuilder namesR(Map<ResourceLocation, Integer> nameMap)
	{
		this.names = nameMap;
		return this;
	}

	/**
	 * Set a list of names that the builder uses as structure resource locations
	 * with equal weights
	 *
	 * @param names
	 *            Names are converted to resource locations using
	 *            {@link JigsawRegistryHelper#locatePiece(String)} from the
	 *            contained jigsaw registry helper. Piece weights are defaulted to
	 *            this instance's weight
	 * @return This instance
	 */
	public JigsawPoolBuilder names(Collection<String> names)
	{
		return this.namesR(names.stream().collect(Collectors.toMap(k -> this.jigsawRegistryHelper.locatePiece(k), v -> this.weight)));
	}

	/**
	 * Set a list of names that the builder uses as structure resource locations
	 * with equal weights
	 *
	 * @param names
	 *            Names are left as is with no conversion. Piece weights are
	 *            defaulted to this instance's weight
	 * @return This instance
	 */
	public JigsawPoolBuilder namesR(Collection<ResourceLocation> names)
	{
		return this.namesR(names.stream().collect(Collectors.toMap(k -> k, v -> this.weight)));
	}

	/**
	 * Set a list of names that the builder uses as structure resource locations
	 * with equal weights
	 *
	 * @param names
	 *            Names are converted to resource locations using
	 *            {@link JigsawRegistryHelper#locatePiece(String)} from the
	 *            contained jigsaw registry helper. Piece weights are defaulted to
	 *            this instance's weight
	 * @return This instance
	 */
	public JigsawPoolBuilder names(String... names)
	{
		return this.names(Arrays.asList(names));
	}

	/**
	 * Set a list of names that the builder uses as structure resource locations
	 * with equal weights
	 *
	 * @param names
	 *            Names are left as is with no conversion. Piece weights are
	 *            defaulted to this instance's weight
	 * @return This instance
	 */
	public JigsawPoolBuilder namesR(ResourceLocation... names)
	{
		return this.namesR(Arrays.asList(names));
	}

	/**
	 * Sets the weight of all pieces the value passed in. For efficiency, set the
	 * weight before the names
	 *
	 * @param weight
	 * @return This instance
	 */
	public JigsawPoolBuilder weight(int weight)
	{
		this.weight = weight;
		return this.namesR(this.names.keySet());
	}

	/**
	 * Structure processors that all pieces in this builder will use
	 *
	 * @param processors
	 *            Empty by default
	 * @return This instance
	 */
	public JigsawPoolBuilder processors(ResourceKey<StructureProcessorList> processors)
	{
		this.processors = processors;
		return this;
	}

	/**
	 * Determines if waterloggable blocks should become waterlogged when placed in
	 * water. Internally calls overrideLiquidSettings
	 *
	 * @param maintainWater
	 *            True by default
	 * @return This instance
	 */
	public JigsawPoolBuilder maintainWater(boolean maintainWater)
	{
		return this.overrideLiquidSettings(maintainWater ? LiquidSettings.APPLY_WATERLOGGING : LiquidSettings.IGNORE_WATERLOGGING);
	}
	
	/**
	 * Determines how liquids should behave
	 * 
	 * @param settings
	 * @return This instance
	 */
	public JigsawPoolBuilder overrideLiquidSettings(LiquidSettings settings)
	{
		return this.overrideLiquidSettings(Optional.ofNullable(settings));
	}

	/**
	 * Determines how liquids should behave
	 * 
	 * @param settings
	 * @return This instance
	 */
	public JigsawPoolBuilder overrideLiquidSettings(Optional<LiquidSettings> settings)
	{
		this.overrideLiquidSettings = settings;
		return this;
	}

	/**
	 * Generates a pool of pieces
	 *
	 * @return The built pieces to be used in registry
	 */
	public ImmutableList<Pair<Function<StructureTemplatePool.Projection, ? extends StructurePoolElement>, Integer>> build()
	{
		List<Pair<Function<StructureTemplatePool.Projection, ? extends StructurePoolElement>, Integer>> jigsawList = new ArrayList<>();
		this.names.forEach((rl, i) -> jigsawList.add(Pair.of(createGelPiece(rl, this.jigsawRegistryHelper.processorLookup.getOrThrow(this.processors), this.overrideLiquidSettings, false), i)));

		return ImmutableList.copyOf(jigsawList);
	}

	/**
	 * Generates a pool of pieces with the default settings
	 *
	 * @param pieceMap
	 *            A map containing the pieces and their weights
	 * @return The built pieces to be used in registry
	 */
	public static ImmutableList<Pair<Function<StructureTemplatePool.Projection, ? extends StructurePoolElement>, Integer>> build(Map<StructurePoolElement, Integer> pieceMap)
	{
		List<Pair<Function<StructureTemplatePool.Projection, ? extends StructurePoolElement>, Integer>> jigsawList = new ArrayList<>();
		pieceMap.forEach((jp, i) -> jigsawList.add(Pair.of(placement -> jp, i)));

		return ImmutableList.copyOf(jigsawList);
	}

	/**
	 * Generates a pool of pieces with the default settings
	 *
	 * @param pieces
	 * @return The built pieces to be used in registry
	 */
	public static ImmutableList<Pair<Function<StructureTemplatePool.Projection, ? extends StructurePoolElement>, Integer>> build(StructurePoolElement... pieces)
	{
		List<Pair<Function<StructureTemplatePool.Projection, ? extends StructurePoolElement>, Integer>> pairs = new ArrayList<>();
		for (StructurePoolElement p : pieces)
			pairs.add(Pair.of(placement -> p, 1));

		return ImmutableList.copyOf(pairs);
	}

	/**
	 * Combines the jigsaw pool builders together after building them. This is used
	 * in cases where multiple structures exist in the same pool using different
	 * settings, processors, etc
	 *
	 * @param builders
	 * @return The built pieces to be used in registry
	 */
	public static ImmutableList<Pair<Function<StructureTemplatePool.Projection, ? extends StructurePoolElement>, Integer>> collect(JigsawPoolBuilder... builders)
	{
		return collect(Arrays.asList(builders));
	}

	/**
	 * Combines the JigsawPoolBuilders together after building them. This is used in
	 * cases where multiple structures exist in the same pool using different
	 * settings, processors, etc
	 *
	 * @param builders
	 * @return The built pieces to be used in registry
	 */
	public static ImmutableList<Pair<Function<StructureTemplatePool.Projection, ? extends StructurePoolElement>, Integer>> collect(List<JigsawPoolBuilder> builders)
	{
		List<Pair<Function<StructureTemplatePool.Projection, ? extends StructurePoolElement>, Integer>> pairs = new ArrayList<>();
		for (JigsawPoolBuilder builder : builders)
			pairs.addAll(builder.build());
		return ImmutableList.copyOf(pairs);
	}

	/**
	 * Creates a copy of this builder. Used in cases where similar settings are used
	 * across multiple builders
	 *
	 * @return The cloned builder
	 */
	public JigsawPoolBuilder clone()
	{
		return new JigsawPoolBuilder(this.jigsawRegistryHelper).weight(this.weight).namesR(this.names).overrideLiquidSettings(this.overrideLiquidSettings).processors(this.processors);
	}

	/**
	 * Creates a gel structure piece for internal use
	 *
	 * @param name
	 * @param processors
	 * @param maintainWater
	 * @param ignoreEntities
	 * @return The gel structure piece function
	 */
	private static Function<StructureTemplatePool.Projection, GelSinglePoolElement> createGelPiece(ResourceLocation name, Holder<StructureProcessorList> processors, Optional<LiquidSettings> overrideLiquidSettings, boolean ignoreEntities)
	{
		return placement -> new GelSinglePoolElement(Either.left(name), processors, placement, overrideLiquidSettings, ignoreEntities);
	}
}
