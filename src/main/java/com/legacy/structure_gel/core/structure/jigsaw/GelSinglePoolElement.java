package com.legacy.structure_gel.core.structure.jigsaw;

import java.util.Optional;
import java.util.function.Function;

import com.legacy.structure_gel.api.data_handler.handlers.DataHandler;
import com.legacy.structure_gel.api.structure.GelTemplate;
import com.legacy.structure_gel.api.structure.base.IModifyState;
import com.legacy.structure_gel.api.structure.jigsaw.ExtendedJigsawStructurePiece;
import com.legacy.structure_gel.api.structure.processor.RemoveGelStructureProcessor;
import com.legacy.structure_gel.core.registry.SGRegistry;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.StructureMode;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElementType;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

/**
 * A jigsaw pool element without the normal ignore air and structure block
 * processor in favor of {@link RemoveGelStructureProcessor}.<br>
 * <br>
 * Includes the following:<br>
 * - Improved water interaction handling.<br>
 * - Data structure block functionality. See
 * {@link ExtendedJigsawStructurePiece}<br>
 * - Block placement overrides, separate from processors. See
 * {@link IModifyState}<br>
 * - Fixes for entity rotation within the structure.
 *
 * @author Silver_David
 */
public class GelSinglePoolElement extends SinglePoolElement
{
	public static final Codec<Either<ResourceLocation, StructureTemplate>> POOL_CODEC = Codec.of(GelSinglePoolElement::encodePool, ResourceLocation.CODEC.map(Either::left));
	public static final MapCodec<GelSinglePoolElement> CODEC = RecordCodecBuilder.mapCodec(instance ->
	{
		// @formatter:off
		return instance.group(
				encodeTemplate(), 
				encodeProcessor(), 
				projectionCodec(), 
				overrideLiquidSettingsCodec(),
				Codec.BOOL.optionalFieldOf("ingore_entities", false).forGetter(element -> element.ignoreEntities))
				.apply(instance, GelSinglePoolElement::new);
		// @formatter:on
	});
	private boolean ignoreEntities = false;

	private static <T> DataResult<T> encodePool(Either<ResourceLocation, StructureTemplate> locationTemplate, DynamicOps<T> dyn, T data)
	{
		Optional<ResourceLocation> optional = locationTemplate.left();
		return !optional.isPresent() ? DataResult.error(() -> "Can not serialize a runtime pool element") : ResourceLocation.CODEC.encode(optional.get(), dyn, data);
	}

	protected static <E extends GelSinglePoolElement> RecordCodecBuilder<E, Holder<StructureProcessorList>> encodeProcessor()
	{
		return StructureProcessorType.LIST_CODEC.fieldOf("processors").forGetter(piece -> piece.processors);
	}

	protected static <E extends GelSinglePoolElement> RecordCodecBuilder<E, Either<ResourceLocation, StructureTemplate>> encodeTemplate()
	{
		return POOL_CODEC.fieldOf("location").forGetter(piece -> piece.template);
	}

	public GelSinglePoolElement(Either<ResourceLocation, StructureTemplate> location, Holder<StructureProcessorList> processors, StructureTemplatePool.Projection projection, Optional<LiquidSettings> overrideLiquidSettings, boolean ignoreEntities)
	{
		super(location, processors, projection, overrideLiquidSettings);
		this.ignoreEntities = ignoreEntities;
	}

	/**
	 * Sets if entities can be allowed to generate in the structure
	 */
	public GelSinglePoolElement setIgnoreEntities(boolean ignoreEntities)
	{
		this.ignoreEntities = ignoreEntities;
		return this;
	}

	/**
	 * Gets the placement settings for the piece to use
	 */
	@Override
	protected StructurePlaceSettings getSettings(Rotation rotation, BoundingBox boundingBox, LiquidSettings liquidSettings, boolean isLegacy)
	{
		StructurePlaceSettings settings = super.getSettings(rotation, boundingBox, liquidSettings, isLegacy);
		settings.setIgnoreEntities(this.ignoreEntities);
		
		settings.getProcessors().remove(BlockIgnoreProcessor.STRUCTURE_BLOCK);
		settings.getProcessors().add(0, RemoveGelStructureProcessor.INSTANCE);
		
		return settings;
	}

