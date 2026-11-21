package com.legacy.structure_gel.core.item.building_tool;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.legacy.structure_gel.core.StructureGelMod;
import com.legacy.structure_gel.core.capability.level.BuildingToolPlayerData;
import com.legacy.structure_gel.core.data_components.BlockPalette;
import com.legacy.structure_gel.core.data_components.BuildingToolProperties;
import com.legacy.structure_gel.core.data_components.CloneRegion;
import com.legacy.structure_gel.core.item.building_tool.SmartBoundingBox.CornerType;
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
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.datafixers.util.Pair;

import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.util.random.WeightedEntry.Wrapper;
import net.minecraft.util.random.WeightedRandom;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SolidBucketItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.InputEvent.MouseScrollingEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public final class BuildingToolItem extends Item
{
	private final BiFunction<BuildingToolMode, String, Component> nameCache = Util.memoize((mode, name) -> Component.literal(name).append(Component.literal(": ")).append(mode.getComponent()));
	public static int leftClickHoldTime = 0, rightClickHoldTime = 0;
	public static final int CLICK_HOLD_TIME = 5;
	private static BlockPos lastHitPos = BlockPos.ZERO;

	{
		if (FMLEnvironment.dist == Dist.CLIENT)
			this.clientInit();
	}

	@OnlyIn(Dist.CLIENT)
	private void clientInit()
	{
		var bus = NeoForge.EVENT_BUS;
		bus.addListener(PlayerInteractEvent.LeftClickEmpty.class, event -> this.onLeftClickEvent(event, net.minecraft.client.Minecraft.getInstance().hitResult instanceof BlockHitResult blockHit ? blockHit.getBlockPos() : null));
		bus.addListener(PlayerInteractEvent.LeftClickBlock.class, event -> this.onLeftClickEvent(event, event.getPos()));
		bus.addListener(PlayerInteractEvent.RightClickItem.class, event -> this.onRightClickEvent(event, net.minecraft.client.Minecraft.getInstance().hitResult instanceof BlockHitResult blockHit ? blockHit.getBlockPos() : null, event::setCancellationResult));
		bus.addListener(PlayerInteractEvent.RightClickBlock.class, event -> this.onRightClickEvent(event, event.getPos(), event::setCancellationResult));
		bus.addListener(InputEvent.Key.class, event -> this.processKeyPress(event));
		bus.addListener(InputEvent.MouseButton.Pre.class, event -> this.interceptMousePress(event));
		bus.addListener(InputEvent.MouseButton.Post.class, event -> this.processMousePress(event));
		bus.addListener(InputEvent.MouseScrollingEvent.class, event -> this.onMouseScroll(event));
		bus.addListener(PlayerTickEvent.Pre.class, event -> this.tick(event));
		bus.addListener(ClientTickEvent.Post.class, event -> this.clientTick(event));
		bus.addListener(RenderFrameEvent.Post.class, event -> this.renderTick(event));
	}

	@Nullable
	public static Pair<InteractionHand, ItemStack> getBuildingTool(Player player)
	{
		ItemStack stack = player.getMainHandItem();
		if (stack.is(SGRegistry.Items.BUILDING_TOOL.get()))
		{
			return Pair.of(InteractionHand.MAIN_HAND, stack);
		}

		stack = player.getOffhandItem();
		if (stack.is(SGRegistry.Items.BUILDING_TOOL.get()))
		{
			return Pair.of(InteractionHand.OFF_HAND, stack);
		}
		return null;
	}

	public BuildingToolItem(Item.Properties properties)
	{
		super(properties);
	}

	@Override
	public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack)
	{
		var builder = ItemAttributeModifiers.builder();
		int reach = getReachDistanceModifier(stack) + getMode(stack).getReachDistance(stack);
		builder.add(Attributes.BLOCK_INTERACTION_RANGE, new AttributeModifier(StructureGelMod.locate("building_tool_block_reach"), reach, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.HAND);
		builder.add(Attributes.ENTITY_INTERACTION_RANGE, new AttributeModifier(StructureGelMod.locate("building_tool_entity_reach"), reach, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.HAND);
		return builder.build();
	}

	@Override
	public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, Player player)
	{
		return false;
	}

	@OnlyIn(Dist.CLIENT)
	public void processKeyPress(InputEvent.Key event)
	{
		net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
		int action = event.getAction();
		if (action == InputConstants.PRESS || action == InputConstants.REPEAT)
		{
			if (mc.player != null)
			{
				Pair<InteractionHand, ItemStack> tool = getBuildingTool(mc.player);
				if (tool != null)
				{
					var hand = tool.getFirst();
					var stack = tool.getSecond();

					this.processModdedKeyMapping(hand, stack, event);
					return;
				}
			}

			this.voidModdedKeyMapping();
		}
	}

	@OnlyIn(Dist.CLIENT)
	public void interceptMousePress(InputEvent.MouseButton.Pre event)
	{
		net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
		int action = event.getAction();
		if (action == InputConstants.PRESS || action == InputConstants.REPEAT)
		{
			if (mc.player != null)
			{
				Pair<InteractionHand, ItemStack> tool = getBuildingTool(mc.player);
				if (tool != null)
				{
					var hand = tool.getFirst();
					var stack = tool.getSecond();

					// On middle click with building tool, override interaction
					if (event.getButton() == mc.options.keyPickItem.getKey().getValue() && mc.screen == null && this.hasPermission(stack, mc.player))
					{
						BlockPos hitPos = mc.hitResult instanceof BlockHitResult blockHit ? blockHit.getBlockPos() : null;
						if (hitPos != null)
						{
							this.onMiddleClick(stack, mc.player, hitPos);
							mc.player.swing(hand);
							SGPacketHandler.sendToServer(new MiddleClickBuildingToolPacket(hand, hitPos));
							if (event instanceof ICancellableEvent)
								event.setCanceled(true);
						}
					}
				}
			}
		}
	}

	@OnlyIn(Dist.CLIENT)
	public void processMousePress(InputEvent.MouseButton.Post event)
	{
		net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
		int action = event.getAction();
		if (action == InputConstants.PRESS || action == InputConstants.REPEAT)
		{
			if (mc.player != null)
			{
				Pair<InteractionHand, ItemStack> tool = getBuildingTool(mc.player);
				if (tool != null)
				{
					var hand = tool.getFirst();
					var stack = tool.getSecond();

					this.processModdedKeyMapping(hand, stack, event);
					return;
				}
			}
			this.voidModdedKeyMapping();
		}
	}

	@OnlyIn(Dist.CLIENT)
	public void processModdedKeyMapping(InteractionHand hand, ItemStack stack, InputEvent event)
	{
		net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
		while (com.legacy.structure_gel.core.client.ClientProxy.UNDO_KEY.get().consumeClick())
		{
			if (mc.screen == null && this.hasPermission(stack, mc.player))
			{
				SGPacketHandler.sendToServer(ActionHistoryPacket.undo(true));
			}
		}
		while (com.legacy.structure_gel.core.client.ClientProxy.REDO_KEY.get().consumeClick())
		{
			if (mc.screen == null && this.hasPermission(stack, mc.player))
			{
				SGPacketHandler.sendToServer(ActionHistoryPacket.redo(true));
			}
		}
		while (com.legacy.structure_gel.core.client.ClientProxy.COPY_KEY.get().consumeClick())
		{
			if (mc.screen == null && this.hasPermission(stack, mc.player) && BuildingToolItem.getMode(stack) == BuildingToolModes.CLONE)
			{
				SGPacketHandler.sendToServer(ActionHistoryPacket.copy(hand, true));
			}
		}
		while (com.legacy.structure_gel.core.client.ClientProxy.PASTE_KEY.get().consumeClick())
		{
			if (mc.screen == null && this.hasPermission(stack, mc.player) && BuildingToolItem.getMode(stack) == BuildingToolModes.CLONE)
			{
				net.minecraft.client.KeyMapping.click(mc.options.keyAttack.getKey());
			}
		}
		while (com.legacy.structure_gel.core.client.ClientProxy.DELETE_KEY.get().consumeClick())
		{
			if (mc.screen == null && this.hasPermission(stack, mc.player))
			{
				SGPacketHandler.sendToServer(new ProcessDeletePacket());
			}
		}
		while (com.legacy.structure_gel.core.client.ClientProxy.BUILDING_TOOL_KEY.get().consumeClick())
		{
			if (this.hasPermission(stack, mc.player))
			{
				BuildingToolBounds.releaseGrabbedCorner(stack, mc.player);
				SGPacketHandler.sendToServer(ActionHistoryPacket.releaseGrabbedCorner(hand));
				StructureGelMod.proxy.openBuildingToolScreen(stack, hand);
			}
		}
	}

	@OnlyIn(Dist.CLIENT)
	public void voidModdedKeyMapping()
	{
		// Simply consumes the clicks and does nothing. Only should run if not holding a building tool
		for (var mapping : com.legacy.structure_gel.core.client.ClientProxy.KEY_MAPPINGS)
		{
			while (mapping.get().consumeClick())
			{
			}
		}
	}

	@OnlyIn(Dist.CLIENT)
	public void onMouseScroll(MouseScrollingEvent event)
	{
		net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
		if (mc.player != null)
		{
			Pair<InteractionHand, ItemStack> tool = getBuildingTool(mc.player);
			if (tool == null)
				return;
			var hand = tool.getFirst();
			var stack = tool.getSecond();

			if (mc.screen == null && mc.options.keySprint.isDown() && this.hasPermission(stack, mc.player))
			{
				int scrollDelta = (int) event.getScrollDeltaY();
				if (scrollDelta != 0)
				{
					if (getSelectedCorner(stack) == null)
					{
						int reach = setReachDistanceModifier(stack, getReachDistanceModifier(stack) + scrollDelta);
						SGPacketHandler.sendToServer(UpdateBuildingToolPacket.builder().hand(hand).reachDistance(reach).build());
						mc.player.displayClientMessage(Component.translatable(BuildingToolMode.SET_REACH_KEY, reach + getMode(stack).getReachDistance(stack)), true);
					}
					else
					{
						float cornerDistance = setSelectedCornerDistance(getDefaultInstance(), getSelectedCornerDistance(stack) + scrollDelta);
						SGPacketHandler.sendToServer(UpdateBuildingToolPacket.builder().hand(hand).cornerReachDistance(cornerDistance).build());
						DecimalFormat formatter = new DecimalFormat("0.##");
						mc.player.displayClientMessage(Component.translatable(BuildingToolMode.SET_REACH_KEY, formatter.format(cornerDistance)), true);
					}
					event.setCanceled(true);
				}
			}
		}
	}

	public void tick(net.neoforged.neoforge.event.tick.PlayerTickEvent.Pre event)
	{
		if (event.getEntity() != null)
		{
			Pair<InteractionHand, ItemStack> tool = getBuildingTool(event.getEntity());
			if (tool == null)
				return;
			var stack = tool.getSecond();

			if (BuildingToolBounds.tick(stack, event.getEntity()))
				return;
		}
	}

	@OnlyIn(Dist.CLIENT)
	private void clientTick(ClientTickEvent.Post event)
	{
		if (net.minecraft.client.Minecraft.getInstance().options.keyAttack.isDown())
		{
			net.minecraft.client.player.LocalPlayer player = net.minecraft.client.Minecraft.getInstance().player;
			if (player.isCreative() && this.hasPermission(player.getMainHandItem(), player))
			{
				leftClickHoldTime++;
			}
		}
		else
		{
			if (leftClickHoldTime > 0)
			{
				leftClickHoldTime = 0;
			}
		}

		if (net.minecraft.client.Minecraft.getInstance().options.keyUse.isDown())
		{
			net.minecraft.client.player.LocalPlayer player = net.minecraft.client.Minecraft.getInstance().player;
			if (player.isCreative() && this.hasPermission(player.getMainHandItem(), player))
			{
				rightClickHoldTime++;
			}
		}
		else
		{
			if (rightClickHoldTime > 0)
			{
				rightClickHoldTime = 0;
			}
		}
	}

	private static long lastActionTime = 0;

	@OnlyIn(Dist.CLIENT)
	private void renderTick(RenderFrameEvent.Post event)
	{
		if (leftClickHoldTime >= CLICK_HOLD_TIME || rightClickHoldTime >= CLICK_HOLD_TIME)
		{
			net.minecraft.client.player.LocalPlayer player = net.minecraft.client.Minecraft.getInstance().player;
			if (player.isCreative())
			{
				HitResult hitResult = net.minecraft.client.Minecraft.getInstance().hitResult;
				if (hitResult instanceof BlockHitResult blockHit)
				{
					BlockPos pos = blockHit.getBlockPos();
					ItemStack stack = player.getMainHandItem();
					BuildingToolMode mode = getMode(stack);
					if (!lastHitPos.equals(pos) || !mode.targetsSpecificPos())
					{
						Direction clickedFace = blockHit.getDirection();
						if (leftClickHoldTime >= CLICK_HOLD_TIME)
						{
							long time = net.minecraft.client.Minecraft.getInstance().level.getGameTime();
							if (time - lastActionTime >= mode.getSpamDelay() || lastActionTime == 0)
							{
								this.onLeftClick(stack, player, pos, clickedFace, true);
								SGPacketHandler.sendToServer(new LeftClickBuildingToolPacket(InteractionHand.MAIN_HAND, pos, clickedFace, true));
								lastActionTime = time;
							}
						}
						else if (rightClickHoldTime >= CLICK_HOLD_TIME)
						{
							InteractionHand hand = player.getUsedItemHand();
							this.onRightClick(stack, hand, player, pos, clickedFace, true);
							SGPacketHandler.sendToServer(new RightClickBuildingToolPacket(hand, pos, clickedFace, true));
						}
						lastHitPos = pos;
					}
				}
			}
		}
	}

	public boolean onRightClick(ItemStack stack, InteractionHand hand, Player player, BlockPos clickedPos, Direction clickedFace, boolean isHolding)
	{
		if (this.hasPermission(stack, player))
		{
			var mode = getMode(stack);
			Level level = player.level();

			player.swing(hand);

			if (!isHolding)
			{
				if (BuildingToolBounds.handleBoundingBoxClick(stack, player))
					return true;
			}

			if (level.getBlockState(clickedPos).isAir())
				mode.onRightClickAir(level, player, clickedPos, stack);
			else
				mode.onRightClickBlock(level, player, clickedPos, stack, clickedFace);

			return true;
		}
		return false;
	}

	// Only runs client side, sent to server
	private void onRightClickEvent(PlayerInteractEvent event, @Nullable BlockPos pos, Consumer<InteractionResult> result)
	{
		if (pos != null)
		{
			Player player = event.getEntity();
			ItemStack stack = event.getItemStack();
			if (player != null && player.level().isClientSide && this.hasPermission(stack, player))
			{
				Direction clickedFace = event.getFace();
				if (clickedFace == null)
					clickedFace = Direction.UP;
				if (this.onRightClick(stack, event.getHand(), player, pos, clickedFace, rightClickHoldTime >= CLICK_HOLD_TIME) && event instanceof ICancellableEvent cancellable)
				{
					cancellable.setCanceled(true);
					result.accept(InteractionResult.SUCCESS);
				}
				SGPacketHandler.sendToServer(new RightClickBuildingToolPacket(event.getHand(), pos, clickedFace, rightClickHoldTime >= CLICK_HOLD_TIME));
			}
		}
	}

	public boolean onLeftClick(ItemStack stack, Player player, BlockPos clickedPos, Direction clickedFace, boolean isHolding)
	{
		if (this.hasPermission(stack, player))
		{
			if (getSelectedCorner(stack) != null)
				return false;

			getMode(stack).onLeftClick(player.level(), player, clickedPos, stack, clickedFace);
			return true;
		}
		return false;
	}

	// Only runs client side, sent to server
	private void onLeftClickEvent(PlayerInteractEvent event, @Nullable BlockPos pos)
	{
		if (pos != null)
		{
			Player player = event.getEntity();
			ItemStack stack = event.getItemStack();
			if (player != null && player.level().isClientSide && this.hasPermission(stack, player) && (!player.swinging || player.swingTime > 1))
			{
				Direction clickedFace = event.getFace();
				if (clickedFace == null)
					clickedFace = Direction.UP;
				if (this.onLeftClick(stack, player, pos, clickedFace, leftClickHoldTime >= CLICK_HOLD_TIME) && event instanceof ICancellableEvent cancellable)
				{
					cancellable.setCanceled(true);
				}
				SGPacketHandler.sendToServer(new LeftClickBuildingToolPacket(event.getHand(), pos, clickedFace, leftClickHoldTime >= CLICK_HOLD_TIME));
			}
		}
	}

	public void onMiddleClick(ItemStack stack, Player player, BlockPos clickedPos)
	{
		if (clickedPos != null && this.hasPermission(stack, player))
		{
			Level level = player.level();
			var mode = getMode(stack);
			if (level.getBlockState(clickedPos).isAir())
				mode.onMiddleClickAir(level, player, clickedPos, stack);
			else
				mode.onMiddleClickBlock(level, player, clickedPos, stack);
		}
	}

	@Override
	public Component getName(ItemStack stack)
	{
		var mode = getMode(stack);
		var name = super.getName(stack);
		return mode.isNone() ? name : this.nameCache.apply(mode, name.getString());
	}

	private static final Component NO_PERMISSION = Component.translatable("info.structure_gel.building_tool.message.no_permission").withStyle(ChatFormatting.RED);

	public static boolean hasToolPermission(ItemStack stack, Player player)
	{
		return SGRegistry.Items.BUILDING_TOOL.get().hasPermission(stack, player);
	}

	private boolean hasPermission(ItemStack stack, Player player)
	{
		if (stack.is(this))
		{
			boolean hasPermission = player.isCreative();
			if (!hasPermission && !player.isSpectator())
				player.displayClientMessage(NO_PERMISSION, true);
			return hasPermission;
		}
		return false;
	}

	private static final String INFO_NAME = "item.structure_gel.building_tool.info.";
	public static final String BLOCK_PALETTE_NAME = "info.structure_gel.building_tool.block_palette";
	public static final String BLOCK_PALETTE_TOOLTIP = StructureGelMod.locate("block_palette_tooltip").toString();

	@SuppressWarnings({ "resource", "rawtypes", "unchecked" })
	@Override
	@OnlyIn(Dist.CLIENT)
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> list, TooltipFlag showAdvanced)
	{
		var mode = getMode(stack);
		if (net.minecraft.client.gui.screens.Screen.hasShiftDown())
		{
			if (!mode.isNone())
			{
				list.add(SGText.applyKeybindFilter(Component.translatable(mode.getDescKey(), mode.getDescArgs())));
				list.add(SGText.NEW_LINE);

				if (mode.hasBlockPalette())
				{
					list.add(SGText.BULLET_POINT.copy().withStyle(ChatFormatting.GRAY).append(Component.translatable(BLOCK_PALETTE_NAME).withStyle(SGText.VALUE_LABEL_STYLE)));
					list.add(Component.literal(BLOCK_PALETTE_TOOLTIP));
				}

				for (ToolModeProperty property : mode.getProperties().values())
					list.add(SGText.BULLET_POINT.copy().withStyle(ChatFormatting.GRAY).append(Component.literal(property.getNameComponent().getString()).withStyle(SGText.VALUE_LABEL_STYLE)).append(Component.literal(": " + property.getValueComponent(getProperty(stack, property)).getString())));

				list.add(SGText.NEW_LINE);
			}
			else
			{
				String buildingToolKey = SGText.keybindString(com.legacy.structure_gel.core.client.ClientProxy.BUILDING_TOOL_KEY.get());
				list.add(SGText.applyKeybindFilter(Component.translatable("info.structure_gel.building_tool_description", buildingToolKey)));
				list.add(SGText.NEW_LINE);
			}
		}
		else if (net.minecraft.client.gui.screens.Screen.hasControlDown())
		{
			net.minecraft.client.Options options = net.minecraft.client.Minecraft.getInstance().options;
			String buildingToolKey = SGText.keybindString(com.legacy.structure_gel.core.client.ClientProxy.BUILDING_TOOL_KEY.get());
			String middleClick = SGText.keybindString(options.keyPickItem);
			String control = SGText.keybindString(options.keySprint);
			String undo = SGText.keybindString(com.legacy.structure_gel.core.client.ClientProxy.UNDO_KEY.get());
			String redo = SGText.keybindString(com.legacy.structure_gel.core.client.ClientProxy.REDO_KEY.get());
			list.add(SGText.applyKeybindFilter(Component.translatable(INFO_NAME + "open_gui", buildingToolKey)));
			list.add(SGText.applyKeybindFilter(Component.translatable(INFO_NAME + "select_block", middleClick)));
			list.add(SGText.applyKeybindFilter(Component.translatable(INFO_NAME + "zoom_reach", control)));
			list.add(SGText.applyKeybindFilter(Component.translatable(INFO_NAME + "undo_and_redo", undo, redo)));
			list.add(SGText.NEW_LINE);
		}
		else
		{
			list.add(SGText.applyKeybindFilter(Component.translatable("info." + StructureGelMod.MODID + ".hold_shift")));
			list.add(SGText.applyKeybindFilter(Component.translatable("info." + StructureGelMod.MODID + ".hold_control")));
		}
	}

	public static String getPaletteString(WeightedRandomList<Wrapper<BlockState>> palette)
	{
		List<Wrapper<BlockState>> states = palette.unwrap().stream().sorted((o1, o2) -> Integer.compare(o2.getWeight().asInt(), o1.getWeight().asInt())).toList();
		if (states.isEmpty())
			states = SimpleWeightedRandomList.<BlockState>builder().add(Blocks.AIR.defaultBlockState(), 1).build().unwrap();
		StringBuilder paletteStr = new StringBuilder();
		float totalWeight = WeightedRandom.getTotalWeight(states);
		Iterator<Wrapper<BlockState>> it = states.iterator();
		while (it.hasNext())
		{
			Wrapper<BlockState> state = it.next();
			int percent = Math.round(state.getWeight().asInt() / totalWeight * 100.0F);
			paletteStr.append("\n   " + percent + "% " + state.data().getBlock().getName().getString());
		}
		return paletteStr.toString();
	}

	@Override
	public boolean overrideOtherStackedOnMe(ItemStack stack, ItemStack other, Slot slot, ClickAction clickAction, Player player, SlotAccess slotAccess)
	{
		if (!other.isEmpty() && clickAction == ClickAction.SECONDARY)
		{
			setPaletteFromInventory(stack, readPaletteFrom(other, player.level()), player);
			return true;
		}
		return false;
	}

	@Override
	public boolean overrideStackedOnOther(ItemStack stack, Slot slot, ClickAction clickAction, Player player)
	{
		if (clickAction == ClickAction.SECONDARY)
		{
			ItemStack other = slot.getItem();
			if (!other.isEmpty())
			{
				setPaletteFromInventory(stack, readPaletteFrom(other, player.level()), player);
				return true;
			}
		}
		return false;
	}

	private static void setPaletteFromInventory(ItemStack stack, BlockPalette palette, Player player)
	{
		setPalette(stack, palette);
		player.playSound(SoundEvents.ITEM_PICKUP, 0.8F, 0.9F + player.level().getRandom().nextFloat() * 0.2F);
	}

	public static BuildingToolMode getMode(ItemStack stack)
	{
		ResourceLocation modeName = stack.get(SGRegistry.DataComponents.BUILDING_TOOL_MODE);
		if (modeName != null)
		{
			BuildingToolMode mode = BuildingToolModes.REGISTRY.get(modeName);
			if (mode != null)
				return mode;
		}
		return BuildingToolModes.NONE;
	}

	public static void setMode(ItemStack stack, BuildingToolMode mode)
	{
		stack.set(SGRegistry.DataComponents.BUILDING_TOOL_MODE, mode.getName());
	}

	public static Optional<BlockPos> getPos(ItemStack stack, int index)
	{
		if (index > -1 && index < SGRegistry.DataComponents.TOOL_POSES.size())
		{
			BlockPos pos = stack.get(SGRegistry.DataComponents.TOOL_POSES.get(index));
			if (pos != null)
				return Optional.of(pos);
		}
		return Optional.empty();
	}

	public static boolean hasCompleteSelection(ItemStack stack)
	{
		return getPos(stack, 0).isPresent() && getPos(stack, 1).isPresent();
	}

	public static boolean setPos(ItemStack stack, int index, Vec3i pos)
	{
		if (getPos(stack, index).equals(Optional.of(pos)))
			return false;
		stack.set(SGRegistry.DataComponents.TOOL_POSES.get(index), new BlockPos(pos));
		return true;
	}

	public static void clearPoses(ItemStack stack)
	{
		for (var pos : SGRegistry.DataComponents.TOOL_POSES)
		{
			stack.remove(pos);
		}
		setSelectedCorner(stack, null);
	}

	public static BlockPalette getPalette(ItemStack stack, LevelReader level)
	{
		BlockPalette palette = stack.getOrDefault(SGRegistry.DataComponents.BLOCK_PALETTE, BlockPalette.EMPTY);
		if (palette.isEmpty())
			return BlockPalette.of(SimpleWeightedRandomList.<BlockState>builder().add(Blocks.AIR.defaultBlockState(), 1).build());
		return palette;
	}

	public static void setPalette(ItemStack stack, BlockPalette palette)
	{
		stack.set(SGRegistry.DataComponents.BLOCK_PALETTE, palette);
	}

	public static BlockPalette readPaletteFrom(Level level, BlockPos pos)
	{
		if (level.getBlockEntity(pos) instanceof Container readFrom)
		{
			if (!readFrom.isEmpty())
			{
				Map<Item, Integer> stacks = new HashMap<>();
				int size = readFrom.getContainerSize();
				for (int i = 0; i < size; i++)
				{
					ItemStack stack = readFrom.getItem(i);
					int count = stack.getCount();
					stacks.compute(stack.getItem(), (item, c) -> c == null ? count : c + count);
				}
				var palette = readPaletteFrom(stacks, level);
				if (!palette.isEmpty())
					return palette;
			}
		}
		return BlockPalette.of(SimpleWeightedRandomList.<BlockState>builder().add(level.getBlockState(pos), 1).build());
	}

	public static BlockPalette readPaletteFrom(ItemStack readFrom, LevelReader level)
	{
		// If this is a block palette item, use it directly
		if (readFrom.is(SGRegistry.Items.BLOCK_PALETTE.get()))
		{
			return getPalette(readFrom, level);
		}

		// Check if the item has contents (shulker box, chest, etc). If so, fill the palette with them
		ItemContainerContents container = readFrom.get(net.minecraft.core.component.DataComponents.CONTAINER);
		if (container != null)
		{
			Map<Item, Integer> stacks = new HashMap<>();
			List<ItemStack> containerItems = container.nonEmptyStream().toList();

			if (!containerItems.isEmpty()) // If items are present, fill the palette with them
			{
				for (ItemStack stack : containerItems)
				{
					int count = stack.getCount();
					stacks.compute(stack.getItem(), (i, c) -> c == null ? count : c + count);
				}
				var palette = readPaletteFrom(stacks, level);
				if (!palette.isEmpty())
					return palette;
			}
		}

		// Default to simply using the item in question if no items are contained
		return readPaletteFrom(Map.of(readFrom.getItem(), readFrom.getCount()), level);
	}

	public static BlockPalette readPaletteFrom(Map<Item, Integer> items, LevelReader level)
	{
		SimpleWeightedRandomList.Builder<BlockState> builder = SimpleWeightedRandomList.builder();
		for (var entry : items.entrySet())
		{
			var opState = getStateForItem(entry.getKey(), level, false);
			if (opState.isPresent())
				builder.add(opState.get(), entry.getValue());
		}
		return BlockPalette.of(builder.build());
	}

	public static ItemStack getItemForBlock(BlockState state, LevelReader level)
	{
		Block block = state.getBlock();
		ItemStack stack;
		if (block instanceof LiquidBlock liquidBlock)
			stack = liquidBlock.fluid.getBucket().getDefaultInstance();
		else
			stack = block.asItem().getDefaultInstance();

		if (stack.is(Items.AIR))
			stack = Items.BUCKET.getDefaultInstance();

		return stack.isItemEnabled(level.enabledFeatures()) ? stack : ItemStack.EMPTY;
	}

	public static Optional<BlockState> getStateForItem(Item item, LevelReader level, boolean includeAir)
	{
		BlockState state = null;
		if (item instanceof BlockItem blockItem)
			state = blockItem.getBlock().defaultBlockState();
		else if (item instanceof BucketItem bucket)
			state = bucket.content.defaultFluidState().createLegacyBlock();
		else if (item instanceof SolidBucketItem solidBucket)
			state = solidBucket.getBlock().defaultBlockState();
		else if (includeAir)
			state = Blocks.AIR.defaultBlockState();

		return Optional.ofNullable(state != null && state.getBlock().isEnabled(level.enabledFeatures()) ? state : null);
	}

	public static void clearPalette(ItemStack stack)
	{
		stack.remove(SGRegistry.DataComponents.BLOCK_PALETTE);
	}

	public static <T> T getProperty(ItemStack stack, ToolModeProperty<T> property)
	{
		BuildingToolProperties props = stack.get(SGRegistry.DataComponents.BUILDING_TOOL_PROPERTIES);
		if (props != null)
		{
			String key = property.getKey();
			if (props.contains(key))
			{
				return property.read(props.get(key));
			}
		}
		return property.getDefaultValue();
	}

	public static <T> void setProperty(ItemStack stack, ToolModeProperty<T> property, T value)
	{
		BuildingToolProperties props = stack.getOrDefault(SGRegistry.DataComponents.BUILDING_TOOL_PROPERTIES, BuildingToolProperties.DEFAULT);
		props = props.withValue(property.getKey(), property.write(value));
		stack.set(SGRegistry.DataComponents.BUILDING_TOOL_PROPERTIES, props);
	}

	public static int getReachDistanceModifier(ItemStack stack)
	{
		return stack.getOrDefault(SGRegistry.DataComponents.BUILDING_TOOL_REACH_MODIFIER, 0);
	}

	public static int setReachDistanceModifier(ItemStack stack, int reachDistanceModifier)
	{
		reachDistanceModifier = Mth.clamp(reachDistanceModifier, ToolModeProperty.REACH_DISTANCE.min(), ToolModeProperty.REACH_DISTANCE.max());
		stack.set(SGRegistry.DataComponents.BUILDING_TOOL_REACH_MODIFIER, reachDistanceModifier);
		return reachDistanceModifier;
	}

	public static boolean causesBlockUpdates(ItemStack stack)
	{
		return stack.getOrDefault(SGRegistry.DataComponents.BUILDING_TOOL_CAUSE_BLOCK_UPDATES, true);
	}

	public static void setCausesBlockUpdates(ItemStack stack, boolean causeBlockUpdates)
	{
		stack.set(SGRegistry.DataComponents.BUILDING_TOOL_CAUSE_BLOCK_UPDATES, causeBlockUpdates);
	}

	@Nullable
	public static CornerType getSelectedCorner(ItemStack stack)
	{
		return stack.get(SGRegistry.DataComponents.CORNER_TYPE);
	}

	public static void setSelectedCorner(ItemStack stack, @Nullable CornerType cornerType)
	{
		if (cornerType == null)
			stack.remove(SGRegistry.DataComponents.CORNER_TYPE);
		else
			stack.set(SGRegistry.DataComponents.CORNER_TYPE, cornerType);
	}

	public static float getSelectedCornerDistance(ItemStack stack)
	{
		return stack.getOrDefault(SGRegistry.DataComponents.BUILDING_TOOL_CORNER_DIST, 3.0F);
	}

	public static float setSelectedCornerDistance(ItemStack stack, float cornerDistance)
	{
		cornerDistance = Mth.clamp(cornerDistance, 1, ToolModeProperty.REACH_DISTANCE.max());
		stack.set(SGRegistry.DataComponents.BUILDING_TOOL_CORNER_DIST, cornerDistance);
		return cornerDistance;
	}

	public static boolean isMovingBounds(ItemStack stack)
	{
		return stack.getOrDefault(SGRegistry.DataComponents.BUILDING_TOOL_MOVING_BOUNDS, false);
	}

	public static void setMovingBounds(ItemStack stack, boolean moveBounds)
	{
		stack.set(SGRegistry.DataComponents.BUILDING_TOOL_MOVING_BOUNDS, moveBounds);
	}

	// Only used client side
	private static BoundingBox lastClipboardRequestBounds = BoundingBox.infinite();
	private static long lastClipboardRequestTime = 0;

	@Nullable
	public static CapturedBlocks getCapturedBlocks(ItemStack stack, Level level, Player player)
	{
		CloneRegion cloneRegion = stack.get(SGRegistry.DataComponents.BUILDING_TOOL_CAPTURED_BLOCKS);
		if (cloneRegion != null)
		{
			BoundingBox bounds = cloneRegion.bounds();
			CapturedBlocks captured = StructureGelMod.proxy.getBuildingToolData(level.getServer(), cloneRegion).getCapturedBlocks(bounds, cloneRegion.playerName());
			if (level.isClientSide && captured == null)
			{
				// Only request one time a second (1000ms), or if we request a different clipboard. This is to prevent spamming large packets.
				long time = System.currentTimeMillis();
				if (!lastClipboardRequestBounds.equals(bounds) || time - lastClipboardRequestTime > 1000 || lastClipboardRequestTime == 0)
				{
					lastClipboardRequestTime = time;
					lastClipboardRequestBounds = bounds;
					SGPacketHandler.sendToServer(new RequestClipboardPacket(cloneRegion));
				}
			}
			return captured;
		}
		return null;
	}

	public static void setCapturedBlocks(ItemStack stack, Level level, Player player, @Nullable Pair<BoundingBox, CapturedBlocks> clipboard)
	{
		if (level instanceof ServerLevel serverLevel)
		{
			if (clipboard == null)
			{
				stack.remove(SGRegistry.DataComponents.BUILDING_TOOL_CAPTURED_BLOCKS);
			}
			else
			{
				BoundingBox bounds = clipboard.getFirst();
				CapturedBlocks captured = clipboard.getSecond();
				String owner = player.getGameProfile().getName();
				BuildingToolPlayerData data = BuildingToolPlayerData.get(serverLevel, owner);
				data.addToClipboard(bounds, captured);

				stack.set(SGRegistry.DataComponents.BUILDING_TOOL_CAPTURED_BLOCKS, new CloneRegion(bounds, level.dimension(), owner));
			}
		}
	}
}
