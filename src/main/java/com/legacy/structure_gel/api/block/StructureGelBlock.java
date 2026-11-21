package com.legacy.structure_gel.api.block;

import com.google.common.collect.ImmutableList;
import com.legacy.structure_gel.api.block.base.IStructureGel;
import com.legacy.structure_gel.api.item.StructureGelItem;
import com.legacy.structure_gel.core.SGConfig;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.common.util.Lazy;

public class StructureGelBlock extends Block implements IStructureGel {
   public static final MapCodec<StructureGelBlock> CODEC = RecordCodecBuilder.mapCodec(
      instance -> instance.group(IStructureGel.Behavior.CODEC.listOf().fieldOf("behaviors").forGetter(b -> b.behaviors))
         .apply(instance, StructureGelBlock::new)
   );
   public static final IntegerProperty COUNT = IntegerProperty.create("count", 0, 51);
   public final ImmutableList<IStructureGel.Behavior> behaviors;
   private static final Lazy<Set<Item>> ITEMS = Lazy.of(() -> (Set)Util.make(new HashSet(), set -> {
      set.add(Items.DEBUG_STICK);
      set.add(Items.GUNPOWDER);
      BuiltInRegistries.ITEM.stream().filter(item -> item instanceof StructureGelItem).forEach(set::add);
   }));

   public StructureGelBlock(List<IStructureGel.Behavior> behaviors) {
      super(
         Properties.of()
            .pushReaction(PushReaction.DESTROY)
            .mapColor(MapColor.NONE)
            .noCollission()
            .strength(0.0F)
            .lootFrom(() -> Blocks.AIR)
            .noOcclusion()
            .sound(SoundType.SLIME_BLOCK)
            .isValidSpawn((state, reader, pos, entity) -> false)
            .isSuffocating((state, reader, pos) -> false)
            .isViewBlocking((state, reader, pos) -> false)
      );
      this.behaviors = ImmutableList.copyOf(behaviors);
      this.registerDefaultState((BlockState)this.defaultBlockState().setValue(COUNT, 50));
   }

   public StructureGelBlock(IStructureGel.Behavior... behaviors) {
      this(List.of(behaviors));
   }

   public MapCodec<? extends Block> codec() {
      return CODEC;
   }

   public BlockState getStateForPlacement(BlockPlaceContext context) {
      Player player = context.getPlayer();
      if (player != null && player.isCreative()) {
         BlockState hookState = this.onHandPlaceHook(context);
         if (hookState != null) {
            return hookState;
         }

         int count = 50;
         if (player.isShiftKeyDown()) {
            count = 0;
            if (this.behaviors.contains(IStructureGel.Behavior.DYNAMIC_SPREAD_DIST)) {
               count = Math.min(Math.max(50 - context.getItemInHand().getCount(), 0), 50);
            }
         }

         return (BlockState)this.defaultBlockState().setValue(COUNT, count);
      } else {
         return Blocks.AIR.defaultBlockState();
      }
   }

