package com.legacy.structure_gel.api.dimension.portal;

import com.legacy.structure_gel.api.block.GelPortalBlock;
import com.legacy.structure_gel.core.util.Internal;
import java.util.Comparator;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.BlockUtil;
import net.minecraft.BlockUtil.FoundRectangle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiManager.Occupancy;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import com.legacy.blue_skies.compat.PortalInfo;
import net.minecraft.world.level.portal.PortalShape;
import net.minecraft.world.phys.Vec3;
import com.legacy.blue_skies.compat.ITeleporter;

public class GelTeleporter implements ITeleporter {
   private final ServerLevel level;
   private final Supplier<ResourceKey<Level>> dimension1;
   private final Supplier<ResourceKey<Level>> dimension2;
   private final Supplier<PoiType> portalPOI;
   private final Supplier<GelPortalBlock> portalBlock;
   private final Supplier<BlockState> frameBlock;
   private final GelTeleporter.ICreatePortalFuncion placementBehavior;

   public GelTeleporter(
      ServerLevel serverLevel,
      Supplier<ResourceKey<Level>> dimension1,
      Supplier<ResourceKey<Level>> dimension2,
      Supplier<PoiType> portalPOI,
      Supplier<GelPortalBlock> portalBlock,
      Supplier<BlockState> frameBlock,
      GelTeleporter.ICreatePortalFuncion placementBehavior
   ) {
      this.level = serverLevel;
      this.dimension1 = dimension1;
      this.dimension2 = dimension2;
      this.portalPOI = portalPOI;
      this.portalBlock = portalBlock;
      this.frameBlock = frameBlock;
      this.placementBehavior = placementBehavior;
   }

   public GelTeleporter(
      ServerLevel serverLevel,
      Supplier<ResourceKey<Level>> dimension1,
      Supplier<ResourceKey<Level>> dimension2,
      Supplier<PoiType> portalPOI,
      Supplier<GelPortalBlock> portalBlock,
      Supplier<BlockState> frameBlock,
      GelTeleporter.CreatePortalBehavior placementBehavior
   ) {
      this(serverLevel, dimension1, dimension2, portalPOI, portalBlock, frameBlock, placementBehavior.get());
   }

   @Internal
   public ResourceKey<Level> getOpposite() {
      return this.level != null && this.level.dimension().location().equals(this.dimension1.get().location()) ? this.dimension2.get() : this.dimension1.get();
   }

   public Supplier<ResourceKey<Level>> getDimension1() {
      return this.dimension1;
   }

   public Supplier<ResourceKey<Level>> getDimension2() {
      return this.dimension2;
   }

   public Supplier<PoiType> getPortalPOI() {
      return this.portalPOI;
   }

   public Supplier<GelPortalBlock> getPortalBlock() {
      return this.portalBlock;
   }

   public Supplier<BlockState> getFrameBlock() {
      return this.frameBlock;
   }

   public GelTeleporter.ICreatePortalFuncion getPlacementBehavior() {
      return this.placementBehavior;
   }

   public ServerLevel getLevel() {
      return this.level;
   }

   public boolean shouldIgnoreBlock(BlockState state, BlockPos pos) {
      return state.is(BlockTags.LEAVES)
         || state.is(BlockTags.LOGS)
         || this.level.isEmptyBlock(pos)
         || state.getCollisionShape(this.level, pos).isEmpty() && !state.liquid();
   }

   public int getDefaultHeight() {
      return 70;
   }

