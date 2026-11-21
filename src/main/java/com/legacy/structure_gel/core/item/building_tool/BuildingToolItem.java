package com.legacy.structure_gel.core.item.building_tool;

import com.legacy.blue_skies.compat.*;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.ImmutableMultimap.Builder;
import com.legacy.structure_gel.core.StructureGelMod;
import com.legacy.structure_gel.core.capability.level.BuildingToolPlayerData;
import com.legacy.structure_gel.core.client.ClientProxy;
import com.legacy.structure_gel.core.network.SGPacketHandler;
import com.legacy.structure_gel.core.network.bi_directional.UpdateBuildingToolPacket;
import com.legacy.structure_gel.core.network.c_to_s.ActionHistoryPacket;
import com.legacy.structure_gel.core.network.c_to_s.LeftClickBuildingToolPacket;
import com.legacy.structure_gel.core.network.c_to_s.MiddleClickBuildingToolPacket;
import com.legacy.structure_gel.core.network.c_to_s.ProcessDeletePacket;
import com.legacy.structure_gel.core.network.c_to_s.RequestClipboardPacket;
import com.legacy.structure_gel.core.network.c_to_s.RightClickBuildingToolPacket;
import com.legacy.structure_gel.core.registry.SGRegistry;
import com.legacy.structure_gel.core.util.SGText;
import com.mojang.datafixers.util.Pair;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.function.BiFunction;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.util.random.WeightedRandom;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.util.random.WeightedEntry.Wrapper;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SolidBucketItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.InputEvent.Key;
import net.neoforged.neoforge.client.event.InputEvent.MouseScrollingEvent;
import net.neoforged.neoforge.client.event.InputEvent.MouseButton.Post;
import net.neoforged.neoforge.client.event.InputEvent.MouseButton.Pre;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.common.util.Lazy;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.LeftClickBlock;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.LeftClickEmpty;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickItem;

public final class BuildingToolItem extends Item {
   public static final String MODE_KEY = "tool_mode";
   public static final String POSES_KEY = "poses";
   public static final String PALETTE_KEY = "pallete";
   public static final String STATE_KEY = "state";
   public static final String WEIGHT_KEY = "weight";
   public static final String PROPERTIES_KEY = "properties";
   public static final String CORNER_KEY = "selected_corner";
   public static final String CORNER_DIST_KEY = "selected_corner_distance";
   public static final String MOVE_BOUNDS_KEY = "move_bounds";
   public static final String CAPTURED_BLOCKS_KEY = "captured_blocks";
   public static final String REACH_DISTANCE_MODIFIER_KEY = "reach_distance";
   public static final String CAUSE_BLOCK_UPDATES_KEY = "cause_block_updates";
   public static final UUID BLOCK_REACH_UUID = UUID.fromString("d247f451-91f5-4819-8bc4-c3ad0baf94fe");
   public static final UUID ENTITY_REACH_UUID = UUID.fromString("197c956e-4243-11ee-be56-0242ac120002");
   private final BiFunction<BuildingToolMode, String, Component> nameCache = Util.memoize(
      (mode, name) -> Component.literal(name).append(Component.literal(": ")).append(mode.getComponent())
   );
   public static int leftClickHoldTime = 0;
   public static int rightClickHoldTime = 0;
   public static final int CLICK_HOLD_TIME = 5;
   private static BlockPos lastHitPos = BlockPos.ZERO;
   private static long lastActionTime = 0L;
   private static final Component NO_PERMISSION = Component.translatable("info.structure_gel.building_tool.message.no_permission")
      .withStyle(ChatFormatting.RED);
   private static final String INFO_NAME = "item.structure_gel.building_tool.info.";
   public static final String BLOCK_PALETTE_NAME = "info.structure_gel.building_tool.block_palette";
   public static final String BLOCK_PALETTE_TOOLTIP = StructureGelMod.locate("block_palette_tooltip").toString();
   private static BoundingBox lastClipboardRequestBounds = BoundingBox.infinite();
   private static long lastClipboardRequestTime = 0L;

   @OnlyIn(Dist.CLIENT)
   private void clientInit() {
      IEventBus bus = NeoForge.EVENT_BUS;
      bus.addListener(
         LeftClickEmpty.class,
         event -> this.onLeftClickEvent(event, Minecraft.getInstance().hitResult instanceof BlockHitResult blockHit ? blockHit.getBlockPos() : null)
      );
      bus.addListener(LeftClickBlock.class, event -> this.onLeftClickEvent(event, event.getPos()));
      bus.addListener(
         RightClickItem.class,
         event -> this.onRightClickEvent(event, Minecraft.getInstance().hitResult instanceof BlockHitResult blockHit ? blockHit.getBlockPos() : null)
      );
      bus.addListener(RightClickBlock.class, event -> this.onRightClickEvent(event, event.getPos()));
      bus.addListener(Key.class, event -> this.processKeyPress(event));
      bus.addListener(Pre.class, event -> this.interceptMousePress(event));
      bus.addListener(Post.class, event -> this.processMousePress(event));
      bus.addListener(MouseScrollingEvent.class, event -> this.onMouseScroll(event));
      bus.addListener(PlayerTickEvent.Post.class, event -> this.tick(event));
      bus.addListener(ClientTickEvent.Post.class, event -> this.clientTick(event));
      bus.addListener(RenderFrameEvent.Post.class, event -> this.renderTick(event));
   }

