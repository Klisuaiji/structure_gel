package com.legacy.structure_gel.core.block;

import com.legacy.blue_skies.compat.*;
import com.legacy.structure_gel.core.StructureGelMod;
import com.legacy.structure_gel.core.block_entity.DataHandlerBlockEntity;
import com.legacy.structure_gel.core.util.SGText;
import com.mojang.serialization.MapCodec;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.GameMasterBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

public class DataHandlerBlock extends Block implements SimpleWaterloggedBlock, EntityBlock, GameMasterBlock {
   public static final MapCodec<DataHandlerBlock> CODEC = simpleCodec(DataHandlerBlock::new);
   public static final DirectionProperty FACING = BlockStateProperties.FACING;
   public static final EnumProperty<Mirror> MIRROR = EnumProperty.create("mirror", Mirror.class);
   public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
   public static final BooleanProperty CONNECT_TO_BLOCKS = BooleanProperty.create("connect_to_blocks");

   public DataHandlerBlock(Properties properties) {
      super(properties);
      this.registerDefaultState(
         (BlockState)((BlockState)((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue(MIRROR, Mirror.NONE))
                  .setValue(FACING, Direction.NORTH))
               .setValue(WATERLOGGED, false))
            .setValue(CONNECT_TO_BLOCKS, true)
      );
   }

   protected MapCodec<? extends Block> codec() {
      return CODEC;
   }

   @Deprecated
   public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
      boolean copyStateTag = false;
      if (FMLEnvironment.dist == Dist.CLIENT && level.isClientSide()) {
         copyStateTag = Screen.hasControlDown();
      }

      return this.getCloneItemStack(level, pos, state, copyStateTag);
   }

   public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean copyStateTag) {
      ItemStack stack = new ItemStack(this);
      if (copyStateTag) {
         CompoundTag stateTag = new CompoundTag();

         for (Property prop : List.of(CONNECT_TO_BLOCKS, WATERLOGGED)) {
            BlockState defaultState = state.getBlock().defaultBlockState();
            Comparable val = state.getValue(prop);
            if (!defaultState.getValue(prop).equals(val)) {
               stateTag.putString(prop.getName(), prop.getName(val));
            }
         }

         if (!stateTag.isEmpty()) {
            stack.addTagElement("BlockStateTag", stateTag);
         }
      }

      return stack;
   }

   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new DataHandlerBlockEntity(pos, state);
   }

   public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
      if (level.getBlockEntity(pos) instanceof DataHandlerBlockEntity dataHandler && player.canUseGameMasterBlocks()) {
         boolean waterlogged = (Boolean)state.getValue(WATERLOGGED);
         Item heldItem = player.getMainHandItem().getItem();
         if ((heldItem != Items.WATER_BUCKET || waterlogged) && (heldItem != Items.BUCKET || !waterlogged)) {
            if (level.isClientSide) {
               StructureGelMod.proxy.openDataHandlerScreen(dataHandler);
            }

            return InteractionResult.sidedSuccess(level.isClientSide);
         } else {
            return InteractionResult.PASS;
         }
      } else {
         return InteractionResult.PASS;
      }
   }

   public BlockState getStateForPlacement(BlockPlaceContext context) {
      FluidState fluidState = context.getLevel().getFluidState(context.getClickedPos());
      return (BlockState)((BlockState)this.defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite()))
         .setValue(WATERLOGGED, fluidState.getType() == Fluids.WATER);
   }

   public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
      if (stack.hasCustomHoverName() && level.getBlockEntity(pos) instanceof DataHandlerBlockEntity dataHandler) {
         dataHandler.setCustomName(stack.getHoverName());
      }
   }

   public FluidState getFluidState(BlockState state) {
      return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(true) : Fluids.EMPTY.defaultFluidState();
   }

   public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
      if ((Boolean)state.getValue(WATERLOGGED)) {
         level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
      }

      return state;
   }

   public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
      return state.getValue(WATERLOGGED) ? Shapes.empty() : Shapes.block();
   }

   public BlockState rotate(BlockState state, Rotation rotation) {
      return (BlockState)state.setValue(FACING, rotation.rotate((Direction)state.getValue(FACING)));
   }

   public BlockState mirror(BlockState state, Mirror mirror) {
      return (BlockState)this.rotate(state, mirror.getRotation((Direction)state.getValue(FACING))).setValue(MIRROR, mirror);
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{MIRROR, FACING, WATERLOGGED, CONNECT_TO_BLOCKS});
   }

   public void appendHoverText(ItemStack stack, BlockGetter level, List<Component> lore, TooltipFlag showAdvanced) {
      if (Screen.hasShiftDown()) {
         lore.add(Component.translatable("info.structure_gel.data_handler_description").withStyle(ChatFormatting.GRAY));
      } else {
         lore.add(SGText.applyKeybindFilter(Component.translatable("info.structure_gel.hold_shift")));
         String beTagKey = "BlockEntityTag";
         CompoundTag tag = LegacyNbt.get(stack);
         if (tag != null && tag.contains(beTagKey, 10)) {
            CompoundTag beTag = tag.getCompound(beTagKey);
            if (beTag.contains("handlers", 9)) {
               lore.add(SGText.NEW_LINE);
               List<DataHandlerBlockEntity.RawHandler> handlers = DataHandlerBlockEntity.loadAllHandlers(beTag).unwrap();
               int size = handlers.size();

               for (int i = 0; i < size; i++) {
                  if (i > 0) {
                     lore.add(SGText.NEW_LINE);
                  }

                  DataHandlerBlockEntity.RawHandler handler = handlers.get(i);
                  ResourceLocation type = handler.typeName();
                  if (!type.getPath().isEmpty()) {
                     lore.add(Component.literal(type.toString()).withStyle(ChatFormatting.GRAY));
                     LinkedHashMap<String, String> dataEntries = handler.dataEntries();
                     if (size > 1) {
                        lore.add(
                           SGText.BULLET_POINT
                              .copy()
                              .withStyle(ChatFormatting.GRAY)
                              .append(Component.translatable("info.structure_gel.building_tool.property.weight").withStyle(SGText.VALUE_LABEL_STYLE))
                              .append(Component.literal(": " + handler.getWeight().asInt()))
                        );
                     }

                     for (Entry<String, String> entry : dataEntries.entrySet()) {
                        String key = entry.getKey();
                        String translationKey = "gui.structure_gel.data_handler." + type + "." + key + ".label";
                        MutableComponent propertyLabel = Component.translatable(translationKey);
                        if (propertyLabel.getString().equals(translationKey)) {
                           propertyLabel = Component.literal(key);
                        }

                        String value = entry.getValue();
                        if (!value.isEmpty()) {
                           lore.add(
                              SGText.BULLET_POINT
                                 .copy()
                                 .withStyle(ChatFormatting.GRAY)
                                 .append(propertyLabel.withStyle(SGText.VALUE_LABEL_STYLE))
                                 .append(Component.literal(": " + value))
                           );
                        }
                     }
                  }
               }
            }
         }
      }
   }
}
