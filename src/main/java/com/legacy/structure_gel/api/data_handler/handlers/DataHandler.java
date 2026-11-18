package com.legacy.structure_gel.api.data_handler.handlers;

import com.legacy.structure_gel.api.data_handler.parsing.DataMap;
import com.legacy.structure_gel.core.SGAccessor;
import com.legacy.structure_gel.core.StructureGelMod;
import com.legacy.structure_gel.core.block.DataHandlerBlock;
import com.legacy.structure_gel.core.block_entity.DataHandlerBlockEntity;
import com.legacy.structure_gel.core.util.Internal;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.phys.Vec3;

public abstract class DataHandler<T extends DataHandler<T>> {
   public DataHandler(DataMap data) {
   }

   protected float getProbability() {
      return 1.0F;
   }

   protected abstract void handle(DataHandler.Context var1);

   protected BlockState createState(BlockState dataHandler, BlockState state) {
      var facing = (Direction & Comparable)dataHandler.getValue(BlockStateProperties.FACING);
      BlockState newState = this.mergeProperty(facing, state, BlockStateProperties.FACING);
      newState = this.mergeProperty(facing, newState, BlockStateProperties.HORIZONTAL_FACING);
      newState = this.mergeProperty(facing, newState, BlockStateProperties.FACING_HOPPER);
      Axis axis = facing.getAxis();
      newState = this.mergeProperty(axis, newState, BlockStateProperties.AXIS);
      newState = this.mergeProperty(axis, newState, BlockStateProperties.HORIZONTAL_AXIS);
      boolean waterlogged = (Boolean)dataHandler.getValue(BlockStateProperties.WATERLOGGED);
      return this.mergeProperty(waterlogged, newState, BlockStateProperties.WATERLOGGED);
   }

   protected boolean createChest(
      StructurePiece piece,
      ServerLevelAccessor level,
      BoundingBox bounds,
      RandomSource rand,
      BlockPos pos,
      ResourceLocation lootTable,
      @Nullable BlockState chestState
   ) {
      return SGAccessor.STRUCTURE_PIECE_CREATE_CHEST.invoke(piece, level, bounds, rand, pos, lootTable, chestState);
   }

   protected <B extends Comparable<B>> BlockState mergeProperty(B valueToMerge, BlockState mergeInto, Property<B> property) {
      return mergeInto.hasProperty(property) && property.getPossibleValues().contains(valueToMerge)
         ? (BlockState)mergeInto.setValue(property, valueToMerge)
         : mergeInto;
   }

   @Internal
   public static void process(
      BlockState dataHandler,
      CompoundTag tag,
      BlockPos pos,
      WorldGenLevel level,
      RandomSource rand,
      BoundingBox bounds,
      StructurePiece piece,
      boolean alwaysGenerate
   ) {
      Optional<DataHandlerBlockEntity.RawHandler> opHandler = DataHandlerBlockEntity.loadAllHandlers(tag).getRandom(rand);
      if (opHandler.isPresent()) {
         DataHandler<?> handler = opHandler.get().tryBake(pos, level);
         if (handler != null) {
            try {
               DataHandler.Context context = new DataHandler.Context(
                  dataHandler,
                  pos,
                  Vec3.ZERO,
                  level,
                  rand,
                  bounds,
                  piece,
                  DataHandlerBlockEntity.loadOffset(tag),
                  DataHandlerBlockEntity.loadUseGravity(tag),
                  DataHandlerBlockEntity.loadMarkPostProcessing(tag)
               );
               handler.process(context, alwaysGenerate);
            } catch (Exception var11) {
               StructureGelMod.LOGGER.error("Failed to process DataHandlerBlockEntity", var11);
               var11.printStackTrace();
            }
         }
      }
   }