   public Optional<FoundRectangle> findPortalAround(BlockPos startPos, boolean toNether) {
      PoiManager poiManager = this.level.getPoiManager();
      int dist = (int)Math.max(
         DimensionType.getTeleportationScale(this.level.getServer().getLevel(this.getOpposite()).dimensionType(), this.level.dimensionType()) * 16.0, 16.0
      );
      poiManager.ensureLoadedAndValid(this.level, startPos, dist);
      Optional<PoiRecord> optional = poiManager.getInSquare(poiType -> poiType.value() == this.portalPOI.get(), startPos, dist, Occupancy.ANY)
         .filter(poi -> poiManager.getType(poi.getPos().below()).orElse(null) != poi.getPoiType())
         .filter(poi -> this.level.getBlockState(poi.getPos()).hasProperty(BlockStateProperties.HORIZONTAL_AXIS))
         .min(Comparator.<PoiRecord>comparingDouble(poi -> poi.getPos().distSqr(startPos)).thenComparingInt(poi -> poi.getPos().getY()));
      return optional.map(
         poi -> {
            BlockPos blockpos = poi.getPos();
            this.level.getChunkSource().addRegionTicket(TicketType.PORTAL, new ChunkPos(blockpos), 3, blockpos);
            BlockState blockstate = this.level.getBlockState(blockpos);
            return BlockUtil.getLargestRectangleAround(
               blockpos, (Axis)blockstate.getValue(BlockStateProperties.HORIZONTAL_AXIS), 21, Axis.Y, 21, pos -> this.level.getBlockState(pos) == blockstate
            );
         }
      );
   }

   public Optional<FoundRectangle> createPortal(BlockPos startPos, Axis enterAxis) {
      return this.placementBehavior.apply(this, startPos, enterAxis);
   }

   @Nullable
   public PortalInfo getPortalInfo(Entity entity, ServerLevel destLevel, Function<ServerLevel, PortalInfo> defaultPortalInfo) {
      BlockPos scaledPos = this.scalePosition(entity, destLevel);
      BlockPos portalPos = this.getPortalEntrancePos(entity);
      BlockState blockstate = entity.level().getBlockState(portalPos);
      Axis portalAxis;
      Vec3 offset;
      if (blockstate.hasProperty(BlockStateProperties.HORIZONTAL_AXIS)) {
         portalAxis = (Axis)blockstate.getValue(BlockStateProperties.HORIZONTAL_AXIS);
         FoundRectangle motionTpResult = BlockUtil.getLargestRectangleAround(
            portalPos, portalAxis, 21, Axis.Y, 21, bp -> entity.level().getBlockState(bp) == blockstate
         );
         offset = PortalShape.getRelativePosition(motionTpResult, portalAxis, entity.position(), entity.getDimensions(entity.getPose()));
      } else {
         portalAxis = Axis.X;
         offset = new Vec3(0.5, 0.0, 0.0);
      }

      Optional<FoundRectangle> result = this.findPortalAround(scaledPos, false);
      if (entity instanceof ServerPlayer && !result.isPresent()) {
         result = this.createPortal(scaledPos, portalAxis);
      }

      if (!result.isPresent()) {
         return null;
      }

      PortalInfo portalInfo = PortalShape.createPortalInfo(
         destLevel, result.get(), portalAxis, offset, entity, entity.getDeltaMovement(), entity.getYRot(), entity.getXRot()
      );
      return new PortalInfo(
         new Vec3(result.get().minCorner.getX() + 0.5, result.get().minCorner.getY() + 0.05, result.get().minCorner.getZ() + 0.5),
         portalInfo.speed,
         portalInfo.yRot,
         portalInfo.xRot
      );
   }

   protected BlockPos scalePosition(Entity entity, ServerLevel destLevel) {
      WorldBorder worldborder = destLevel.getWorldBorder();
      double minX = Math.max(-2.9999872E7, worldborder.getMinX() + 16.0);
      double minZ = Math.max(-2.9999872E7, worldborder.getMinZ() + 16.0);
      double maxX = Math.min(2.9999872E7, worldborder.getMaxX() - 16.0);
      double maxZ = Math.min(2.9999872E7, worldborder.getMaxZ() - 16.0);
      double scaling = DimensionType.getTeleportationScale(entity.level().dimensionType(), destLevel.dimensionType());
      return BlockPos.containing(Mth.clamp(entity.getX() * scaling, minX, maxX), entity.getY(), Mth.clamp(entity.getZ() * scaling, minZ, maxZ));
   }

   protected BlockPos getPortalEntrancePos(Entity entity) {
      return entity.portalEntrancePos;
   }

