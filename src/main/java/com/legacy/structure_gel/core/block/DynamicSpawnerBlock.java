package com.legacy.structure_gel.core.block;

import com.legacy.blue_skies.compat.*;
import com.legacy.structure_gel.core.StructureGelMod;
import com.legacy.structure_gel.core.block_entity.DynamicSpawnerBlockEntity;
import com.legacy.structure_gel.core.registry.SGRegistry;
import com.legacy.structure_gel.core.util.SGText;
import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.phys.BlockHitResult;

public class DynamicSpawnerBlock extends BaseEntityBlock {
   public static final MapCodec<DynamicSpawnerBlock> CODEC = simpleCodec(DynamicSpawnerBlock::new);

   public DynamicSpawnerBlock(Properties properties) {
      super(properties);
   }

   protected MapCodec<? extends BaseEntityBlock> codec() {
      return CODEC;
   }

   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new DynamicSpawnerBlockEntity(pos, state);
   }

   public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
      if (level.getBlockEntity(pos) instanceof DynamicSpawnerBlockEntity spawner && player.canUseGameMasterBlocks()) {
         if (level.isClientSide) {
            StructureGelMod.proxy.openDynamicSpawnerScreen(spawner);
         }

         return InteractionResult.sidedSuccess(level.isClientSide);
      } else {
         return InteractionResult.PASS;
      }
   }

   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
      return createTickerHelper(
         type,
         SGRegistry.BlockEntities.DYNAMIC_SPAWNER.get(),
         level.isClientSide ? DynamicSpawnerBlockEntity::clientTick : DynamicSpawnerBlockEntity::serverTick
      );
   }

   public int getExpDrop(BlockState state, LevelReader level, RandomSource rand, BlockPos pos, int fortune, int silktouch) {
      if (level.getBlockEntity(pos) instanceof DynamicSpawnerBlockEntity spawner) {
         DynamicSpawnerBlockEntity.ExpRange range = spawner.getExpRange();
         int min = range.min;
         int max = range.max;
         int r = (max - min) / 2 + 1;
         return min + rand.nextInt(r) + rand.nextInt(r);
      } else {
         return Blocks.SPAWNER.getExpDrop(Blocks.SPAWNER.defaultBlockState(), level, rand, pos, fortune, silktouch);
      }
   }

   public RenderShape getRenderShape(BlockState p_56794_) {
      return RenderShape.MODEL;
   }

   public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
      return new ItemStack((ItemLike)SGRegistry.Blocks.DYNAMIC_SPAWNER.get());
   }

   public void appendHoverText(ItemStack stack, BlockGetter level, List<Component> lore, TooltipFlag showAdvanced) {
      if (Screen.hasShiftDown()) {
         lore.add(Component.translatable("info.structure_gel.dynamic_spawner_description").withStyle(ChatFormatting.GRAY));
      } else {
         lore.add(SGText.applyKeybindFilter(Component.translatable("info.structure_gel.hold_shift")));
         String beTagKey = "BlockEntityTag";
         CompoundTag tag = LegacyNbt.get(stack);
         if (tag != null && tag.contains(beTagKey, 10)) {
            lore.add(SGText.NEW_LINE);
            CompoundTag beTag = tag.getCompound(beTagKey);
            String typeName = beTag.getString("SpawnerID");
            if (!typeName.isBlank()) {
               lore.add(
                  SGText.BULLET_POINT
                     .copy()
                     .withStyle(ChatFormatting.GRAY)
                     .append(SGText.TYPE_LABEL.copy().withStyle(SGText.VALUE_LABEL_STYLE))
                     .append(Component.literal(": " + typeName))
               );
            }

            if (beTag.contains("exp_range", 10)) {
               DynamicSpawnerBlockEntity.ExpRange range = DynamicSpawnerBlockEntity.ExpRange.load(tag.getCompound("exp_range"));
               lore.add(
                  SGText.BULLET_POINT
                     .copy()
                     .withStyle(ChatFormatting.GRAY)
                     .append(Component.translatable("gui.structure_gel.dynamic_spawner.exp_range").withStyle(SGText.VALUE_LABEL_STYLE))
                     .append(Component.literal(": " + range.min + " - " + range.max))
               );
            }
         }
      }
   }
}
