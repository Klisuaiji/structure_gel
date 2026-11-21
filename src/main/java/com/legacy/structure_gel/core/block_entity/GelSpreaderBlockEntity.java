package com.legacy.structure_gel.core.block_entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;

import com.legacy.structure_gel.api.block.gel.GelSpreadBehavior;
import com.legacy.structure_gel.api.block.gel.GelSpreadRestriction;
import com.legacy.structure_gel.api.block.gel.StructureGelBlock;
import com.legacy.structure_gel.core.block.GelSpreaderBlock;
import com.legacy.structure_gel.core.registry.SGRegistry;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class GelSpreaderBlockEntity extends SGBlockEntity
{
	private Direction.Axis axis = Direction.Axis.Y;
	private ItemStack placedBy = ItemStack.EMPTY;
	private ResourceKey<Block> gelKey = SGRegistry.Blocks.RED_GEL.getKey();
	@Nullable
	private StructureGelBlock gel;

	public GelSpreaderBlockEntity(BlockPos pos, BlockState state)
	{
		super(SGRegistry.BlockEntities.GEL_SPREADER.get(), pos, state);
		this.axis = state.getValue(GelSpreaderBlock.AXIS);
	}

	static final String GEL_KEY = "gel";

	@Override
	protected void saveAdditional(CompoundTag tag, Provider registries)
	{
		super.saveAdditional(tag, registries);
		tag.putString(GEL_KEY, gelKey.location().toString());
	}

	@Override
	protected void loadAdditional(CompoundTag tag, Provider registries)
	{
		super.loadAdditional(tag, registries);
		if (tag.contains(GEL_KEY, Tag.TAG_STRING))
		{
			ResourceLocation gelName = ResourceLocation.tryParse(tag.getString(GEL_KEY));
			if (gelName != null)
				this.gelKey = ResourceKey.create(Registries.BLOCK, gelName);
		}
	}

	public void applyProperties(ItemStack stack, RegistryAccess registries)
	{
		if (stack.getItem() instanceof BlockItem blockItem)
		{
			this.placedBy = stack.copy();
			this.setGel(registries.registryOrThrow(Registries.BLOCK).getResourceKey(blockItem.getBlock()));
		}
	}

	public StructureGelBlock getGel(Provider registries)
	{
		if (this.gel == null)
		{
			if (registries.lookupOrThrow(Registries.BLOCK).getOrThrow(this.gelKey).value() instanceof StructureGelBlock gel)
			{
				this.gel = gel;
			}
			else
				throw new IllegalStateException("The block " + this.gelKey.location() + " must be of type " + SGRegistry.BlockTypes.GEL.getKey().location());
		}
		return this.gel;
	}

	public void setGel(Optional<ResourceKey<Block>> key)
	{
		if (key.isEmpty())
			return;
		this.gelKey = key.get();
		this.gel = null;
	}

	private List<BlockPos> modifiedPositions = new ArrayList<>();

	private void tick(ServerLevel level, BlockPos pos, BlockState state)
	{
		// This should be empty if we just started. Add this block's position.
		if (this.modifiedPositions.isEmpty())
			this.modifiedPositions.add(pos);

		List<BlockPos> newlyPlaced = new ArrayList<>();
		StructureGelBlock gel = this.getGel(level.registryAccess());
		GelSpreadBehavior spreadBehavior = gel.getSpreadBehavior();
		List<GelSpreadRestriction> restrictions = gel.getSpreadRestrictions();
		Optional<Direction.Axis> axis = Optional.of(this.axis);

		// Place gel blocks offset from each already placed gel block and store the valid placements
		for (BlockPos placedPos : this.modifiedPositions)
		{
			var context = new GelSpreadBehavior.Context(placedPos, axis);
			spreadBehavior.getSpreadOffsets(context).stream().map(context.spreadFrom()::offset).filter(spreadPos ->
			{
				GelSpreadRestriction.Context rContext = new GelSpreadRestriction.Context(level, pos, this.placedBy, spreadPos);
				for (var r : restrictions)
					if (!r.isPermitted(rContext))
						return false;
				return true;
			}).forEach(spreadPos ->
			{
				if (level.getBlockState(spreadPos).isAir() && this.placeGel(level, gel, spreadPos))
				{
					newlyPlaced.add(spreadPos);
				}
			});
		}

		// Set the placed positions to the newly placed blocks. If none were placed, this can be removed
		this.modifiedPositions = newlyPlaced;
		if (modifiedPositions.isEmpty())
			this.placeGel(level, gel, pos);
	}

	private boolean placeGel(ServerLevel level, Block gel, BlockPos pos)
	{
		return StructureGelBlock.placeGel(level, gel, pos);
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state, GelSpreaderBlockEntity blockEntity)
	{
		if (level instanceof ServerLevel)
		{
			blockEntity.tick((ServerLevel) level, pos, state);
		}
	}
}
