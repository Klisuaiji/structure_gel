package com.legacy.structure_gel.api.block.base;

import com.mojang.serialization.Codec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public interface IStructureGel {
   default boolean spreadHookPre(BlockState state, Level level, BlockPos pos, RandomSource rand) {
      return true;
   }

   default boolean removalHookPre(BlockState state, Level level, BlockPos pos, RandomSource rand) {
      return true;
   }

   default void spreadHookPost(BlockState state, Level level, BlockPos pos, RandomSource rand) {
   }

   default void removalHookPost(BlockState state, Level level, BlockPos pos, RandomSource rand) {
   }

   default boolean checkPlacementHook(Level level, BlockPos spreadPos, int count) {
      return true;
   }

   @Nullable
   default BlockState onHandPlaceHook(BlockPlaceContext context) {
      return null;
   }

   default BlockState getReplacementState() {
      return Blocks.AIR.defaultBlockState();
   }

   enum Behavior implements StringRepresentable {
      DEFAULT("default"),
      PHOTOSENSITIVE("photosensitive"),
      DIAGONAL_SPREAD("diagonal_spread"),
      AXIS_SPREAD("axis_spread"),
      DYNAMIC_SPREAD_DIST("dynamic_spread_dist");

      public static final Codec<IStructureGel.Behavior> CODEC = StringRepresentable.fromEnum(IStructureGel.Behavior::values);
      private final String name;
      private final String translation;

      Behavior(String name) {
         this.name = name;
         this.translation = String.format("info.%s.%s", "structure_gel", name);
      }

      public String getSerializedName() {
         return this.name;
      }

      public String getTranslation() {
         return this.translation;
      }
   }
}
