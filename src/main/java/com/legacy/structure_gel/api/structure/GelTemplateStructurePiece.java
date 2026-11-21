package com.legacy.structure_gel.api.structure;

import java.util.function.Function;

import javax.annotation.Nullable;

import com.legacy.structure_gel.api.data_handler.handlers.DataHandler;
import com.legacy.structure_gel.api.structure.base.IModifyState;
import com.legacy.structure_gel.api.structure.processor.RemoveGelStructureProcessor;
import com.legacy.structure_gel.core.registry.SGRegistry;
import com.legacy.structure_gel.core.util.Internal;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

/**
 * An extension of {@link TemplateStructurePiece} with more extensible methods
 * and compatibility with gel blocks.<br>
 * <br>
 * Includes the following:<br>
 * - Built in variables for name (structure file location), rotation, and
 * mirroring. Includes NBT handling.<br>
 * - Methods to simplify adding processors and setting PlacementSettings.<br>
 * - Block placement overrides, separate from processors. See
 * {@link IModifyState}<br>
 * - Fixes for entity rotation within the structure.
 *
 * @author Silver_David
 */
public abstract class GelTemplateStructurePiece extends TemplateStructurePiece implements IModifyState
{
	public Rotation rotation = Rotation.NONE;
	public Mirror mirror = Mirror.NONE;

	/**
	 * Standard constructor with all arguments intact
	 * 
	 * @param structurePieceType
	 * @param componentType
	 * @param structureManager
	 * @param templateName
	 * @param placeSettings
	 * @param pos
	 */
	public GelTemplateStructurePiece(StructurePieceType structurePieceType, int componentType, StructureTemplateManager structureManager, ResourceLocation templateName, StructurePlaceSettings placeSettings, BlockPos pos)
	{
		super(structurePieceType, componentType, structureManager, templateName, templateName.toString(), placeSettings, pos);
	}

	/**
	 * Constructor without the placement settings argument. Override
	 * {@link #getPlaceSettings(StructureTemplateManager)} and call
	 * {@link #setupPlaceSettings(StructureTemplateManager)} at the end of your
	 * constructor
	 * 
	 * @param structurePieceType
	 * @param componentType
	 * @param structureManager
	 * @param templateName
	 * @param pos
	 */
	public GelTemplateStructurePiece(StructurePieceType structurePieceType, int componentType, StructureTemplateManager structureManager, ResourceLocation templateName, BlockPos pos)
	{
		this(structurePieceType, componentType, structureManager, templateName, new StructurePlaceSettings(), pos);
	}

	/**
	 * This consturctor is used to read existing template data from the world's save
	 * data. All values should be stored in nbt
	 * 
	 * @param structurePieceType
	 * @param nbt
	 * @param structureManager
	 * @param placeSettingsGetter
	 */
	public GelTemplateStructurePiece(StructurePieceType structurePieceType, CompoundTag nbt, StructureTemplateManager structureManager, Function<ResourceLocation, StructurePlaceSettings> placeSettingsGetter)
	{
		super(structurePieceType, nbt, structureManager, placeSettingsGetter);
		this.rotation = getRotation(nbt);
		this.mirror = getMirror(nbt);
	}

	/**
	 * This consturctor is used to read existing template data from the world's save
	 * data. All values should be stored in nbt. Override
	 * {@link #getPlaceSettings(StructureTemplateManager)} and call
	 * {@link #setupPlaceSettings(StructureTemplateManager)} at the end of your
	 * constructor
	 * 
	 * @param structurePieceType
	 * @param nbt
	 * @param structureManager
	 */
	public GelTemplateStructurePiece(StructurePieceType structurePieceType, CompoundTag nbt, StructureTemplateManager structureManager)
	{
		this(structurePieceType, nbt, structureManager, name -> new StructurePlaceSettings());
	}

	/**
	 * Override this method with your own placement settings if you need to this
	 * after the constructor has finished. Then call
	 * {@link #setupPlaceSettings(StructureTemplateManager)} at the end of your
	 * constructor
	 * 
	 * @param structureManager
	 * 
	 * @return This structure piece's placement settings
	 */
	protected StructurePlaceSettings getPlaceSettings(StructureTemplateManager structureManager)
	{
		return new StructurePlaceSettings();
	}

	/**
	 * If placement settings need to be setup after the constructor is finished,
	 * call this method at the end of your constructor and override
	 * {@link #getPlaceSettings(StructureTemplateManager)} with the placement
	 * settings you need
	 * 
	 * @param structureManager
	 */
	protected void setupPlaceSettings(StructureTemplateManager structureManager)
	{
		StructurePlaceSettings placeSettings = this.getPlaceSettings(structureManager).setRotation(this.rotation).setMirror(this.mirror);
		placeSettings.setRotation(this.rotation);
		placeSettings.setMirror(this.mirror);
		if (this.useGelProcessor() && !placeSettings.getProcessors().contains(RemoveGelStructureProcessor.INSTANCE)) // Automatically done, but setting it here helps with performance
			placeSettings.addProcessor(RemoveGelStructureProcessor.INSTANCE);
		this.placeSettings = placeSettings;
		this.boundingBox = this.template.getBoundingBox(this.placeSettings, this.templatePosition);
	}

	/**
	 * @return If this piece should use the {@link RemoveGelStructureProcessor}
	 */
	protected boolean useGelProcessor()
	{
		return true;
	}

	/**
	 * @param nbt
	 * @return The piece's pos from nbt
	 */
	public static BlockPos getPos(CompoundTag nbt)
	{
		return new BlockPos(nbt.getInt("TPX"), nbt.getInt("TPY"), nbt.getInt("TPZ"));
	}

	/**
	 * @param nbt
	 * @return The piece's rotation from nbt
	 */
	public static Rotation getRotation(CompoundTag nbt)
	{
		return Rotation.valueOf(nbt.getString("Rot"));
	}

	/**
	 * @param nbt
	 * @return The piece's mirror from nbt
	 */
	public static Mirror getMirror(CompoundTag nbt)
	{
		return Mirror.valueOf(nbt.getString("Mirror"));
	}

	/**
	 * Use this to store data about this piece instance into nbt. This will be
	 * loaded later when the world needs it
	 *
	 * @param level
	 * @param nbt
	 */
	@Override
	protected void addAdditionalSaveData(StructurePieceSerializationContext level, CompoundTag nbt)
	{
		super.addAdditionalSaveData(level, nbt);
		nbt.putString("Rot", this.rotation.name());
		nbt.putString("Mirror", this.mirror.name());
	}

	@Internal
	@Override
	public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator chunkGen, RandomSource rand, BoundingBox bounds, ChunkPos chunkPos, BlockPos pos)
	{
		// Data handlers and IModifyState are handled in a TemplateStructurePieceMixin
		super.postProcess(level, structureManager, chunkGen, rand, bounds, chunkPos, pos);
	}

	public static void processDataHandlers(TemplateStructurePiece piece, StructureTemplate template, BlockPos templatePos, StructurePlaceSettings placeSettings, WorldGenLevel level, RandomSource rand, BoundingBox bounds)
	{
		var dataHandler = SGRegistry.Blocks.DATA_HANDLER.get();
		for (StructureTemplate.StructureBlockInfo blockInfo : template.filterBlocks(templatePos, placeSettings, dataHandler))
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

	@Override
	@Nullable
	public BlockState modifyState(ServerLevelAccessor level, RandomSource rand, BlockPos pos, BlockState originalState)
	{
		return originalState;
	}
}
