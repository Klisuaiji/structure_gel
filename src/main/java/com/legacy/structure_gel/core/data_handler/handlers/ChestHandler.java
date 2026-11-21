package com.legacy.structure_gel.core.data_handler.handlers;

import com.legacy.structure_gel.api.block_entity.IRotatable;
import com.legacy.structure_gel.api.data_handler.LootTableAlias;
import com.legacy.structure_gel.api.data_handler.handlers.DataHandler;
import com.legacy.structure_gel.api.data_handler.handlers.ProbabilityDataHandler;
import com.legacy.structure_gel.api.data_handler.parsing.DataMap;
import com.legacy.structure_gel.api.data_handler.parsing.DataParser;
import com.legacy.structure_gel.api.registry.StructureGelRegistries;
import com.legacy.structure_gel.core.registry.SGRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

public class ChestHandler extends ProbabilityDataHandler<ChestHandler> {
   private static final DataParser PARSER = DataParser.of(SGRegistry.DataHandlerTypes.CHEST, parser -> {
      parser.add("loot", BuiltInLootTables.SIMPLE_DUNGEON).setSuggestions(StructureGelRegistries.LOOT_TABLE_ALIAS::keySet, ResourceLocation::toString);
      parser.add("chest", Blocks.CHEST.defaultBlockState()).setDefaultValName("minecraft:chest");
      parser.add("generation_chance", 1.0F, 0.0F, 1.0F);
   });
   private final ResourceLocation lootTable;
   private final BlockState chest;

   public ChestHandler(DataMap data) {
      super(data, "generation_chance");
      this.lootTable = data.get("loot", ResourceLocation.class);
      this.chest = data.get("chest", BlockState.class);
   }

   public static DataParser parser() {
      return PARSER;
   }

   @Override
   protected void handle(DataHandler.Context context) {
      BlockState dataHandler = context.getDataHandlerState();
      WorldGenLevel level = context.getLevel();
      BlockPos pos = context.getBlockPos();
      RandomSource rand = context.getRandom();
      BoundingBox bounds = context.getBounds();
      StructurePiece piece = context.getPiece();
      BlockState chest = this.createState(dataHandler, this.chest);
      ResourceLocation loot = LootTableAlias.getLootTable(this.lootTable);

      try {
         this.createChest(piece, level, bounds, rand, pos, loot, chest);
      } catch (Exception e) {
         level.setBlock(pos, chest, 2);
      }

      BlockEntity blockEntity = level.getBlockEntity(pos);
      if (blockEntity instanceof RandomizableContainerBlockEntity) {
         ((RandomizableContainerBlockEntity)blockEntity).setLootTable(loot, rand.nextLong());
      }

      if (blockEntity instanceof IRotatable r) {
         r.transform(piece.getMirror(), piece.getRotation());
      }
   }
}