   public static Optional<FoundRectangle> createAndFindPortalSurface(GelTeleporter teleporter, BlockPos startPos, Axis enterAxis) {
      ServerLevel serverLevel = teleporter.level;
      int x = startPos.getX();
      int y = serverLevel.getHeight();
      int z = startPos.getZ();
      MutableBlockPos mutablePos = new MutableBlockPos(x, y, z);
      int i = y;
      BlockState state = serverLevel.getBlockState(mutablePos);

      int minHeight;
      for (minHeight = teleporter.level.getMinBuildHeight(); i > minHeight && teleporter.shouldIgnoreBlock(state, mutablePos); i--) {
         state = serverLevel.getBlockState(mutablePos.move(Direction.DOWN));
      }

      if (i <= minHeight) {
         y = teleporter.getDefaultHeight();
      } else {
         y = i + 1;
      }

      BlockState frameState = teleporter.getFrameBlock().get();

      for (int horizontalOffset = -1; horizontalOffset < 3; horizontalOffset++) {
         for (int verticalOffset = -1; verticalOffset < 4; verticalOffset++) {
            if (horizontalOffset == -1 || horizontalOffset == 2 || verticalOffset == -1 || verticalOffset == 3) {
               BlockPos pos2 = new BlockPos(x, y + verticalOffset, z + horizontalOffset);
               serverLevel.setBlock(pos2, frameState, 3);
            }
         }
      }

      BlockState portalState = (BlockState)teleporter.getPortalBlock().get().defaultBlockState().setValue(NetherPortalBlock.AXIS, Axis.Z);

      for (int horizontalOffset = 0; horizontalOffset < 2; horizontalOffset++) {
         for (int verticalOffset = 0; verticalOffset < 3; verticalOffset++) {
            BlockPos pos2 = new BlockPos(x, y + verticalOffset, z + horizontalOffset);
            serverLevel.setBlock(pos2, portalState, 18);
         }
      }

      boolean placePlatform = true;

      label72:
      for (int x1 = -1; x1 < 2; x1++) {
         for (int z1 = 0; z1 < 2; z1++) {
            BlockPos pos2 = new BlockPos(x + x1, y - 1, z + z1);
            BlockState existingState = serverLevel.getBlockState(pos2);
            if (!teleporter.shouldIgnoreBlock(existingState, pos2) && !existingState.liquid() && existingState.getBlock() != frameState.getBlock()) {
               placePlatform = false;
               break label72;
            }
         }
      }

      if (placePlatform) {
         for (int x1 = -1; x1 < 2; x1++) {
            for (int z1 = 0; z1 < 2; z1++) {
               serverLevel.setBlockAndUpdate(new BlockPos(x + x1, y - 1, z + z1), frameState);
            }
         }
      }

      return teleporter.findPortalAround(startPos, false);
   }

