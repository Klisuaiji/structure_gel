package com.legacy.structure_gel.api.block_entity;

import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;

/**
 * Allows the block entity data to be rotated when used in a structure
 */
public interface IRotatable
{
	/**
	 * Rotate the block entity's data. Saving not included.
	 */
	void rotate(Rotation rotation);

	/**
	 * Rotate the block entity's data. Saving not included.
	 */
	void mirror(Mirror mirror);

	/**
	 * Transform the block entity's data. Saving not included.
	 */
	default void transform(Mirror mirror, Rotation rotation)
	{
		this.mirror(mirror);
		this.rotate(rotation);
	}
}
