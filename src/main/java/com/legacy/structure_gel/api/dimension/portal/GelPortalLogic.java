package com.legacy.structure_gel.api.dimension.portal;

import com.google.common.collect.ImmutableList;
import com.legacy.structure_gel.api.block.GelPortalBlock;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.block.state.BlockState;

public class GelPortalLogic {
   public final Block frame;
   public final GelPortalBlock portal;
   public final LevelAccessor level;
   public final Axis axis;
   public final Direction rightDir;
   public final Direction leftDir;
   public int portalBlockCount;
   @Nullable
   public BlockPos bottomLeft;
   public int height;
   public int width;
   public List<Block> allowedInsideBlocks = new ArrayList<>();

   private GelPortalLogic(LevelAccessor level, BlockPos pos, Axis axis, Block frame, GelPortalBlock portal, List<Block> allowedInsideBlocks) {
      this.level = level;
      this.axis = axis;
      this.frame = frame;
      this.portal = portal;
      this.allowedInsideBlocks = allowedInsideBlocks;
      if (axis == Axis.X) {
         this.leftDir = Direction.EAST;
         this.rightDir = Direction.WEST;
      } else {
         this.leftDir = Direction.NORTH;
         this.rightDir = Direction.SOUTH;
      }

      BlockPos blockpos = pos;

      while (pos.getY() > blockpos.getY() - 21 && pos.getY() > 0 && this.isValidInsideMaterial(level.getBlockState(pos.below()))) {
         pos = pos.below();
      }

      int i = this.getDistanceUntilEdge(pos, this.leftDir) - 1;
      if (i >= 0) {
         this.bottomLeft = pos.relative(this.leftDir, i);
         this.width = this.getDistanceUntilEdge(this.bottomLeft, this.rightDir);
         if (this.width < 2 || this.width > 21) {
            this.bottomLeft = null;
            this.width = 0;
         }
      }

      if (this.bottomLeft != null) {
         this.height = this.calculatePortalHeight();
      }
   }

   public static boolean fillPortal(Level level, BlockPos pos, GelPortalBlock portal, List<Block> allowedInsideBlocks) {
      GelTeleporter teleporter = portal.getTeleporter(null);
      return level.dimension() != teleporter.getDimension1().get() && level.dimension() != teleporter.getDimension2().get()
         ? false
         : trySpawnPortal(level, pos, portal, allowedInsideBlocks);
   }

   public static boolean trySpawnPortal(Level level, BlockPos pos, GelPortalBlock portalBlock, List<Block> allowedInsideBlocks) {
      GelPortalLogic portal = getPortalFrame(level, pos, portalBlock, allowedInsideBlocks);
      if (portal != null && portal.portalBlockCount == 0) {
         portal.placePortalBlocks();
         return true;
      } else {
         return false;
      }
   }

   @Nullable
   public static GelPortalLogic getPortalFrame(Level level, BlockPos pos, GelPortalBlock portalBlock, List<Block> allowedInsideBlocks) {
      GelPortalLogic portal = getPortalFrame(level, pos, portalBlock, allowedInsideBlocks, Axis.X);
      if (portal == null) {
         portal = getPortalFrame(level, pos, portalBlock, allowedInsideBlocks, Axis.Z);
      }

      return portal;
   }

   @Nullable
   private static GelPortalLogic getPortalFrame(Level level, BlockPos pos, GelPortalBlock portalBlock, List<Block> allowedInsideBlocks, Axis axis) {
      GelPortalLogic portal = new GelPortalLogic(level, pos, axis, portalBlock.getFrameBlock().get().getBlock(), portalBlock, allowedInsideBlocks);
      return portal.isValidSize() ? portal : null;
   }

   public static boolean isPortalComplete(LevelAccessor level, BlockPos pos, Axis axis, Block frame, GelPortalBlock portal) {
      GelPortalLogic portalSize = new GelPortalLogic(level, pos, axis, frame, portal, ImmutableList.of());
      return portalSize.isValidSize() && portalSize.hasAllPortalBlocks();
   }

   private boolean isValidSize() {
      return this.bottomLeft != null && this.width >= 2 && this.width <= 21 && this.height >= 3 && this.height <= 21;
   }

   private boolean hasAllPortalBlocks() {
      return this.portalBlockCount >= this.width * this.height;
   }

   private int getDistanceUntilEdge(BlockPos pos, Direction direction) {
      int i;
      for (i = 0; i < 22; i++) {
         BlockPos blockpos = pos.relative(direction, i);
         if (!this.isValidInsideMaterial(this.level.getBlockState(blockpos)) || this.level.getBlockState(blockpos.below()).getBlock() != this.frame) {
            break;
         }
      }

      Block block = this.level.getBlockState(pos.relative(direction, i)).getBlock();
      return block == this.frame ? i : 0;
   }

   private int calculatePortalHeight() {
      label56:
      for (this.height = 0; this.height < 21; this.height++) {
         for (int i = 0; i < this.width; i++) {
            BlockPos blockpos = this.bottomLeft.relative(this.rightDir, i).above(this.height);
            BlockState blockstate = this.level.getBlockState(blockpos);
            if (!this.isValidInsideMaterial(blockstate)) {
               break label56;
            }

            Block block = blockstate.getBlock();
            if (block == this.portal) {
               this.portalBlockCount++;
            }

            if (i == 0) {
               block = this.level.getBlockState(blockpos.relative(this.leftDir)).getBlock();
               if (block != this.frame) {
                  break label56;
               }
            } else if (i == this.width - 1) {
               block = this.level.getBlockState(blockpos.relative(this.rightDir)).getBlock();
               if (block != this.frame) {
                  break label56;
               }
            }
         }
      }

      for (int j = 0; j < this.width; j++) {
         if (this.level.getBlockState(this.bottomLeft.relative(this.rightDir, j).above(this.height)).getBlock() != this.frame) {
            this.height = 0;
            break;
         }
      }

      if (this.height <= 21 && this.height >= 3) {
         return this.height;
      } else {
         this.bottomLeft = null;
         this.width = 0;
         this.height = 0;
         return 0;
      }
   }

   private boolean isValidInsideMaterial(BlockState state) {
      Block block = state.getBlock();
      return state.isAir() || block == this.portal || this.allowedInsideBlocks.contains(block);
   }

   private void placePortalBlocks() {
      for (int i = 0; i < this.width; i++) {
         BlockPos blockpos = this.bottomLeft.relative(this.rightDir, i);

         for (int j = 0; j < this.height; j++) {
            this.level.setBlock(blockpos.above(j), (BlockState)this.portal.defaultBlockState().setValue(NetherPortalBlock.AXIS, this.axis), 18);
         }
      }
   }
}
