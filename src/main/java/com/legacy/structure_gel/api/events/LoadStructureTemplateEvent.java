package com.legacy.structure_gel.api.events;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import com.legacy.structure_gel.core.StructureGelMod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.JigsawBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureEntityInfo;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.Event;

/**
 * Fires on the Forge event bus when a StructureTemplate first loads. Use this
 * to modify the blocks or entities in the StructureTemplate.
 */
public class LoadStructureTemplateEvent extends Event
{
	private final ResourceLocation id;
	private final List<StructureTemplate.Palette> palettes;
	private final List<StructureTemplate.StructureEntityInfo> entityInfoList;
	private final Vec3i size;
	private final RegistryAccess registryAccess;

	public LoadStructureTemplateEvent(ResourceLocation id, List<StructureTemplate.Palette> palettes, List<StructureTemplate.StructureEntityInfo> entityInfoList, Vec3i size, MinecraftServer server)
	{
		this.id = id;
		this.palettes = palettes;
		this.entityInfoList = entityInfoList;
		this.size = size;
		this.registryAccess = server.registryAccess();
	}

	public ResourceLocation getId()
	{
		return id;
	}

	public List<StructureTemplate.Palette> getPalettes()
	{
		return palettes;
	}

	public List<StructureTemplate.StructureEntityInfo> getEntityInfoList()
	{
		return entityInfoList;
	}

	public Vec3i getSize()
	{
		return size;
	}

	/**
	 * @return The block at the position passed, if it exists.
	 */
	public Optional<StructureBlockInfo> getBlock(StructureTemplate.Palette palette, BlockPos pos)
	{
		if (this.isInBounds(pos))
		{
			for (var info : palette.blocks())
				if (info.pos().equals(pos))
					return Optional.of(info);
		}
		else
		{
			StructureGelMod.LOGGER.warn("Attempted to get a block from a StructureTemplate outside of its size bounds. Template: {}, Pos: {}, Size: {}", id, pos, size);
		}
		return Optional.empty();
	}

	/**
	 * @return True if a block exists in the template for that position, and it
	 *         passes the predicate.
	 */
	public boolean hasBlock(StructureTemplate.Palette palette, BlockPos pos, Predicate<StructureBlockInfo> predicate)
	{
		return this.getBlock(palette, pos).map(predicate::test).orElse(false);
	}

	/**
	 * Replaces the block at the position passed with the passed block in all
	 * palettes. Must be within bounds.
	 */
	public void setBlock(BlockPos pos, BlockState state, CompoundTag tag)
	{
		for (var palette : this.getPalettes())
			this.setBlock(palette, pos, state, tag);
	}

	/**
	 * Replaces the block at the position passed with the passed block. Must be
	 * within bounds.
	 */
	public void setBlock(StructureTemplate.Palette palette, BlockPos pos, BlockState state, @Nullable CompoundTag tag)
	{
		this.setBlock(palette, new StructureBlockInfo(pos, state, tag));
	}

	/**
	 * Replaces the block at the position passed with the passed block. Must be
	 * within bounds.
	 */
	public void setBlock(StructureTemplate.Palette palette, StructureBlockInfo structureBlockInfo)
	{
		BlockPos placePos = structureBlockInfo.pos();
		if (this.isInBounds(placePos))
		{
			List<StructureBlockInfo> blocks = palette.blocks();
			blocks.removeIf(info -> info.pos().equals(placePos));
			blocks.add(structureBlockInfo);
		}
		else
			StructureGelMod.LOGGER.warn("Attempted to add a block to a StructureTemplate outside of its size bounds. Template: {}, Pos: {}, Size: {}", id, placePos, size);
	}

	/**
	 * Places a jigsaw at the passed position for all palettes. Modify the jigsaw
	 * data with jigsawConsumer
	 */
	public void setJigsaw(BlockState jigsawState, BlockPos pos, Consumer<JigsawBlockEntity> jigsawConsumer)
	{
		CompoundTag tag = this.getJigsawTag(jigsawState, pos, jigsawConsumer);
		for (var palette : this.getPalettes())
			this.setBlock(palette, pos, jigsawState, tag);
	}

	/**
	 * Places a jigsaw at the passed position. Modify the jigsaw data with
	 * jigsawConsumer
	 */
	public void setJigsaw(StructureTemplate.Palette palette, BlockState jigsawState, BlockPos pos, Consumer<JigsawBlockEntity> jigsawConsumer)
	{
		this.setBlock(palette, pos, jigsawState, this.getJigsawTag(jigsawState, pos, jigsawConsumer));
	}

	private CompoundTag getJigsawTag(BlockState jigsawState, BlockPos pos, Consumer<JigsawBlockEntity> jigsawConsumer)
	{
		JigsawBlockEntity jigsaw = new JigsawBlockEntity(pos, jigsawState);
		jigsawConsumer.accept(jigsaw);
		return jigsaw.saveWithoutMetadata(this.registryAccess);
	}

	/**
	 * Adds an entity at the passed position. Must be within bounds.
	 */
	public void addEntity(Vec3 pos, BlockPos blockPos, CompoundTag nbt)
	{
		this.addEntity(new StructureEntityInfo(pos, blockPos, nbt));
	}

	/**
	 * Adds an entity at the passed position. Must be within bounds.
	 */
	public void addEntity(StructureEntityInfo structureEntityInfo)
	{
		BlockPos placePos = structureEntityInfo.blockPos;
		if (this.isInBounds(placePos))
			this.entityInfoList.add(structureEntityInfo);
		else
			StructureGelMod.LOGGER.warn("Attempted to add an entity to a StructureTemplate outside of its size bounds. Template: {}, Pos: {}, Size: {}", id, placePos, size);
	}

	/**
	 * @return True if the position passed is within the template's bounds.
	 */
	public boolean isInBounds(BlockPos pos)
	{
		return pos.getX() < size.getX() && pos.getY() < size.getY() && pos.getZ() < size.getZ();
	}

	/**
	 * @return The position passed, moved to be within the template's bounds. Useful
	 *         for jigsaws to make sure they're at the edge in case the template is
	 *         larger than expected.
	 */
	public BlockPos moveInBounds(BlockPos pos)
	{
		return new BlockPos(Mth.clamp(pos.getX(), 0, size.getX() - 1), Mth.clamp(pos.getY(), 0, size.getY() - 1), Mth.clamp(pos.getZ(), 0, size.getZ() - 1));
	}
}
