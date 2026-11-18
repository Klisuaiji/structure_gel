package com.legacy.structure_gel.core.item.building_tool;

import com.google.common.collect.ImmutableMap;
import com.legacy.structure_gel.api.block_entity.IRotatable;
import com.legacy.structure_gel.core.StructureGelMod;
import com.legacy.structure_gel.core.network.SGPacketHandler;
import com.legacy.structure_gel.core.network.bi_directional.UpdateBuildingToolPacket;
import com.legacy.structure_gel.core.util.SGText;
import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.util.random.WeightedEntry.Wrapper;
import net.minecraft.world.Clearable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.neoforge.common.SoundActions;

public abstract class BuildingToolMode {
   private static final String MESSAGE_PRE = "info.structure_gel.building_tool.message.";
   protected static final String SET_POS_KEY = "info.structure_gel.building_tool.message.set_pos";
   protected static final String SIZE_KEY = "info.structure_gel.building_tool.message.size";
   protected static final String CLEAR_POSES_KEY = "info.structure_gel.building_tool.message.clear_poses";
   protected static final String MISSING_POS_KEY = "info.structure_gel.building_tool.message.missing_pos";
   protected static final String POS_UNLOADED_KEY = "info.structure_gel.building_tool.message.pos_unloaded";
   protected static final String SELECT_STATE_KEY = "info.structure_gel.building_tool.message.select_state";
   protected static final String SELECT_PALETTE_KEY = "info.structure_gel.building_tool.message.select_palette";
   protected static final String MISSING_STATE_KEY = "info.structure_gel.building_tool.message.missing_state";
   public static final String SET_REACH_KEY = "info.structure_gel.building_tool.message.set_reach";
   protected static final String PLACE_BLOCKS_KEY = "info.structure_gel.building_tool.message.place_blocks";
   protected static final String PLACE_BLOCK_KEY = "info.structure_gel.building_tool.message.place_block";
   protected static final String PLACE_BLOCKS_WITH_KEY = "info.structure_gel.building_tool.message.place_blocks_with";
   protected static final String REPLACE_BLOCKS_WITH_KEY = "info.structure_gel.building_tool.message.replace_blocks_with";
   protected static final String REPLACE_BLOCKS_KEY = "info.structure_gel.building_tool.message.replace_blocks";
   protected static final String REPLACE_BLOCK_KEY = "info.structure_gel.building_tool.message.replace_block";
   protected static final String CLONE_BLOCKS_KEY = "info.structure_gel.building_tool.message.clone_blocks";
   protected static final String CLIPBOARD_MISSING_KEY = "info.structure_gel.building_tool.message.clipboard_missing";
   protected static final String MOVE_BLOCKS_KEY = "info.structure_gel.building_tool.message.move_blocks";
   protected static final String GRABBED_CORNER_KEY = "info.structure_gel.building_tool.message.grabbed_corner";
   protected static final String PLACED_CORNER_KEY = "info.structure_gel.building_tool.message.placed_corner";
   protected static final String GRABBED_BOUNDS_KEY = "info.structure_gel.building_tool.message.grabbed_bounds";
   protected static final String PLACED_BOUNDS_KEY = "info.structure_gel.building_tool.message.placed_bounds";
   protected static final String SELECT_CORNERS = "item.structure_gel.building_tool.mode.select_corners";
   protected static final String SELECT_POSITIONS = "item.structure_gel.building_tool.mode.select_positions";
   protected static final String LENGTH_KEY = "info.structure_gel.building_tool.message.length";
   public static final Codec<BuildingToolMode> CODEC = ResourceLocation.CODEC.xmap(name -> BuildingToolModes.REGISTRY.get(name), mode -> mode.getName());
   protected final ResourceLocation name;
   private final int modelIndex;
   private final ResourceLocation iconTexture;
   private final Component component;
   private final String descKey;
   private final Map<String, ToolModeProperty<?>> properties;