   @Nullable
   public static Pair<InteractionHand, ItemStack> getBuildingTool(Player player) {
      ItemStack stack = player.getMainHandItem();
      if (stack.is(SGRegistry.Items.BUILDING_TOOL.get())) {
         return Pair.of(InteractionHand.MAIN_HAND, stack);
      }

      stack = player.getOffhandItem();
      return stack.is(SGRegistry.Items.BUILDING_TOOL.get()) ? Pair.of(InteractionHand.OFF_HAND, stack) : null;
   }

   public BuildingToolItem(Properties properties) {
      super(properties);
      if (FMLEnvironment.dist == Dist.CLIENT) {
         this.clientInit();
      }
   }

   public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
      if (slot != EquipmentSlot.MAINHAND && slot != EquipmentSlot.OFFHAND) {
         return ImmutableMultimap.of();
      }

      int reach = getReachDistanceModifier(stack) + getMode(stack).getReachDistance(stack);
      Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
      builder.put((Attribute)NeoForgeMod.BLOCK_REACH.value(), LegacyAttrs.modifier(BLOCK_REACH_UUID, "building_tool_block_reach", reach, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE));
      builder.put(
         (Attribute)NeoForgeMod.ENTITY_REACH.value(), LegacyAttrs.modifier(ENTITY_REACH_UUID, "building_tool_entity_reach", reach, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE)
      );
      return builder.build();
   }

   public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, Player player) {
      return false;
   }

   @OnlyIn(Dist.CLIENT)
   public void processKeyPress(Key event) {
      Minecraft mc = Minecraft.getInstance();
      int action = event.getAction();
      if (action == 1 || action == 2) {
         if (mc.player != null) {
            Pair<InteractionHand, ItemStack> tool = getBuildingTool(mc.player);
            if (tool != null) {
               InteractionHand hand = (InteractionHand)tool.getFirst();
               ItemStack stack = (ItemStack)tool.getSecond();
               this.processModdedKeyMapping(hand, stack, event);
               return;
            }
         }

         this.voidModdedKeyMapping();
      }
   }

   @OnlyIn(Dist.CLIENT)
   public void interceptMousePress(Pre event) {
      Minecraft mc = Minecraft.getInstance();
      int action = event.getAction();
      if ((action == 1 || action == 2) && mc.player != null) {
         Pair<InteractionHand, ItemStack> tool = getBuildingTool(mc.player);
         if (tool != null) {
            InteractionHand hand = (InteractionHand)tool.getFirst();
            ItemStack stack = (ItemStack)tool.getSecond();
            if (event.getButton() == mc.options.keyPickItem.getKey().getValue() && mc.screen == null && this.hasPermission(stack, mc.player)) {
               BlockPos hitPos = mc.hitResult instanceof BlockHitResult blockHit ? blockHit.getBlockPos() : null;
               if (hitPos != null) {
                  this.onMiddleClick(stack, mc.player, hitPos);
                  mc.player.swing(hand);
                  SGPacketHandler.sendToServer(new MiddleClickBuildingToolPacket(hand, hitPos));
                  if (event instanceof ICancellableEvent) {
                     event.setCanceled(true);
                  }
               }
            }
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   public void processMousePress(Post event) {
      Minecraft mc = Minecraft.getInstance();
      int action = event.getAction();
      if (action == 1 || action == 2) {
         if (mc.player != null) {
            Pair<InteractionHand, ItemStack> tool = getBuildingTool(mc.player);
            if (tool != null) {
               InteractionHand hand = (InteractionHand)tool.getFirst();
               ItemStack stack = (ItemStack)tool.getSecond();
               this.processModdedKeyMapping(hand, stack, event);
               return;
            }
         }

         this.voidModdedKeyMapping();
      }
   }

   @OnlyIn(Dist.CLIENT)
   public void processModdedKeyMapping(InteractionHand hand, ItemStack stack, InputEvent event) {
      Minecraft mc = Minecraft.getInstance();

      while (((KeyMapping)ClientProxy.UNDO_KEY.get()).consumeClick()) {
         if (mc.screen == null && this.hasPermission(stack, mc.player)) {
            SGPacketHandler.sendToServer(ActionHistoryPacket.undo(true));
         }
      }

      while (((KeyMapping)ClientProxy.REDO_KEY.get()).consumeClick()) {
         if (mc.screen == null && this.hasPermission(stack, mc.player)) {
            SGPacketHandler.sendToServer(ActionHistoryPacket.redo(true));
         }
      }

      while (((KeyMapping)ClientProxy.COPY_KEY.get()).consumeClick()) {
         if (mc.screen == null && this.hasPermission(stack, mc.player) && getMode(stack) == BuildingToolModes.CLONE) {
            SGPacketHandler.sendToServer(ActionHistoryPacket.copy(hand, true));
         }
      }

      while (((KeyMapping)ClientProxy.PASTE_KEY.get()).consumeClick()) {
         if (mc.screen == null && this.hasPermission(stack, mc.player) && getMode(stack) == BuildingToolModes.CLONE) {
            KeyMapping.click(mc.options.keyAttack.getKey());
         }
      }

      while (((KeyMapping)ClientProxy.DELETE_KEY.get()).consumeClick()) {
         if (mc.screen == null && this.hasPermission(stack, mc.player)) {
            SGPacketHandler.sendToServer(new ProcessDeletePacket());
         }
      }

      while (((KeyMapping)ClientProxy.BUILDING_TOOL_KEY.get()).consumeClick()) {
         if (this.hasPermission(stack, mc.player)) {
            BuildingToolBounds.releaseGrabbedCorner(stack, mc.player);
            SGPacketHandler.sendToServer(ActionHistoryPacket.releaseGrabbedCorner(hand));
            StructureGelMod.proxy.openBuildingToolScreen(stack, hand);
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   public void voidModdedKeyMapping() {
      for (Lazy<KeyMapping> mapping : ClientProxy.KEY_MAPPINGS) {
         while (((KeyMapping)mapping.get()).consumeClick()) {
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   public void onMouseScroll(MouseScrollingEvent event) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null) {
         Pair<InteractionHand, ItemStack> tool = getBuildingTool(mc.player);
         if (tool == null) {
            return;
         }

         InteractionHand hand = (InteractionHand)tool.getFirst();
         ItemStack stack = (ItemStack)tool.getSecond();
         if (mc.screen == null && mc.options.keySprint.isDown() && this.hasPermission(stack, mc.player)) {
            int scrollDelta = (int)event.getScrollDeltaY();
            if (scrollDelta != 0) {
               if (getSelectedCorner(stack) == null) {
                  int reach = setReachDistanceModifier(stack, getReachDistanceModifier(stack) + scrollDelta);
                  SGPacketHandler.sendToServer(UpdateBuildingToolPacket.builder().hand(hand).reachDistance(reach).build());
                  mc.player
                     .displayClientMessage(
                        Component.translatable(
                           "info.structure_gel.building_tool.message.set_reach", new Object[]{reach + getMode(stack).getReachDistance(stack)}
                        ),
                        true
                     );
               } else {
                  float cornerDistance = setSelectedCornerDistance(this.getDefaultInstance(), getSelectedCornerDistance(stack) + scrollDelta);
                  SGPacketHandler.sendToServer(UpdateBuildingToolPacket.builder().hand(hand).cornerReachDistance(cornerDistance).build());
                  DecimalFormat formatter = new DecimalFormat("0.##");
                  mc.player
                     .displayClientMessage(
                        Component.translatable("info.structure_gel.building_tool.message.set_reach", new Object[]{formatter.format(cornerDistance)}), true
                     );
               }

               event.setCanceled(true);
            }
         }
      }
   }

   public void tick(PlayerTickEvent.Post event) {
      if (event.getEntity() != null) {
         Pair<InteractionHand, ItemStack> tool = getBuildingTool(event.getEntity());
         if (tool == null) {
            return;
         }

         ItemStack stack = (ItemStack)tool.getSecond();
         if (BuildingToolBounds.tick(stack, event.getEntity())) {
            return;
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   private void clientTick(ClientTickEvent.Post event) {
               if (Minecraft.getInstance().options.keyAttack.isDown()) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player.isCreative() && this.hasPermission(player.getMainHandItem(), player)) {
               leftClickHoldTime++;
            }
         } else if (leftClickHoldTime > 0) {
            leftClickHoldTime = 0;
         }

         if (Minecraft.getInstance().options.keyUse.isDown()) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player.isCreative() && this.hasPermission(player.getMainHandItem(), player)) {
               rightClickHoldTime++;
            }
         } else if (rightClickHoldTime > 0) {
            rightClickHoldTime = 0;
         }
   }

   @OnlyIn(Dist.CLIENT)
   private void renderTick(RenderFrameEvent.Post event) {
               if (leftClickHoldTime >= 5 || rightClickHoldTime >= 5) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player.isCreative() && Minecraft.getInstance().hitResult instanceof BlockHitResult blockHit) {
               BlockPos pos = blockHit.getBlockPos();
               ItemStack stack = player.getMainHandItem();
               BuildingToolMode mode = getMode(stack);
               if (!lastHitPos.equals(pos) || !mode.targetsSpecificPos()) {
                  Direction clickedFace = blockHit.getDirection();
                  if (leftClickHoldTime >= 5) {
                     long time = Minecraft.getInstance().level.getGameTime();
                     if (time - lastActionTime >= mode.getSpamDelay() || lastActionTime == 0L) {
                        this.onLeftClick(stack, player, pos, clickedFace, true);
                        SGPacketHandler.sendToServer(new LeftClickBuildingToolPacket(InteractionHand.MAIN_HAND, pos, clickedFace, true));
                        lastActionTime = time;
                     }
                  } else if (rightClickHoldTime >= 5) {
                     InteractionHand hand = player.getUsedItemHand();
                     this.onRightClick(stack, hand, player, pos, clickedFace, true);
                     SGPacketHandler.sendToServer(new RightClickBuildingToolPacket(hand, pos, clickedFace, true));
                  }

                  lastHitPos = pos;
               }
            }
         }
   }

   public boolean onRightClick(ItemStack stack, InteractionHand hand, Player player, BlockPos clickedPos, Direction clickedFace, boolean isHolding) {
      if (this.hasPermission(stack, player)) {
         BuildingToolMode mode = getMode(stack);
         Level level = player.level();
         player.swing(hand);
         if (!isHolding && BuildingToolBounds.handleBoundingBoxClick(stack, player)) {
            return true;
         }

         if (level.getBlockState(clickedPos).isAir()) {
            mode.onRightClickAir(level, player, clickedPos, stack);
         } else {
            mode.onRightClickBlock(level, player, clickedPos, stack, clickedFace);
         }

         return true;
      } else {
         return false;
      }
   }

   private void onRightClickEvent(PlayerInteractEvent event, BlockPos pos) {
      if (pos != null) {
         Player player = event.getEntity();
         ItemStack stack = event.getItemStack();
         if (player != null && player.level().isClientSide && this.hasPermission(stack, player)) {
            Direction clickedFace = event.getFace();
            if (clickedFace == null) {
               clickedFace = Direction.UP;
            }

            if (this.onRightClick(stack, event.getHand(), player, pos, clickedFace, rightClickHoldTime >= 5) && event instanceof ICancellableEvent cancellable) {
               cancellable.setCanceled(true);
               event.setCancellationResult(InteractionResult.SUCCESS);
            }

            SGPacketHandler.sendToServer(new RightClickBuildingToolPacket(event.getHand(), pos, clickedFace, rightClickHoldTime >= 5));
         }
      }
   }

   public boolean onLeftClick(ItemStack stack, Player player, BlockPos clickedPos, Direction clickedFace, boolean isHolding) {
      if (this.hasPermission(stack, player)) {
         if (getSelectedCorner(stack) != null) {
            return false;
         }

         getMode(stack).onLeftClick(player.level(), player, clickedPos, stack, clickedFace);
         return true;
      } else {
         return false;
      }
   }

   private void onLeftClickEvent(PlayerInteractEvent event, BlockPos pos) {
      if (pos != null) {
         Player player = event.getEntity();
         ItemStack stack = event.getItemStack();
         if (player != null && player.level().isClientSide && this.hasPermission(stack, player) && (!player.swinging || player.swingTime > 1)) {
            Direction clickedFace = event.getFace();
            if (clickedFace == null) {
               clickedFace = Direction.UP;
            }

            if (this.onLeftClick(stack, player, pos, clickedFace, leftClickHoldTime >= 5) && event instanceof ICancellableEvent cancellable) {
               cancellable.setCanceled(true);
               event.setCancellationResult(InteractionResult.SUCCESS);
            }

            SGPacketHandler.sendToServer(new LeftClickBuildingToolPacket(event.getHand(), pos, clickedFace, leftClickHoldTime >= 5));
         }
      }
   }

   public void onMiddleClick(ItemStack stack, Player player, BlockPos clickedPos) {
      if (clickedPos != null && this.hasPermission(stack, player)) {
         Level level = player.level();
         BuildingToolMode mode = getMode(stack);
         if (level.getBlockState(clickedPos).isAir()) {
            mode.onMiddleClickAir(level, player, clickedPos, stack);
         } else {
            mode.onMiddleClickBlock(level, player, clickedPos, stack);
         }
      }
   }

   public Component getName(ItemStack stack) {
      BuildingToolMode mode = getMode(stack);
      Component name = super.getName(stack);
      return mode.isNone() ? name : this.nameCache.apply(mode, name.getString());
   }

   public static boolean hasToolPermission(ItemStack stack, Player player) {
      return SGRegistry.Items.BUILDING_TOOL.get().hasPermission(stack, player);
   }

   private boolean hasPermission(ItemStack stack, Player player) {
      if (stack.is(this)) {
         boolean hasPermission = player.isCreative();
         if (!hasPermission && !player.isSpectator()) {
            player.displayClientMessage(NO_PERMISSION, true);
         }

         return hasPermission;
      } else {
         return false;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public void appendHoverText(ItemStack stack, Level level, List<Component> list, TooltipFlag showAdvanced) {
      BuildingToolMode mode = getMode(stack);
      if (Screen.hasShiftDown()) {
         if (!mode.isNone()) {
            list.add(SGText.applyKeybindFilter(Component.translatable(mode.getDescKey(), mode.getDescArgs())));
            list.add(SGText.NEW_LINE);
            list.add(
               SGText.BULLET_POINT
                  .copy()
                  .withStyle(ChatFormatting.GRAY)
                  .append(Component.translatable("info.structure_gel.building_tool.block_palette").withStyle(SGText.VALUE_LABEL_STYLE))
            );
            list.add(Component.literal(BLOCK_PALETTE_TOOLTIP));

            for (ToolModeProperty property : mode.getProperties().values()) {
               list.add(
                  SGText.BULLET_POINT
                     .copy()
                     .withStyle(ChatFormatting.GRAY)
                     .append(Component.literal(property.getNameComponent().getString()).withStyle(SGText.VALUE_LABEL_STYLE))
                     .append(Component.literal(": " + property.getValueComponent(getProperty(stack, property)).getString()))
               );
            }

            list.add(SGText.NEW_LINE);
         } else {
            String buildingToolKey = SGText.keybindString((KeyMapping)ClientProxy.BUILDING_TOOL_KEY.get());
            list.add(SGText.applyKeybindFilter(Component.translatable("info.structure_gel.building_tool_description", new Object[]{buildingToolKey})));
            list.add(SGText.NEW_LINE);
         }
      } else if (Screen.hasControlDown()) {
         Options options = Minecraft.getInstance().options;
         String buildingToolKey = SGText.keybindString((KeyMapping)ClientProxy.BUILDING_TOOL_KEY.get());
         String middleClick = SGText.keybindString(options.keyPickItem);
         String control = SGText.keybindString(options.keySprint);
         String undo = SGText.keybindString((KeyMapping)ClientProxy.UNDO_KEY.get());
         String redo = SGText.keybindString((KeyMapping)ClientProxy.REDO_KEY.get());
         list.add(SGText.applyKeybindFilter(Component.translatable("item.structure_gel.building_tool.info.open_gui", new Object[]{buildingToolKey})));
         list.add(SGText.applyKeybindFilter(Component.translatable("item.structure_gel.building_tool.info.select_block", new Object[]{middleClick})));
         list.add(SGText.applyKeybindFilter(Component.translatable("item.structure_gel.building_tool.info.zoom_reach", new Object[]{control})));
         list.add(SGText.applyKeybindFilter(Component.translatable("item.structure_gel.building_tool.info.undo_and_redo", new Object[]{undo, redo})));
         list.add(SGText.NEW_LINE);
      } else {
         list.add(SGText.applyKeybindFilter(Component.translatable("info.structure_gel.hold_shift")));
         list.add(SGText.applyKeybindFilter(Component.translatable("info.structure_gel.hold_control")));
      }
   }

   public static String getPaletteString(WeightedRandomList<Wrapper<BlockState>> palette) {
      List<Wrapper<BlockState>> states = palette.unwrap().stream().sorted((o1, o2) -> Integer.compare(o2.getWeight().asInt(), o1.getWeight().asInt())).toList();
      if (states.isEmpty()) {
         states = SimpleWeightedRandomList.builder().add(Blocks.AIR.defaultBlockState(), 1).build().unwrap();
      }

      StringBuilder paletteStr = new StringBuilder();
      float totalWeight = WeightedRandom.getTotalWeight(states);

      for (Wrapper<BlockState> state : states) {
         int percent = Math.round(state.getWeight().asInt() / totalWeight * 100.0F);
         paletteStr.append("\n   " + percent + "% " + ((BlockState)state.getData()).getBlock().getName().getString());
      }

      return paletteStr.toString();
   }

   public boolean overrideOtherStackedOnMe(ItemStack stack, ItemStack other, Slot slot, ClickAction clickAction, Player player, SlotAccess slotAccess) {
      if (!other.isEmpty() && clickAction == ClickAction.SECONDARY) {
         setPaletteFromInventory(stack, readPaletteFrom(other, player.level()), player);
         return true;
      } else {
         return false;
      }
   }

   public boolean overrideStackedOnOther(ItemStack stack, Slot slot, ClickAction clickAction, Player player) {
      if (clickAction == ClickAction.SECONDARY) {
         ItemStack other = slot.getItem();
         if (!other.isEmpty()) {
            setPaletteFromInventory(stack, readPaletteFrom(other, player.level()), player);
            return true;
         }
      }

      return false;
   }

   private static void setPaletteFromInventory(ItemStack stack, WeightedRandomList<Wrapper<BlockState>> palette, Player player) {
      setPalette(stack, palette);
      player.playSound(SoundEvents.ITEM_PICKUP, 0.8F, 0.9F + player.level().getRandom().nextFloat() * 0.2F);
   }

   public static BuildingToolMode getMode(ItemStack stack) {
      CompoundTag tag = LegacyNbt.get(stack);
      if (tag != null && tag.contains("tool_mode", 8)) {
         String modeS = tag.getString("tool_mode");
         if (LegacyRL.isValid(modeS)) {
            BuildingToolMode mode = BuildingToolModes.REGISTRY.get(ResourceLocation.parse(modeS));
            if (mode != null) {
               return mode;
            }
         }
      }

      return BuildingToolModes.NONE;
   }

   public static void setMode(ItemStack stack, BuildingToolMode mode) {
      CompoundTag tag = LegacyNbt.getOrCreate(stack);
      tag.putString("tool_mode", mode.getName().toString());
      tag.remove("properties");
   }

   public static Optional<BlockPos> getPos(ItemStack stack, int index) {
      CompoundTag tag = LegacyNbt.get(stack);
      if (tag != null && tag.contains("poses", 10)) {
         CompoundTag poses = tag.getCompound("poses");
         String indexKey = Integer.toString(index);
         if (poses.contains(indexKey, 11)) {
            int[] coords = poses.getIntArray(indexKey);
            if (coords.length == 3) {
               return Optional.of(new BlockPos(coords[0], coords[1], coords[2]));
            }
         }
      }

      return Optional.empty();
   }

   public static boolean hasCompleteSelection(ItemStack stack) {
      return getPos(stack, 0).isPresent() && getPos(stack, 1).isPresent();
   }

   public static boolean setPos(ItemStack stack, int index, Vec3i pos) {
      if (getPos(stack, index).equals(Optional.of(pos))) {
         return false;
      }

      CompoundTag tag = LegacyNbt.getOrCreate(stack);
      CompoundTag posesTag = tag.getCompound("poses");
      posesTag.putIntArray(Integer.toString(index), new int[]{pos.getX(), pos.getY(), pos.getZ()});
      tag.put("poses", posesTag);
      return true;
   }

   public static void clearPoses(ItemStack stack) {
      CompoundTag tag = LegacyNbt.get(stack);
      if (tag != null && tag.contains("poses", 10)) {
         tag.put("poses", new CompoundTag());
      }

      setSelectedCorner(stack, null);
   }

   public static WeightedRandomList<Wrapper<BlockState>> getPalette(ItemStack stack, LevelReader level) {
      WeightedRandomList<Wrapper<BlockState>> ret = WeightedRandomList.create();
      CompoundTag tag = LegacyNbt.get(stack);
      if (tag != null && tag.contains("pallete", 9)) {
         ListTag paletteTag = tag.getList("pallete", 10);
         net.minecraft.util.random.SimpleWeightedRandomList.Builder<BlockState> builder = SimpleWeightedRandomList.builder();
         FeatureFlagSet featureFlags = level.enabledFeatures();

         for (Tag t : paletteTag) {
            if (t instanceof CompoundTag stateTag && stateTag.contains("state", 10) && stateTag.contains("weight", 3)) {
               try {
                  BlockState state = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), stateTag.getCompound("state"));
                  if (state.getBlock().isEnabled(featureFlags)) {
                     int weight = stateTag.getInt("weight");
                     builder.add(state, weight);
                  }
               } catch (Exception e) {
                  StructureGelMod.LOGGER.error("Couldn't read block state from tag.", stateTag.getAsString());
               }
            }
         }

         ret = builder.build();
      }

      if (ret.isEmpty()) {
         ret = SimpleWeightedRandomList.builder().add(Blocks.AIR.defaultBlockState(), 1).build();
      }

      return ret;
   }

   public static void setPalette(ItemStack stack, WeightedRandomList<Wrapper<BlockState>> palette) {
      ListTag paletteList = new ListTag();

      for (Wrapper<BlockState> state : palette.unwrap()) {
         CompoundTag stateTag = new CompoundTag();
         stateTag.put("state", NbtUtils.writeBlockState((BlockState)state.getData()));
         stateTag.putInt("weight", state.getWeight().asInt());
         paletteList.add(stateTag);
      }

      LegacyNbt.getOrCreate(stack).put("pallete", paletteList);
   }

   public static WeightedRandomList<Wrapper<BlockState>> readPaletteFrom(Level level, BlockPos pos) {
      if (level.getBlockEntity(pos) instanceof Container readFrom && !readFrom.isEmpty()) {
         Map<Item, Integer> stacks = new HashMap<>();
         int size = readFrom.getContainerSize();

         for (int i = 0; i < size; i++) {
            ItemStack stack = readFrom.getItem(i);
            int count = stack.getCount();
            stacks.compute(stack.getItem(), (item, c) -> c == null ? count : c + count);
         }

         WeightedRandomList<Wrapper<BlockState>> palette = readPaletteFrom(stacks, level);
         if (!palette.isEmpty()) {
            return palette;
         }
      }

      return SimpleWeightedRandomList.builder().add(level.getBlockState(pos), 1).build();
   }

   public static WeightedRandomList<Wrapper<BlockState>> readPaletteFrom(ItemStack readFrom, LevelReader level) {
      if (readFrom.is(SGRegistry.Items.BLOCK_PALETTE.get())) {
         return getPalette(readFrom, level);
      }

      CompoundTag tag = BlockItem.getBlockEntityData(readFrom);
      if (tag != null) {
         Map<Item, Integer> stacks = new HashMap<>();
         if (tag.contains("Items", 9)) {
            NonNullList<ItemStack> contents = NonNullList.withSize(27, ItemStack.EMPTY);
            ContainerHelper.loadAllItems(tag, contents);
            if (contents.isEmpty()) {
               return readPaletteFrom(Map.of(readFrom.getItem(), readFrom.getCount()), level);
            }

            for (ItemStack stack : contents) {
               int count = stack.getCount();
               stacks.compute(stack.getItem(), (i, c) -> c == null ? count : c + count);
            }
         }

         WeightedRandomList<Wrapper<BlockState>> palette = readPaletteFrom(stacks, level);
         if (!palette.isEmpty()) {
            return palette;
         }
      }

      Optional<BlockState> opState = getStateForItem(readFrom.getItem(), level, true);
      return (WeightedRandomList<Wrapper<BlockState>>)(opState.isPresent()
         ? SimpleWeightedRandomList.builder().add(opState.get(), 1).build()
         : WeightedRandomList.create());
   }

   public static WeightedRandomList<Wrapper<BlockState>> readPaletteFrom(Map<Item, Integer> items, LevelReader level) {
      net.minecraft.util.random.SimpleWeightedRandomList.Builder<BlockState> builder = SimpleWeightedRandomList.builder();

      for (Entry<Item, Integer> entry : items.entrySet()) {
         Optional<BlockState> opState = getStateForItem(entry.getKey(), level, false);
         if (opState.isPresent()) {
            builder.add(opState.get(), entry.getValue());
         }
      }

      return builder.build();
   }

   public static ItemStack getItemForBlock(BlockState state, LevelReader level) {
      Block block = state.getBlock();
      ItemStack stack;
      if (block instanceof LiquidBlock liquidBlock) {
         stack = liquidBlock.getFluid().getBucket().getDefaultInstance();
      } else {
         stack = block.asItem().getDefaultInstance();
      }

      if (stack.is(Items.AIR)) {
         stack = Items.BUCKET.getDefaultInstance();
      }

      return stack.isItemEnabled(level.enabledFeatures()) ? stack : ItemStack.EMPTY;
   }

   public static Optional<BlockState> getStateForItem(Item item, LevelReader level, boolean includeAir) {
      BlockState state = null;
      if (item instanceof BlockItem blockItem) {
         state = blockItem.getBlock().defaultBlockState();
      } else if (item instanceof BucketItem bucket) {
         state = bucket.getFluid().defaultFluidState().createLegacyBlock();
      } else if (item instanceof SolidBucketItem solidBucket) {
         state = solidBucket.getBlock().defaultBlockState();
      } else if (includeAir) {
         state = Blocks.AIR.defaultBlockState();
      }

      return Optional.ofNullable(state != null && state.getBlock().isEnabled(level.enabledFeatures()) ? state : null);
   }

   public static void clearPalette(ItemStack stack) {
      CompoundTag tag = LegacyNbt.get(stack);
      if (tag != null && tag.contains("pallete", 9)) {
         tag.remove("pallete");
      }
   }

   public static <T> T getProperty(ItemStack stack, ToolModeProperty<T> property) {
      CompoundTag tag = LegacyNbt.get(stack);
      if (tag != null && tag.contains("properties", 10)) {
         CompoundTag propertiesTag = tag.getCompound("properties");
         String key = property.getKey();
         if (propertiesTag.contains(key, 8)) {
            return property.read(propertiesTag.getString(key));
         }
      }

      return property.getDefaultValue();
   }

   public static <T> void setProperty(ItemStack stack, ToolModeProperty<T> property, T value) {
      CompoundTag tag = LegacyNbt.getOrCreate(stack);
      CompoundTag propertiesTag = tag.getCompound("properties");
      propertiesTag.putString(property.getKey(), property.write(value));
      tag.put("properties", propertiesTag);
   }

   public static int getReachDistanceModifier(ItemStack stack) {
      CompoundTag tag = LegacyNbt.get(stack);
      return tag != null && tag.contains("reach_distance", 3) ? tag.getInt("reach_distance") : 0;
   }

   public static int setReachDistanceModifier(ItemStack stack, int reachDistanceModifier) {
      reachDistanceModifier = Mth.clamp(reachDistanceModifier, ToolModeProperty.REACH_DISTANCE.min(), ToolModeProperty.REACH_DISTANCE.max());
      LegacyNbt.getOrCreate(stack).putInt("reach_distance", reachDistanceModifier);
      return reachDistanceModifier;
   }

   public static boolean causesBlockUpdates(ItemStack stack) {
      CompoundTag tag = LegacyNbt.get(stack);
      return tag != null && tag.contains("cause_block_updates", 1) ? tag.getBoolean("cause_block_updates") : false;
   }

   public static void setCausesBlockUpdates(ItemStack stack, boolean causeBlockUpdates) {
      LegacyNbt.getOrCreate(stack).putBoolean("cause_block_updates", causeBlockUpdates);
   }

   @Nullable
   public static SmartBoundingBox.CornerType getSelectedCorner(ItemStack stack) {
      CompoundTag tag = LegacyNbt.get(stack);
      if (tag != null && tag.contains("selected_corner", 3)) {
         int cornerID = tag.getInt("selected_corner");
         SmartBoundingBox.CornerType[] types = SmartBoundingBox.CornerType.values();
         if (cornerID > -1 && cornerID < types.length) {
            return types[cornerID];
         }
      }

      return null;
   }

   public static void setSelectedCorner(ItemStack stack, SmartBoundingBox.CornerType cornerType) {
      LegacyNbt.getOrCreate(stack).putInt("selected_corner", cornerType == null ? -1 : cornerType.ordinal());
   }

   public static float getSelectedCornerDistance(ItemStack stack) {
      CompoundTag tag = LegacyNbt.get(stack);
      return tag != null && tag.contains("selected_corner_distance", 5) ? tag.getFloat("selected_corner_distance") : 3.0F;
   }

   public static float setSelectedCornerDistance(ItemStack stack, float cornerDistance) {
      cornerDistance = Mth.clamp(cornerDistance, 1.0F, ToolModeProperty.REACH_DISTANCE.max().intValue());
      LegacyNbt.getOrCreate(stack).putFloat("selected_corner_distance", cornerDistance);
      return cornerDistance;
   }

   public static boolean isMovingBounds(ItemStack stack) {
      CompoundTag tag = LegacyNbt.get(stack);
      return tag != null && tag.contains("move_bounds", 1) ? tag.getBoolean("move_bounds") : false;
   }

   public static void setMovingBounds(ItemStack stack, boolean moveBounds) {
      LegacyNbt.getOrCreate(stack).putBoolean("move_bounds", moveBounds);
   }

   @Nullable
   public static CapturedBlocks getCapturedBlocks(ItemStack stack, Level level, Player player) {
      CompoundTag tag = LegacyNbt.get(stack);
      if (tag != null && tag.contains("captured_blocks", 8)) {
         String boundsStr = tag.getString("captured_blocks");
         int[] corners = Stream.of(boundsStr.split(",")).mapToInt(s -> Integer.parseInt(s)).toArray();
         BoundingBox bounds = new BoundingBox(corners[0], corners[1], corners[2], corners[3], corners[4], corners[5]);
         CapturedBlocks captured = StructureGelMod.proxy.getBuildingToolData(level, player.getGameProfile().getName()).getCapturedBlocks(bounds);
         if (level.isClientSide && captured == null) {
            long time = System.currentTimeMillis();
            if (!lastClipboardRequestBounds.equals(bounds) || time - lastClipboardRequestTime > 1000L || lastClipboardRequestTime == 0L) {
               lastClipboardRequestTime = time;
               lastClipboardRequestBounds = bounds;
               SGPacketHandler.sendToServer(new RequestClipboardPacket(bounds));
            }
         }

         return captured;
      } else {
         return null;
      }
   }

   public static void setCapturedBlocks(ItemStack stack, Level level, Player player, Pair<BoundingBox, CapturedBlocks> clipboard) {
      if (level instanceof ServerLevel serverLevel) {
         CompoundTag tag = LegacyNbt.getOrCreate(stack);
         if (clipboard == null) {
            tag.remove("captured_blocks");
         } else {
            BoundingBox bounds = (BoundingBox)clipboard.getFirst();
            CapturedBlocks captured = (CapturedBlocks)clipboard.getSecond();
            BuildingToolPlayerData data = BuildingToolPlayerData.get(serverLevel, player.getGameProfile().getName());
            data.addToClipboard(bounds, captured);
            String boundsStr = bounds.minX() + "," + bounds.minY() + "," + bounds.minZ() + "," + bounds.maxX() + "," + bounds.maxY() + "," + bounds.maxZ();
            tag.putString("captured_blocks", boundsStr);
         }
      }
   }
}
