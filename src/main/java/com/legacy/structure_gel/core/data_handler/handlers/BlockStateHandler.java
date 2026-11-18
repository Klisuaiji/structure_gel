package com.legacy.structure_gel.core.data_handler.handlers;

import com.legacy.structure_gel.api.block_entity.IRotatable;
import com.legacy.structure_gel.api.data_handler.handlers.DataHandler;
import com.legacy.structure_gel.api.data_handler.handlers.ProbabilityDataHandler;
import com.legacy.structure_gel.api.data_handler.parsing.DataMap;
import com.legacy.structure_gel.api.data_handler.parsing.DataParser;
import com.legacy.structure_gel.core.registry.SGRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.StructurePiece;

public class BlockStateHandler extends ProbabilityDataHandler<BlockStateHandler> {
   private static final DataParser PARSER = DataParser.of(SGRegistry.DataHandlerTypes.BLOCK_STATE, parser -> {
      parser.add("block_state", Blocks.AIR.defaultBlockState());
      parser.add("tag", new CompoundTag());
      parser.add("generation_chance", 1.0F, 0.0F, 1.0F);
   });
   private final BlockState blockState;
   private final CompoundTag tag;

   public BlockStateHandler(DataMap data) {
      super(data, "generation_chance");
      this.blockState = data.get("block_state", BlockState.class);
      this.tag = data.get("tag", CompoundTag.class);
   }

   public static DataParser parser() {
      return PARSER;
   }

   @Override
   protected void handle(DataHandler.Context context) {
      WorldGenLevel level = context.getLevel();
      BlockState dataHandler = context.getDataHandlerState();
      BlockPos pos = context.getBlockPos();
      StructurePiece piece = context.getPiece();
      BlockState state = this.createState(dataHandler, this.blockState);
      level.setBlock(pos, state, 18);
      BlockEntity blockEntity = level.getBlockEntity(pos);
      if (blockEntity != null) {
         blockEntity.load(this.tag);
         if (blockEntity instanceof IRotatable r) {
            r.transform(piece.getMirror(), piece.getRotation());
         }

         blockEntity.setChanged();
      }

      if (level instanceof ServerLevel) {
         BlockState updatedState = level.getBlockState(pos);
         ((ServerLevel)level).sendBlockUpdated(pos, updatedState, updatedState, 3);
      }
   }
}