   public BuildingToolMode(String name, int modelIndex) {
      this(StructureGelMod.locate(name), modelIndex);
   }

   private BuildingToolMode(ResourceLocation name, int modelIndex) {
      this.name = name;
      this.modelIndex = modelIndex;
      this.iconTexture = new ResourceLocation(name.getNamespace(), "textures/item/building_tool_" + name.getPath() + ".png");
      this.component = Component.translatable("item.structure_gel.building_tool.mode." + name.toString());
      this.descKey = "item.structure_gel.building_tool.mode.description." + name.toString();
      List<ToolModeProperty<?>> props = new ArrayList<>();
      this.addProperties(props);
      this.properties = props.stream().collect(ImmutableMap.toImmutableMap(ToolModeProperty::getKey, Function.identity()));
   }

   public boolean isNone() {
      return false;
   }

   public void onSelect(ItemStack stack, Level level, Player player) {
   }

   public void onReleaseCorner(ItemStack stack, Level level, Player player) {
   }

   public abstract boolean hasBlockPalette();

   public boolean targetsSpecificPos() {
      return true;
   }

   public int getSpamDelay() {
      return 0;
   }

   public void onRightClickBlock(Level level, Player player, BlockPos clickedPos, ItemStack stack, Direction clickedFace) {
   }

   public void onRightClickAir(Level level, Player player, BlockPos clickedPos, ItemStack stack) {
   }

   public void onLeftClick(Level level, Player player, BlockPos clickedPos, ItemStack stack, Direction clickedFace) {
   }

   public void onMiddleClickBlock(Level level, Player player, BlockPos clickedPos, ItemStack stack) {
      if (player instanceof ServerPlayer serverPlayer) {
         WeightedRandomList<Wrapper<BlockState>> palette = BuildingToolItem.readPaletteFrom(level, clickedPos);
         BuildingToolItem.setPalette(stack, palette);
         SGPacketHandler.sendToClient(UpdateBuildingToolPacket.builder().toClient().palette(palette).build(), serverPlayer);
         sendMessage(
            player,
            palette.unwrap().size() == 1 ? "info.structure_gel.building_tool.message.select_state" : "info.structure_gel.building_tool.message.select_palette",
            ((BlockState)((Wrapper)palette.unwrap().get(0)).getData()).getBlock().getName().getString()
         );
         playSound(player, SoundEvents.ITEM_PICKUP);
      }
   }

   public void onMiddleClickAir(Level level, Player player, BlockPos clickedPos, ItemStack stack) {
   }

   public boolean onDelete(Level level, Player player, ItemStack stack) {
      return false;
   }

   public void addProperties(List<ToolModeProperty<?>> properties) {
   }

   protected boolean setBlock(Level level, BlockPos pos, BlockState state, boolean causeBlockUpdates, ActionHistory.ActionBuilder actionBuilder) {
      return this.setBlock(level, pos, state, causeBlockUpdates, null, actionBuilder);
   }

   protected boolean setBlock(
      Level level, BlockPos pos, BlockState state, boolean causeBlockUpdates, @Nullable CompoundTag blockEntityTag, ActionHistory.ActionBuilder actionBuilder
   ) {
      return this.setBlock(level, pos, (Function<BlockState, BlockState>)(old -> state), causeBlockUpdates, blockEntityTag, actionBuilder);
   }

   protected boolean setBlock(
      Level level, BlockPos pos, Function<BlockState, BlockState> modifyState, boolean causeBlockUpdates, ActionHistory.ActionBuilder actionBuilder
   ) {
      return this.setBlock(level, pos, modifyState, causeBlockUpdates, null, actionBuilder);
   }

   protected boolean setBlock(
      Level level,
      BlockPos pos,
      Function<BlockState, BlockState> modifyState,
      boolean causeBlockUpdates,
      @Nullable CompoundTag blockEntityTag,
      ActionHistory.ActionBuilder actionBuilder
   ) {
      return this.setBlock(level, pos, modifyState, causeBlockUpdates, blockEntityTag, actionBuilder, Mirror.NONE, Rotation.NONE);
   }