   public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
      level.scheduleTick(pos, this, 2);
   }

   public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource rand) {
      if ((Integer)state.getValue(COUNT) < 50) {
         if (!this.spreadHookPre(state, level, pos, rand)) {
            return;
         }

         for (Direction d : Direction.values()) {
            BlockPos offset = pos.relative(d);
            this.addGel(state, level, offset, (Integer)state.getValue(COUNT) + 1);
            if (this.behaviors.contains(IStructureGel.Behavior.DIAGONAL_SPREAD)) {
               if (d != Direction.UP && d != Direction.DOWN) {
                  this.addGel(state, level, offset.relative(d.getClockWise()), (Integer)state.getValue(COUNT) + 1);
               } else {
                  for (int i = 0; i < 4; i++) {
                     this.addGel(state, level, offset.relative(Direction.from2DDataValue(i)), (Integer)state.getValue(COUNT) + 1);
                  }
               }
            }
         }

         this.setGel(state, level, pos, 50);
         this.spreadHookPost(state, level, pos, rand);
      } else if ((Integer)state.getValue(COUNT) == 51) {
         if (!this.removalHookPre(state, level, pos, rand)) {
            return;
         }

         for (Direction d : Direction.values()) {
            BlockPos offset = pos.relative(d);
            this.removeGel(state, level, offset);
            if (this.behaviors.contains(IStructureGel.Behavior.DIAGONAL_SPREAD)) {
               if (d != Direction.UP && d != Direction.DOWN) {
                  this.removeGel(state, level, offset.relative(d.getClockWise()));
               } else {
                  for (int i = 0; i < 4; i++) {
                     this.removeGel(state, level, offset.relative(Direction.from2DDataValue(i)));
                  }
               }
            }
         }

         level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
         this.removalHookPost(state, level, pos, rand);
      }
   }

   public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
      if (player.isCreative() && player.getItemInHand(hand).getItem() == Items.GUNPOWDER) {
         this.removeGel(state, level, pos);
         return InteractionResult.SUCCESS;
      } else {
         return InteractionResult.FAIL;
      }
   }

   public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
      if (!player.isCreative()) {
         for (Direction d : Direction.values()) {
            BlockPos offset = pos.relative(d);
            if (level.getBlockState(offset).getBlock() == this) {
               this.removeGel(state, level, offset);
            }
         }
      }

      return super.playerWillDestroy(level, pos, state, player);
   }

   public void addGel(BlockState state, Level level, BlockPos pos, int count) {
      if (level.isEmptyBlock(pos) && this.checkAbove(level, pos) && this.checkPlacementHook(level, pos, count)) {
         this.setGel(state, level, pos, count);
      }
   }

   public void removeGel(BlockState state, Level level, BlockPos pos) {
      if (level.getBlockState(pos).getBlock() == this) {
         this.setGel(state, level, pos, 51);
      }
   }

   public void setGel(BlockState state, Level level, BlockPos pos, int count) {
      level.setBlock(pos, (BlockState)state.setValue(COUNT, count), 2);
      level.scheduleTick(pos, level.getBlockState(pos).getBlock(), 2);
   }

   public boolean checkAbove(Level level, BlockPos pos) {
      if (!this.behaviors.contains(IStructureGel.Behavior.PHOTOSENSITIVE)) {
         return true;
      }

      if (!level.canSeeSky(pos)) {
         return true;
      }

      for (int dy = 1; pos.above(dy).getY() < 256; dy++) {
         if (!level.isEmptyBlock(pos.above(dy))) {
            return true;
         }
      }

      return false;
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{COUNT});
   }

   public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return Shapes.empty();
   }

   public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      if (!SGConfig.COMMON.advancedGelBehavior()) {
         return Shapes.block();
      } else {
         return context != CollisionContext.empty()
               && !((Set)ITEMS.get()).stream().anyMatch(context::isHoldingItem)
               && !(
                  context instanceof EntityCollisionContext eContext
                     && eContext.getEntity() instanceof Player player
                     && (player.isShiftKeyDown() || !player.isCreative())
               )
            ? Shapes.empty()
            : Shapes.block();
      }
   }

   public boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
      if (!SGConfig.COMMON.advancedGelBehavior()) {
         return false;
      }

      Player player = context.getPlayer();
      if (player != null) {
         Item item = context.getItemInHand().getItem();
         if (player.isShiftKeyDown() || ((Set)ITEMS.get()).contains(item)) {
            return false;
         }
      }

      return true;
   }

   public void neighborChanged(BlockState state, Level level, BlockPos pos, Block oldBlock, BlockPos neighborPos, boolean flag) {
      if (SGConfig.COMMON.advancedGelBehavior()) {
         BlockState newState = level.getBlockState(neighborPos);
         if (newState.isAir() && newState.getBlock() != oldBlock && !(oldBlock instanceof StructureGelBlock)) {
            level.setBlock(neighborPos, state, 2);
         }
      }
   }

   public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
      return true;
   }

   @OnlyIn(Dist.CLIENT)
   public boolean skipRendering(BlockState state, BlockState adjacentBlockState, Direction side) {
      return adjacentBlockState.getBlock() == this;
   }

   @OnlyIn(Dist.CLIENT)
   public float getShadeBrightness(BlockState state, BlockGetter worldIn, BlockPos pos) {
      return 1.0F;
   }
}