   public static Optional<FoundRectangle> createAndFindPortalNether(GelTeleporter teleporter, BlockPos startPos, Axis enterAxis) {
      Direction direction = Direction.get(AxisDirection.POSITIVE, enterAxis);
      double d0 = -1.0;
      BlockPos blockpos = null;
      double d1 = -1.0;
      BlockPos blockpos1 = null;
      WorldBorder worldborder = teleporter.level.getWorldBorder();
      int i = teleporter.level.getHeight() - 1;
      MutableBlockPos blockpos$mutable = startPos.mutable();
      int minHeight = teleporter.level.getMinBuildHeight();

      for (MutableBlockPos blockpos$mutable1 : BlockPos.spiralAround(startPos, 16, Direction.EAST, Direction.SOUTH)) {
         int j = Math.min(i, teleporter.level.getHeight(Types.MOTION_BLOCKING, blockpos$mutable1.getX(), blockpos$mutable1.getZ()));
         if (worldborder.isWithinBounds(blockpos$mutable1) && worldborder.isWithinBounds(blockpos$mutable1.move(direction, 1))) {
            blockpos$mutable1.move(direction.getOpposite(), 1);

            for (int l = j; l >= minHeight; l--) {
               blockpos$mutable1.setY(l);
               if (teleporter.level.isEmptyBlock(blockpos$mutable1)) {
                  int i1 = l;

                  while (l > 0 && teleporter.level.isEmptyBlock(blockpos$mutable1.move(Direction.DOWN))) {
                     l--;
                  }

                  if (l + 4 <= i) {
                     int j1 = i1 - l;
                     if (j1 <= 0 || j1 >= 3) {
                        blockpos$mutable1.setY(l);
                        if (teleporter.checkRegionForPlacement(blockpos$mutable1, blockpos$mutable, direction, 0)) {
                           double d2 = startPos.distSqr(blockpos$mutable1);
                           if (teleporter.checkRegionForPlacement(blockpos$mutable1, blockpos$mutable, direction, -1)
                              && teleporter.checkRegionForPlacement(blockpos$mutable1, blockpos$mutable, direction, 1)
                              && (d0 == -1.0 || d0 > d2)) {
                              d0 = d2;
                              blockpos = blockpos$mutable1.immutable();
                           }

                           if (d0 == -1.0 && (d1 == -1.0 || d1 > d2)) {
                              d1 = d2;
                              blockpos1 = blockpos$mutable1.immutable();
                           }
                        }
                     }
                  }
               }
            }
         }
      }

      if (d0 == -1.0 && d1 != -1.0) {
         blockpos = blockpos1;
         d0 = d1;
      }

      if (d0 == -1.0) {
         blockpos = new BlockPos(startPos.getX(), Mth.clamp(startPos.getY(), 70, teleporter.level.getHeight() - 10), startPos.getZ()).immutable();
         Direction direction1 = direction.getClockWise();
         if (!worldborder.isWithinBounds(blockpos)) {
            return Optional.empty();
         }

         for (int l1 = -1; l1 < 2; l1++) {
            for (int k2 = 0; k2 < 2; k2++) {
               for (int i3 = -1; i3 < 3; i3++) {
                  BlockState blockstate1 = i3 < 0 ? teleporter.frameBlock.get() : Blocks.AIR.defaultBlockState();
                  blockpos$mutable.setWithOffset(
                     blockpos, k2 * direction.getStepX() + l1 * direction1.getStepX(), i3, k2 * direction.getStepZ() + l1 * direction1.getStepZ()
                  );
                  teleporter.level.setBlockAndUpdate(blockpos$mutable, blockstate1);
               }
            }
         }
      }

      for (int k1 = -1; k1 < 3; k1++) {
         for (int i2 = -1; i2 < 4; i2++) {
            if (k1 == -1 || k1 == 2 || i2 == -1 || i2 == 3) {
               blockpos$mutable.setWithOffset(blockpos, k1 * direction.getStepX(), i2, k1 * direction.getStepZ());
               teleporter.level.setBlock(blockpos$mutable, teleporter.frameBlock.get(), 3);
            }
         }
      }

      BlockState blockstate = (BlockState)teleporter.portalBlock.get().defaultBlockState().setValue(NetherPortalBlock.AXIS, enterAxis);

      for (int j2 = 0; j2 < 2; j2++) {
         for (int l2 = 0; l2 < 3; l2++) {
            blockpos$mutable.setWithOffset(blockpos, j2 * direction.getStepX(), l2, j2 * direction.getStepZ());
            teleporter.level.setBlock(blockpos$mutable, blockstate, 18);
         }
      }

      return Optional.of(new FoundRectangle(blockpos.immutable(), 2, 3));
   }

   private boolean checkRegionForPlacement(BlockPos pos, MutableBlockPos mutablePos, Direction facing, int offset) {
      Direction direction = facing.getClockWise();

      for (int i = -1; i < 3; i++) {
         for (int j = -1; j < 4; j++) {
            mutablePos.setWithOffset(pos, facing.getStepX() * i + direction.getStepX() * offset, j, facing.getStepZ() * i + direction.getStepZ() * offset);
            if (j < 0 && !this.level.getBlockState(mutablePos).isSolid()) {
               return false;
            }

            if (j >= 0 && !this.level.isEmptyBlock(mutablePos)) {
               return false;
            }
         }
      }

      return true;
   }

   public enum CreatePortalBehavior {
      NETHER(GelTeleporter::createAndFindPortalNether),
      ON_SURFACE(GelTeleporter::createAndFindPortalSurface);

      private final GelTeleporter.ICreatePortalFuncion function;

      CreatePortalBehavior(GelTeleporter.ICreatePortalFuncion function) {
         this.function = function;
      }

      public GelTeleporter.ICreatePortalFuncion get() {
         return this.function;
      }
   }

   @FunctionalInterface
   public interface ICreatePortalFuncion {
      Optional<FoundRectangle> apply(GelTeleporter var1, BlockPos var2, Axis var3);
   }
}