   protected boolean setBlock(
      Level level,
      BlockPos pos,
      Function<BlockState, BlockState> modifyState,
      boolean causeBlockUpdates,
      @Nullable CompoundTag blockEntityTag,
      ActionHistory.ActionBuilder actionBuilder,
      Mirror mirror,
      Rotation rotation
   ) {
      BlockState oldState = level.getBlockState(pos);
      BlockState newState = modifyState.apply(oldState);
      if (newState == null) {
         return false;
      } else {
         BlockEntity oldBlockEntity = level.getBlockEntity(pos);
         CompoundTag oldBETag;
         if (oldBlockEntity != null) {
            oldBETag = oldBlockEntity.saveWithoutMetadata();
            Clearable.tryClear(oldBlockEntity);
            level.setBlock(pos, Blocks.BARRIER.defaultBlockState(), 18);
         } else {
            oldBETag = null;
         }

         int flags = 2;
         if (causeBlockUpdates) {
            flags++;
            if (newState.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF) || newState.hasProperty(BlockStateProperties.BED_PART)) {
               flags += 16;
            }
         } else {
            flags += 16;
         }

         boolean ret = level.setBlock(pos, newState, flags);
         BlockEntity newBlockEntity = level.getBlockEntity(pos);
         if (newBlockEntity != null) {
            if (blockEntityTag != null) {
               newBlockEntity.load(blockEntityTag);
            }

            if (newBlockEntity instanceof IRotatable r) {
               r.transform(mirror, rotation);
            }
         }

         if (ret) {
            actionBuilder.changeBlock(pos, oldState, oldBETag, newState, blockEntityTag, flags);
         }

         return ret;
      }
   }

   protected boolean addEntity(Level level, Entity entity, ActionHistory.ActionBuilder actionBuilder) {
      if (level.addFreshEntity(entity)) {
         actionBuilder.addEntity(entity);
         return true;
      } else {
         return false;
      }
   }

   protected boolean removeEntity(ServerLevel level, UUID uuid, ActionHistory.ActionBuilder actionBuilder) {
      Entity entity = level.getEntity(uuid);
      return entity == null ? false : this.removeEntity(level, entity, actionBuilder);
   }

   protected boolean removeEntity(Level level, Entity entity, ActionHistory.ActionBuilder actionBuilder) {
      actionBuilder.removeEntity(entity);
      entity.discard();
      return true;
   }

   public ResourceLocation getName() {
      return this.name;
   }

   public int modelIndex() {
      return this.modelIndex;
   }

   public ResourceLocation getIconTexture() {
      return this.iconTexture;
   }

   public Component getComponent() {
      return this.component;
   }

   public String getDescKey() {
      return this.descKey;
   }

   public abstract Object[] getDescArgs();

   public Map<String, ToolModeProperty<?>> getProperties() {
      return this.properties;
   }

   public int getReachDistance(ItemStack stack) {
      return 2;
   }

   public int forPosesWithin(Vec3i cornerA, Vec3i cornerB, Function<BlockPos, Boolean> action) {
      int total = 0;
      BoundingBox area = BoundingBox.fromCorners(cornerA, cornerB);

      for (int x = area.minX(); x <= area.maxX(); x++) {
         for (int z = area.minZ(); z <= area.maxZ(); z++) {
            for (int y = area.minY(); y <= area.maxY(); y++) {
               if (action.apply(new BlockPos(x, y, z))) {
                  total++;
               }
            }
         }
      }

      return total;
   }

   protected void setPos(ItemStack stack, int index, BlockPos pos, Player player, boolean message) {
      if (BuildingToolItem.setPos(stack, index, pos) && message) {
         MutableComponent msg = Component.translatable(
            "info.structure_gel.building_tool.message.set_pos",
            new Object[]{
               Component.literal(Integer.toString(index + 1))
                  .withStyle(index == 0 ? SGText.X_LABEL_STYLE : (index == 1 ? SGText.Z_LABEL_STYLE : SGText.Y_LABEL_STYLE)),
               Component.literal(pos.getX() + "").withStyle(SGText.X_LABEL_STYLE),
               Component.literal(pos.getY() + "").withStyle(SGText.Y_LABEL_STYLE),
               Component.literal(pos.getZ() + "").withStyle(SGText.Z_LABEL_STYLE)
            }
         );
         sendMessage(player, msg);
         playSound(player, pos, SoundEvents.WOODEN_BUTTON_CLICK_ON, 1.0F - 0.6F * index);
      }
   }

   public void clearPoses(ItemStack stack, Player player) {
      if (BuildingToolItem.getPos(stack, 0).isPresent() || BuildingToolItem.getPos(stack, 1).isPresent()) {
         sendMessage(player, "info.structure_gel.building_tool.message.clear_poses");
         playSound(player, player.blockPosition(), SoundEvents.ARMOR_STAND_BREAK);
      }

      BuildingToolItem.clearPoses(stack);
   }

   @Nullable
   public Component getHudMessage(ItemStack stack) {
      return null;
   }

   protected static void sendMessage(Player player, String translationKey, Object... args) {
      sendMessage(player, translationKey, Style.EMPTY, args);
   }

   protected static void sendMessage(Player player, String translationKey, Style style, Object... args) {
      sendMessage(player, Component.translatable(translationKey, args).setStyle(style));
   }

   protected static void sendMessage(Player player, Component component) {
      player.displayClientMessage(component, true);
   }

   protected static void playSound(Player player, SoundEvent sound) {
      playSound(player, player.blockPosition(), sound);
   }

   protected static void playSound(Player player, BlockPos pos, SoundEvent sound) {
      playSound(player, pos, sound, 1.0F + player.level().getRandom().nextFloat() * 0.2F);
   }

   protected static void playSound(Player player, BlockPos pos, SoundEvent sound, float pitch) {
      player.level().playSound(null, pos, sound, SoundSource.BLOCKS, 0.8F, pitch);
   }

   protected static void playSound(Player player, BlockState state) {
      SoundEvent sound;
      if (state.getBlock() instanceof LiquidBlock liquidBlock) {
         SoundEvent emptySound = liquidBlock.getFluid().getFluidType().getSound(SoundActions.BUCKET_EMPTY);
         sound = emptySound == null ? SoundEvents.BUCKET_EMPTY : emptySound;
      } else {
         sound = state.getSoundType().getPlaceSound();
      }

      playSound(player, sound);
   }

   @Override
   public String toString() {
      return "BuildingToolMode[" + this.name + "]";
   }

   protected static void sendPlaceMessage(
      Player player, ToolModeProperty.Replace replaceMode, int placedBlocks, WeightedRandomList<Wrapper<BlockState>> palette
   ) {
      int paletteSize = palette.unwrap().size();
      sendPlaceMessage(
         player,
         replaceMode,
         placedBlocks,
         paletteSize,
         paletteSize > 0 ? (BlockState)((Wrapper)palette.unwrap().get(0)).getData() : Blocks.STONE.defaultBlockState()
      );
   }

   protected static void sendPlaceMessage(Player player, ToolModeProperty.Replace replaceMode, int placedBlocks, int paletteSize, BlockState messageState) {
      String message;
      if (replaceMode.usePlaceMessage) {
         message = paletteSize == 1
            ? "info.structure_gel.building_tool.message.place_blocks_with"
            : (placedBlocks > 1 ? "info.structure_gel.building_tool.message.place_blocks" : "info.structure_gel.building_tool.message.place_block");
      } else {
         message = paletteSize == 1
            ? "info.structure_gel.building_tool.message.replace_blocks_with"
            : (placedBlocks > 1 ? "info.structure_gel.building_tool.message.replace_blocks" : "info.structure_gel.building_tool.message.replace_block");
      }

      sendMessage(player, message, placedBlocks, paletteSize == 1 ? messageState.getBlock().getName().getString() : "");
      if (placedBlocks > 0) {
         playSound(player, messageState);
      }
   }

   public abstract static class ForCorners extends BuildingToolMode {
      public ForCorners(String name, int modelIndex) {
         super(name, modelIndex);
      }

      public boolean hasAllCorners() {
         return true;
      }

      @Override
      public void onRightClickBlock(Level level, Player player, BlockPos clickedPos, ItemStack stack, Direction clickedFace) {
         this.setPosition(player, clickedPos, stack, true);
      }

      @Override
      public void onRightClickAir(Level level, Player player, BlockPos clickedPos, ItemStack stack) {
         this.setPosition(player, clickedPos, stack, true);
      }

      public void setPosition(Player player, BlockPos clickedPos, ItemStack stack, boolean message) {
         if (!player.isShiftKeyDown()) {
            this.setPositionA(player, clickedPos, stack, message);
         } else {
            this.setPositionB(player, clickedPos, stack, message);
         }
      }

      public void setPositionA(Player player, BlockPos clickedPos, ItemStack stack, boolean message) {
         this.setPos(stack, 0, clickedPos, player, message);
      }

      public void setPositionB(Player player, BlockPos clickedPos, ItemStack stack, boolean message) {
         this.setPos(stack, 1, clickedPos, player, message);
      }

      @Override
      public void onMiddleClickAir(Level level, Player player, BlockPos clickedPos, ItemStack stack) {
         this.clearPoses(stack, player);
      }

      public boolean requiresSelectionLoaded() {
         return true;
      }

      @Override
      public void onLeftClick(Level level, Player player, BlockPos clickedPos, ItemStack stack, Direction clickedFace) {
         Optional<BlockPos> pos0 = BuildingToolItem.getPos(stack, 0);
         Optional<BlockPos> pos1 = BuildingToolItem.getPos(stack, 1);
         if (pos0.isPresent() && pos1.isPresent()) {
            if (this.requiresSelectionLoaded() && (!level.isLoaded(pos0.get()) || !level.isLoaded(pos1.get()))) {
               sendMessage(player, "info.structure_gel.building_tool.message.pos_unloaded", new Object[0]);
            } else {
               this.performAction(level, player, clickedPos, stack, pos0.get(), pos1.get());
            }
         } else {
            List<String> missingPoses = new ArrayList<>(1);
            if (pos0.isEmpty()) {
               missingPoses.add(Integer.toString(1));
            }

            if (pos1.isEmpty()) {
               missingPoses.add(Integer.toString(2));
            }

            sendMessage(
               player,
               "info.structure_gel.building_tool.message.missing_pos",
               Style.EMPTY.withColor(ChatFormatting.RED),
               new Object[]{"[" + String.join(", ", missingPoses) + "]"}
            );
         }
      }

      protected abstract void performAction(Level var1, Player var2, BlockPos var3, ItemStack var4, BlockPos var5, BlockPos var6);

      @Override
      public Component getHudMessage(ItemStack stack) {
         Optional<BlockPos> p1 = BuildingToolItem.getPos(stack, 0);
         Optional<BlockPos> p2 = BuildingToolItem.getPos(stack, 1);
         if (p1.isPresent() && p2.isPresent()) {
            BoundingBox bounds = BoundingBox.fromCorners((Vec3i)p1.get(), (Vec3i)p2.get());
            return Component.translatable(
               "info.structure_gel.building_tool.message.size",
               new Object[]{
                  Component.literal(bounds.getXSpan() + "").setStyle(SGText.X_LABEL_STYLE),
                  Component.literal(bounds.getYSpan() + "").setStyle(SGText.Y_LABEL_STYLE),
                  Component.literal(bounds.getZSpan() + "").setStyle(SGText.Z_LABEL_STYLE)
               }
            );
         } else {
            return null;
         }
      }
   }
}
