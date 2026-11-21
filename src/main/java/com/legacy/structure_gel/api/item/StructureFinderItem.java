package com.legacy.structure_gel.api.item;

import java.util.Optional;

import javax.annotation.Nullable;

import com.legacy.structure_gel.core.registry.SGRegistry;
import com.mojang.datafixers.util.Pair;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;

public interface StructureFinderItem
{
	default int searchRange()
	{
		return 100;
	}

	@Nullable
	default BlockPos findAndStoreStructure(ItemStack stack, ServerLevel level, BlockPos searchPos)
	{
		ResourceKey<Structure> structure = this.getStructure(stack);
		if (structure != null)
		{
			Optional<Holder.Reference<Structure>> holder = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).get(structure);
			if (holder.isPresent())
			{
				@Nullable
				Pair<BlockPos, Holder<Structure>> nearest = level.getChunkSource().getGenerator().findNearestMapStructure(level, HolderSet.direct(holder.get()), searchPos, this.searchRange(), false);
				if (nearest != null)
				{
					BlockPos pos = nearest.getFirst();
					this.setStructurePos(stack, pos);
					this.setStructureDimension(stack, level.dimension());
					return pos;
				}
			}
		}
		return null;
	}

	@Nullable
	default ResourceKey<Structure> getStructure(ItemStack stack)
	{
		return stack.get(SGRegistry.DataComponents.STRUCTURE);
	}

	default void setStructure(ItemStack stack, @Nullable ResourceKey<Structure> structure)
	{
		if (structure == null)
			stack.remove(SGRegistry.DataComponents.STRUCTURE);
		else
			stack.set(SGRegistry.DataComponents.STRUCTURE, structure);
	}

	@Nullable
	default Optional<GlobalPos> getStructureLocation(ItemStack stack)
	{
		return Optional.ofNullable(stack.get(SGRegistry.DataComponents.STRUCTURE_LOCATION));
	}
	
	default void setStructureLocation(ItemStack stack, Optional<GlobalPos> location)
	{		
		if (location.isPresent())
			stack.set(SGRegistry.DataComponents.STRUCTURE_LOCATION, location.get());
		else
			stack.remove(SGRegistry.DataComponents.STRUCTURE_LOCATION);
	}
	
	@Nullable
	default BlockPos getStructurePos(ItemStack stack)
	{
		return this.getStructureLocation(stack).map(GlobalPos::pos).orElse(null);
	}

	default void setStructurePos(ItemStack stack, @Nullable BlockPos structurePos)
	{
		if (structurePos == null)
			this.setStructureLocation(stack, null);
		else
			this.setStructureLocation(stack, this.getStructureLocation(stack).map(g -> new GlobalPos(g.dimension(), structurePos)));
	}
	
	@Nullable
	default ResourceKey<Level> getStructureDimension(ItemStack stack)
	{
		return this.getStructureLocation(stack).map(GlobalPos::dimension).orElse(null);
	}

	default void setStructureDimension(ItemStack stack, @Nullable ResourceKey<Level> dimension)
	{
		if (dimension == null)
			this.setStructureLocation(stack, null);
		else
			this.setStructureLocation(stack, this.getStructureLocation(stack).map(g -> new GlobalPos(dimension, g.pos())));
	}
}