   @Internal
   public void process(DataHandler.Context context, boolean alwaysGenerate) {
      WorldGenLevel level = context.getLevel();
      BlockPos handlerPos = context.getDataHandlerPos();
      BlockState dataHandler = context.getDataHandlerState();
      RandomSource rand = context.getRandom();
      level.setBlock(handlerPos, dataHandler.getValue(DataHandlerBlock.WATERLOGGED) ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState(), 18);
      float chance = this.getProbability();
      if (alwaysGenerate || chance >= 1.0F || rand.nextFloat() < chance) {
         Vec3 position = Vec3.atBottomCenterOf(handlerPos)
            .add(
               offsetPosition(context.offset, (Direction)dataHandler.getValue(DataHandlerBlock.FACING), (Mirror)dataHandler.getValue(DataHandlerBlock.MIRROR))
            );
         if (context.useGravity) {
            position = new Vec3(position.x(), level.getHeight(Types.OCEAN_FLOOR, (int)Math.floor(position.x()), (int)Math.floor(position.z())), position.z());
         }

         context.position = position;
         this.handle(context);
         if (context.markPostProcessing) {
            BlockPos blockPos = BlockPos.containing(context.position);
            ChunkPos chunkPos = new ChunkPos(blockPos);
            if (level.hasChunk(chunkPos.x, chunkPos.z) && level.getChunk(chunkPos.x, chunkPos.z) instanceof ProtoChunk chunk) {
               chunk.markPosForPostprocessing(blockPos);
            }
         }
      }
   }

   public static Vec3 offsetPosition(Vec3 facingOffset, Direction facing, Mirror mirror) {
      double left = -facingOffset.x;
      double up = facingOffset.y;
      double forward = -facingOffset.z;
      double dx = 0.0;
      double dy = 0.0;
      double dz = 0.0;
      if (mirror == Mirror.FRONT_BACK) {
         if (facing.getAxis() == Axis.Y) {
            up = -up;
         } else {
            left = -left;
         }
      }

      if (mirror == Mirror.LEFT_RIGHT) {
         left = -left;
      }

      if (facing == Direction.NORTH) {
         dx = left;
         dy = up;
         dz = forward;
      }

      if (facing == Direction.EAST) {
         dx = -forward;
         dy = up;
         dz = left;
      }

      if (facing == Direction.SOUTH) {
         dx = -left;
         dy = up;
         dz = -forward;
      }

      if (facing == Direction.WEST) {
         dx = forward;
         dy = up;
         dz = -left;
      }

      if (facing == Direction.UP) {
         dx = left;
         dy = -forward;
         dz = up;
      }

      if (facing == Direction.DOWN) {
         dx = left;
         dy = forward;
         dz = -up;
      }

      return new Vec3(dx, dy, dz);
   }

   public static final class Context {
      private final BlockState dataHandler;
      private final BlockPos dataHandlerPos;
      private Vec3 position;
      private final WorldGenLevel level;
      private final RandomSource random;
      private final BoundingBox bounds;
      private final StructurePiece piece;
      private final Vec3 offset;
      private final boolean useGravity;
      private final boolean markPostProcessing;

      private Context(
         BlockState dataHandler,
         BlockPos dataHandlerPos,
         Vec3 position,
         WorldGenLevel level,
         RandomSource rand,
         BoundingBox bounds,
         StructurePiece piece,
         Vec3 offset,
         boolean useGravity,
         boolean markPostProcessing
      ) {
         this.dataHandler = dataHandler;
         this.dataHandlerPos = dataHandlerPos;
         this.position = position;
         this.level = level;
         this.random = rand;
         this.bounds = bounds;
         this.piece = piece;
         this.offset = offset;
         this.useGravity = useGravity;
         this.markPostProcessing = markPostProcessing;
      }

      public BlockState getDataHandlerState() {
         return this.dataHandler;
      }

      public BlockPos getDataHandlerPos() {
         return this.dataHandlerPos;
      }

      public Vec3 getPos() {
         return this.position;
      }

      public BlockPos getBlockPos() {
         return BlockPos.containing(this.position);
      }

      public WorldGenLevel getLevel() {
         return this.level;
      }

      public RandomSource getRandom() {
         return this.random;
      }

      public BoundingBox getBounds() {
         return this.bounds;
      }

      public StructurePiece getPiece() {
         return this.piece;
      }
   }
}
