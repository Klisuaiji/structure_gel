package com.legacy.structure_gel.api.util;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Helper methods for dealing with VoxelShapes
 *
 * @author David
 */
public class VoxelShapeUtil
{
	/**
	 * Horizonal direction values.
	 */
	public static final List<Direction> HORIZONTAL_DIRS = List.of(Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST);

	/**
	 * 
	 * @param northFacingShape
	 * @param validDirections
	 * @return A map consisting of shapes made by rotating the passed voxel shape
	 *         for each direction passed. Rotations are based on how block models
	 *         are typically rotated in their blockstate json.
	 */
	public static Map<Direction, VoxelShape> createRotations(VoxelShape northFacingShape, Set<Direction> validDirections)
	{
		EnumMap<Direction, VoxelShape> shapes = new EnumMap<>(Direction.class);

		boolean up = validDirections.contains(Direction.UP);
		boolean down = validDirections.contains(Direction.DOWN);
		if (up || down)
		{
			VoxelShape[] verticalShapes = new VoxelShape[] { Shapes.empty(), Shapes.empty() };
			northFacingShape.forAllBoxes((x, y, z, a, b, c) ->
			{
				// Up
				if (up)
					verticalShapes[0] = Shapes.or(verticalShapes[0], Shapes.box(x, 1.0 - c, y, a, 1.0 - z, b));

				// Down
				if (down)
					verticalShapes[1] = Shapes.or(verticalShapes[1], Shapes.box(x, z, y, a, c, b));
			});

			if (up)
				shapes.put(Direction.UP, verticalShapes[0]);
			if (down)
				shapes.put(Direction.DOWN, VoxelShapeUtil.mirror(verticalShapes[1], Direction.Axis.Z));
		}

		for (Direction dir : HORIZONTAL_DIRS)
		{
			if (validDirections.contains(dir))
				shapes.put(dir, VoxelShapeUtil.rotate(northFacingShape, dir));
		}

		return shapes;
	}

	/**
	 * 
	 * @param northFacingShape
	 * @param validDirections
	 * @return A map consisting of shapes made by rotating the passed voxel shape
	 *         for each direction passed. Rotations are based on how block models
	 *         are typically rotated in their blockstate json.
	 */
	public static Map<Direction, VoxelShape> createRotations(VoxelShape northFacingShape, Direction... validDirections)
	{
		return createRotations(northFacingShape, Set.of(validDirections));
	}

	/**
	 * 
	 * @param northFacingShape
	 * @return A map consisting of shapes made by rotating the passed voxel shape
	 *         for all directions. Rotations are based on how block models are
	 *         typically rotated in their blockstate json.
	 */
	public static Map<Direction, VoxelShape> createRotations(VoxelShape northFacingShape)
	{
		return createRotations(northFacingShape, Direction.values());
	}

	/**
	 * Rotates the provider {@link VoxelShape} to the new direction, assuming it
	 * starts by facing north.
	 *
	 * @param shape
	 * @param newDir
	 * @return {@link VoxelShape}
	 */
	public static VoxelShape rotate(VoxelShape shape, Direction newDir)
	{
		return rotate(shape, Direction.NORTH, newDir);
	}

	/**
	 * Rotates the provided {@link VoxelShape} from one direction to the other.
	 *
	 * @param shape
	 * @param originalDir
	 * @param newDir
	 * @return {@link VoxelShape}
	 */
	public static VoxelShape rotate(VoxelShape shape, Direction originalDir, Direction newDir)
	{
		if (originalDir != newDir)
		{
			VoxelShape[] newShape = new VoxelShape[] { Shapes.empty() };
			shape.forAllBoxes((x, y, z, a, b, c) ->
			{
				double i = 1 - c;
				double j = 1 - z;
				newShape[0] = Shapes.or(newShape[0], Shapes.box(Math.min(i, j), y, x, Math.max(i, j), b, a));
			});
			return rotate(newShape[0], originalDir.getClockWise(), newDir);
		}
		return shape;
	}

	/**
	 * Mirrors the provided {@link VoxelShape} based on the axis of facingProperty.
	 *
	 * @param shape
	 * @param facingProperty
	 * @return {@link VoxelShape}
	 */
	public static VoxelShape mirror(VoxelShape shape, Direction facingProperty)
	{
		return mirror(shape, facingProperty.getAxis());
	}

	/**
	 * Mirrors the provided {@link VoxelShape}. {@link Mirror#FRONT_BACK} for x
	 * axis. {@link Mirror#LEFT_RIGHT} for z axis.
	 *
	 * @param shape
	 * @param mirror
	 * @return {@link VoxelShape}
	 */
	public static VoxelShape mirror(VoxelShape shape, Mirror mirror)
	{
		return mirror(shape, mirror == Mirror.FRONT_BACK ? Axis.X : Axis.Z);
	}

	/**
	 * Mirrors the provided {@link VoxelShape} along the input axis.
	 *
	 * @param shape
	 * @param axis
	 * @return {@link VoxelShape}
	 */
	public static VoxelShape mirror(VoxelShape shape, Axis axis)
	{
		VoxelShape[] newShape = new VoxelShape[] { Shapes.empty() };
		switch (axis)
		{
		case X:
			shape.forAllBoxes((x, y, z, a, b, c) ->
			{
				double i = 1 - x;
				double j = 1 - a;
				newShape[0] = Shapes.or(newShape[0], Shapes.box(Math.min(i, j), y, z, Math.max(i, j), b, c));
			});
			break;
		case Z:
			shape.forAllBoxes((x, y, z, a, b, c) ->
			{
				double i = 1 - z;
				double j = 1 - c;
				newShape[0] = Shapes.or(newShape[0], Shapes.box(x, y, Math.min(i, j), a, b, Math.max(i, j)));
			});
			break;
		case Y:
			shape.forAllBoxes((x, y, z, a, b, c) ->
			{
				double i = 1 - y;
				double j = 1 - b;
				newShape[0] = Shapes.or(newShape[0], Shapes.box(x, Math.min(i, j), z, a, Math.max(i, j), c));
			});
			break;
		}
		return newShape[0];
	}
}
