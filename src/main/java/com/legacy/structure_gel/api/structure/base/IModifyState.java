package com.legacy.structure_gel.api.structure.base;

import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

/**
 * Used to modify blocks placed by structures.
 *
 * @author Silver_David
 */
public interface IModifyState
{
	public static final IModifyState NOOP = (level, rand, pos, originalState) -> originalState;
	
	/**
	 * Modifies the passed state. This method is called after processors are applied
	 *
	 * @param level
	 * @param rand
	 * @param pos
	 * @param originalState
	 * @return The new block state to place, or null to prevent placement
	 */
	@Nullable
	BlockState modifyState(ServerLevelAccessor level, RandomSource rand, BlockPos pos, BlockState originalState);

	/**
	 * Attempts to merge the blockstate properties in "from" into "to"
	 * 
	 * @param to
	 *            The blockstate to merge properties into
	 * @param from
	 *            The blockstate to get properties from
	 * @return The merged blockstate
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static BlockState mergeStates(BlockState to, BlockState from)
	{
		BlockState state = to;
		for (Map.Entry<Property<?>, Comparable<?>> entry : from.getValues().entrySet())
		{
			Property property = entry.getKey();
			if (state.hasProperty(property))
			{
				state = state.setValue(property, (Comparable) entry.getValue());
			}
		}
		return state;
	}
}
