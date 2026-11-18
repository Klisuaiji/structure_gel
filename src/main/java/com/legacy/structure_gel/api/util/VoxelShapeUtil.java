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

public class VoxelShapeUtil {
   public static final List<Direction> HORIZONTAL_DIRS = List.of(Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST);

   public static Map<Direction, VoxelShape> createRotations(VoxelShape northFacingShape, Set<Direction> validDirections) {
      EnumMap<Direction, VoxelShape> shapes = new EnumMap<>(Direction.class);
      boolean up = validDirections.contains(Direction.UP);
      boolean down = validDirections.contains(Direction.DOWN);
      if (up || down) {
         VoxelShape[] verticalShapes = new VoxelShape[]{Shapes.empty(), Shapes.empty()};
         northFacingShape.forAllBoxes((x, y, z, a, b, c) -> {
            if (up) {
               verticalShapes[0] = Shapes.or(verticalShapes[0], Shapes.box(x, 1.0 - c, y, a, 1.0 - z, b));
            }

            if (down) {
               verticalShapes[1] = Shapes.or(verticalShapes[1], Shapes.box(x, z, y, a, c, b));
            }
         });
         if (up) {
            shapes.put(Direction.UP, verticalShapes[0]);
         }

         if (down) {
            shapes.put(Direction.DOWN, mirror(verticalShapes[1], Axis.Z));
         }
      }

      for (Direction dir : HORIZONTAL_DIRS) {
         if (validDirections.contains(dir)) {
            shapes.put(dir, rotate(northFacingShape, dir));
         }
      }

      return shapes;
   }

   public static Map<Direction, VoxelShape> createRotations(VoxelShape northFacingShape, Direction... validDirections) {
      return createRotations(northFacingShape, Set.of(validDirections));
   }

   public static Map<Direction, VoxelShape> createRotations(VoxelShape northFacingShape) {
      return createRotations(northFacingShape, Direction.values());
   }

   public static VoxelShape rotate(VoxelShape shape, Direction newDir) {
      return rotate(shape, Direction.NORTH, newDir);
   }

   public static VoxelShape rotate(VoxelShape shape, Direction originalDir, Direction newDir) {
      if (originalDir != newDir) {
         VoxelShape[] newShape = new VoxelShape[]{Shapes.empty()};
         shape.forAllBoxes((x, y, z, a, b, c) -> {
            double i = 1.0 - c;
            double j = 1.0 - z;
            newShape[0] = Shapes.or(newShape[0], Shapes.box(Math.min(i, j), y, x, Math.max(i, j), b, a));
         });
         return rotate(newShape[0], originalDir.getClockWise(), newDir);
      } else {
         return shape;
      }
   }

   public static VoxelShape mirror(VoxelShape shape, Direction facingProperty) {
      return mirror(shape, facingProperty.getAxis());
   }

   public static VoxelShape mirror(VoxelShape shape, Mirror mirror) {
      return mirror(shape, mirror == Mirror.FRONT_BACK ? Axis.X : Axis.Z);
   }

   public static VoxelShape mirror(VoxelShape shape, Axis axis) {
      VoxelShape[] newShape = new VoxelShape[]{Shapes.empty()};
      switch (axis) {
         case X:
            shape.forAllBoxes((x, y, z, a, b, c) -> {
               double i = 1.0 - x;
               double j = 1.0 - a;
               newShape[0] = Shapes.or(newShape[0], Shapes.box(Math.min(i, j), y, z, Math.max(i, j), b, c));
            });
            break;
         case Z:
            shape.forAllBoxes((x, y, z, a, b, c) -> {
               double i = 1.0 - z;
               double j = 1.0 - c;
               newShape[0] = Shapes.or(newShape[0], Shapes.box(x, y, Math.min(i, j), a, b, Math.max(i, j)));
            });
            break;
         case Y:
            shape.forAllBoxes((x, y, z, a, b, c) -> {
               double i = 1.0 - y;
               double j = 1.0 - b;
               newShape[0] = Shapes.or(newShape[0], Shapes.box(x, Math.min(i, j), z, a, Math.max(i, j), c));
            });
      }

      return newShape[0];
   }
}
