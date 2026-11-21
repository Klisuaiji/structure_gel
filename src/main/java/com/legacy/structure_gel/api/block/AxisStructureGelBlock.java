package com.legacy.structure_gel.api.block;

import com.legacy.structure_gel.api.block.base.IStructureGel;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;

public class AxisStructureGelBlock extends StructureGelBlock {
   public static final MapCodec<AxisStructureGelBlock> CODEC = RecordCodecBuilder.mapCodec(
      instance -> instance.group(IStructureGel.Behavior.CODEC.listOf().fieldOf("behaviors").forGetter(b -> b.behaviors))
         .apply(instance, AxisStructureGelBlock::new)
   );
   public static final EnumProperty<Axis> AXIS = BlockStateProperties.AXIS;

   public AxisStructureGelBlock(List<IStructureGel.Behavior> behaviors) {
      super(behaviors);
   }

   public AxisStructureGelBlock(IStructureGel.Behavior... behaviors) {
      super(behaviors);
   }

   @Override
   public MapCodec<? extends Block> codec() {
      return CODEC;
   }

   @Override
   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      super.createBlockStateDefinition(builder);
      builder.add(new Property[]{AXIS});
   }

   @Override
   public BlockState onHandPlaceHook(BlockPlaceContext context) {
      Player player = context.getPlayer();
      return (BlockState)((BlockState)this.defaultBlockState().setValue(COUNT, player != null && player.isShiftKeyDown() ? 0 : 50))
         .setValue(AXIS, context.getNearestLookingDirection().getAxis());
   }

   @Override
   public boolean spreadHookPre(BlockState state, Level level, BlockPos pos, RandomSource rand) {
      if (this.behaviors.contains(IStructureGel.Behavior.AXIS_SPREAD)) {
         for (Direction d : Direction.values()) {
            if (d.getAxis() != state.getValue(AXIS)) {
               BlockPos offset = pos.relative(d);
               this.addGel(state, level, offset, (Integer)state.getValue(COUNT) + 1);
            }
         }

         this.setGel(state, level, pos, 50);
         return false;
      } else {
         return true;
      }
   }
}