	/**
	 * Changes how the place function works to allow for data structure blocks
	 * 
	 * @return True if the structure piece placed
	 */
	@Override
	public boolean place(StructureTemplateManager templateManager, WorldGenLevel level, StructureManager structureManager, ChunkGenerator chunkGen, BlockPos templatePos, BlockPos jigsawPos, Rotation rotation, BoundingBox bounds, RandomSource rand, LiquidSettings liquidSettings, boolean isLegacy)
	{
		if (this instanceof SinglePoolElementExtension singlePool && singlePool.structure_gel$getPiece() instanceof ExtendedJigsawStructurePiece extendedJigsawPiece)
		{
			return this.place(templateManager, level, structureManager, chunkGen, templatePos, jigsawPos, rotation, bounds, rand, liquidSettings, isLegacy, extendedJigsawPiece);
		}
		else
		{
			return super.place(templateManager, level, structureManager, chunkGen, templatePos, jigsawPos, rotation, bounds, rand, liquidSettings, isLegacy);
		}
	}

	public boolean place(StructureTemplateManager templateManager, WorldGenLevel level, StructureManager structureManager, ChunkGenerator chunkGen, BlockPos templatePos, BlockPos jigsawPos, Rotation rotation, BoundingBox bounds, RandomSource rand, LiquidSettings liquidSettings, boolean isLegacy, ExtendedJigsawStructurePiece extendedJigsawPiece)
	{
		StructureTemplate template = this.getTemplate(templateManager);
		StructurePlaceSettings placementSettings = this.getSettings(rotation, bounds, liquidSettings, isLegacy);
		if (!GelTemplate.placeInWorld(template, level, templatePos, jigsawPos, placementSettings, rand, 18, extendedJigsawPiece::modifyState))
			return false;

		processDataHandlers(extendedJigsawPiece, template, templatePos, jigsawPos, placementSettings, level, rotation, bounds, rand);

		for (StructureTemplate.StructureBlockInfo blockInfo : StructureTemplate.processBlockInfos(level, templatePos, jigsawPos, placementSettings, this.getDataMarkers(templateManager, templatePos, rotation, false), template))
		{
			this.handleDataMarker(level, blockInfo, templatePos, rotation, rand, bounds);
			if (blockInfo.nbt() != null && level.getBlockState(blockInfo.pos()).is(Blocks.STRUCTURE_BLOCK) && StructureMode.valueOf(blockInfo.nbt().getString("mode")) == StructureMode.DATA)
				extendedJigsawPiece.handleDataMarker(blockInfo.nbt().getString("metadata"), blockInfo.pos(), level, rand, bounds);
		}

		return true;
	}

	public static void processDataHandlers(StructurePiece piece, StructureTemplate template, BlockPos templatePos, BlockPos jigsawPos, StructurePlaceSettings placeSettings, WorldGenLevel level, Rotation rotation, BoundingBox bounds, RandomSource rand)
	{
		var dataHandler = SGRegistry.Blocks.DATA_HANDLER.get();
		for (StructureTemplate.StructureBlockInfo blockInfo : StructureTemplate.processBlockInfos(level, templatePos, jigsawPos, placeSettings, template.filterBlocks(templatePos, new StructurePlaceSettings().setRotation(rotation), dataHandler, false), template))
		{
			if (blockInfo.nbt() != null)
			{
				BlockState state = level.getBlockState(blockInfo.pos());
				if (state.is(dataHandler))
				{
					DataHandler.process(state, blockInfo.nbt(), blockInfo.pos(), level, rand, bounds, piece, false);
				}
			}
		}
	}

	/**
	 * @return The name of the piece
	 */
	public ResourceLocation getLocation()
	{
		return this.template.left().get();
	}

	/**
	 * @param templateManager
	 * @return The structure template for this piece
	 */
	private StructureTemplate getTemplate(StructureTemplateManager templateManager)
	{
		return this.template.map(templateManager::getOrCreate, Function.identity());
	}

	/**
	 * @return The structure pool element type
	 */
	@Override
	public StructurePoolElementType<?> getType()
	{
		return SGRegistry.JigsawDeserializers.GEL_SINGLE_POOL_ELEMENT.get();
	}

	@Override
	public String toString()
	{
		return "Gel[" + this.template + "]";
	}
}
