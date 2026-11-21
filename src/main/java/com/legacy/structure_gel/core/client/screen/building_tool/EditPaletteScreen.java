package com.legacy.structure_gel.core.client.screen.building_tool;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;

import javax.annotation.Nullable;

import com.legacy.structure_gel.api.client.gui.LayeredScreen;
import com.legacy.structure_gel.core.client.ClientProxy;
import com.legacy.structure_gel.core.client.ClientUtil;
import com.legacy.structure_gel.core.client.SGSprites;
import com.legacy.structure_gel.core.client.widget.PropertyEditBox;
import com.legacy.structure_gel.core.client.widget.RenderWidget;
import com.legacy.structure_gel.core.client.widget.TexturedEditBox;
import com.legacy.structure_gel.core.client.widget.TooltipSpriteButton;
import com.legacy.structure_gel.core.data_components.BlockPalette;
import com.legacy.structure_gel.core.item.BlockPaletteItem;
import com.legacy.structure_gel.core.item.building_tool.ToolModeProperty;
import com.legacy.structure_gel.core.network.SGPacketHandler;
import com.legacy.structure_gel.core.network.c_to_s.ExportPalettePacket;
import com.legacy.structure_gel.core.network.c_to_s.RequestRegistryKeysPacket;
import com.legacy.structure_gel.core.registry.SGRegistry;
import com.legacy.structure_gel.core.util.SGText;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Pair;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.commands.arguments.blocks.BlockStateParser.BlockResult;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.util.random.WeightedEntry.Wrapper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

public class EditPaletteScreen extends LayeredScreen
{
	private final BuildingToolScreen parent;
	private static final int MAX_VISIBLE_AT_ONCE = 7;
	private List<PaletteEntryGroup> paletteEntryWidgets = new ArrayList<>();
	private TooltipSpriteButton scrollUpButton, scrollDownButton;
	private int displayIndexStart = 0;
	private int addIndex = 0;
	private int removeCooldown = 0;

	public EditPaletteScreen(BuildingToolScreen parent)
	{
		super(Component.empty());
		this.parent = parent;
	}

	@Override
	protected void init()
	{
		int centerX = this.width / 2;
		int centerY = this.height / 2;

		List<Wrapper<BlockState>> paletteList = this.parent.getPalette().isEmpty() ? SimpleWeightedRandomList.<BlockState>builder().add(Blocks.AIR.defaultBlockState(), 1).build().unwrap() : this.parent.getPalette().unwrap();
		this.paletteEntryWidgets = new ArrayList<>();
		for (int i = 0; i < paletteList.size(); i++)
		{
			var wrapper = paletteList.get(i);
			this.addPaletteEntry(wrapper.data(), wrapper.getWeight().asInt(), false);
		}

		// Add button
		var addButton = this.addRenderableWidget(new TooltipSpriteButton(centerX + 185, centerY - 11, 16, 16, SGSprites.ADD, b -> this.addPaletteEntry(null, 1, true)));
		addButton.setTooltip(Tooltip.create(SGText.ADD_LABEL));

		// Scroll buttons
		this.scrollUpButton = this.addRenderableWidget(new TooltipSpriteButton(centerX + 185, centerY - 11 - 20, 16, 16, SGSprites.UP, b -> this.scrollUp()));
		this.scrollDownButton = this.addRenderableWidget(new TooltipSpriteButton(centerX + 185, centerY - 11 + 20, 16, 16, SGSprites.DOWN, b -> this.scrollDown()));

		// Export button
		var exportButton = this.addRenderableWidget(new TooltipSpriteButton(scrollUpButton.getX(), scrollUpButton.getY() - 30, 16, 16, SGSprites.EXPORT_PALETTE, b ->
		{
			ItemStack paletteItem = SGRegistry.Items.BLOCK_PALETTE.getDefaultInstance();
			BlockPaletteItem.setPalette(paletteItem, BlockPalette.of(getPalette()));
			SGPacketHandler.sendToServer(new ExportPalettePacket(paletteItem));
		}));
		exportButton.setTooltip(Tooltip.create(SGText.EXPORT_LABEL));

		this.repositionPaletteEntries();
	}

	private void addPaletteEntry(@Nullable BlockState state, int weight, boolean reposition)
	{
		int centerX = this.width / 2;
		int centerY = this.height / 2;
		int stateEditWidth = 206;

		int index = this.addIndex++;

		TooltipSpriteButton removeButton = this.addWidget(new TooltipSpriteButton(centerX - 190, centerY, 16, 16, SGSprites.SUBTRACT, b -> this.removePaletteEntry(index)));
		removeButton.setTooltip(Tooltip.create(SGText.REMOVE_LABEL));

		TexturedEditBox stateEdit = this.addWidget(new TexturedEditBox(this.minecraft.font, (centerX - 30) - stateEditWidth / 2, centerY, stateEditWidth, 24, SGSprites.BUILDING_TOOL_BUTTON, BuildingToolScreen.BLOCK_PALETTE_TEXT));
		stateEdit.setSuggestions(this::getBlockSuggestions);
		stateEdit.setMaxLength(300);
		stateEdit.setResponder(s ->
		{
			int color = EditBox.DEFAULT_TEXT_COLOR;
			try
			{
				BlockResult result = BlockStateParser.parseForBlock(this.minecraft.level.holderLookup(Registries.BLOCK), s, false);
				if (!result.blockState().getBlock().isEnabled(this.minecraft.level.enabledFeatures()))
				{
					color = SGText.INVALID_TEXT_COLOR;
				}
			}
			catch (CommandSyntaxException e)
			{
				color = SGText.INVALID_TEXT_COLOR;
			}
			stateEdit.setTextColor(color, color);
		});
		if (state != null)
		{
			stateEdit.setValue(serializeState(state.is(Blocks.VOID_AIR) ? Blocks.AIR.defaultBlockState() : state));
		}

		weight = state == null ? 1 : weight;
		PropertyEditBox<Integer> weightEdit = this.addWidget(new PropertyEditBox<>(this.minecraft.font, centerX + 79, centerY, ToolModeProperty.WEIGHT, weight));

		var display = this.addRenderableOnly(RenderWidget.builder(font, centerX - 163, stateEdit.getY() + 4, 24, 24).setCustomRender((graphics, mouseX, mouseY, partialTick) ->
		{
			Optional<ItemStack> stack = this.parent.getItemForBlock(stateEdit.getValue());
			if (stack.isPresent())
				graphics.renderItem(stack.get(), centerX - 163 + 4, stateEdit.getY() + 4);
		}).build());

		var paletteGroup = new PaletteEntryGroup(index, display, stateEdit, weightEdit, removeButton);
		this.paletteEntryWidgets.add(paletteGroup);

		if (reposition)
		{
			if (this.paletteEntryWidgets.size() > MAX_VISIBLE_AT_ONCE)
				this.scrollDown();
			else
				this.repositionPaletteEntries();
		}
	}

	// Copied from BlockStateParser, but with handling for not including default values
	public static String serializeState(BlockState state)
	{
		StringBuilder strBuilder = new StringBuilder(state.getBlockHolder().unwrapKey().map(key -> key.location().toString()).orElse("air"));
		if (!state.getProperties().isEmpty())
		{
			BlockState defaultState = state.getBlock().defaultBlockState();
			List<String> props = new ArrayList<>();
			for (Entry<Property<?>, Comparable<?>> entry : state.getValues().entrySet())
			{
				// Skip serialization if it's a default value
				if (defaultState.getValue(entry.getKey()).equals(entry.getValue()))
					continue;
				props.add(getPropertyString(entry.getKey(), "=", entry.getValue()));
			}

			if (!props.isEmpty())
			{
				strBuilder.append('[');
				boolean addComma = false;
				for (String s : props)
				{
					if (addComma)
					{
						strBuilder.append(',');
					}
					strBuilder.append(s);
					addComma = true;
				}
				strBuilder.append(']');
			}
		}

		return strBuilder.toString();
	}

	@SuppressWarnings("unchecked")
	public static <T extends Comparable<T>> String getPropertyString(Property<T> prop, String spacer, Comparable<?> val)
	{
		return prop.getName() + spacer + prop.getName((T) val);
	}

	private Collection<String> getBlockSuggestions()
	{
		var keys = ClientUtil.REGISTRY_KEYS.get(Registries.BLOCK);
		if (keys == null)
		{
			SGPacketHandler.sendToServer(new RequestRegistryKeysPacket(Registries.BLOCK));
			return Collections.emptyList();
		}
		else
		{
			return keys.stream().map(r -> r.location().toString()).toList();
		}
	}

	private void removePaletteEntry(int id)
	{
		if (this.removeCooldown > 0)
			return;
		int positionInList = 0;
		for (int i = 0; i < this.paletteEntryWidgets.size(); i++)
		{
			if (this.paletteEntryWidgets.get(i).id == id)
			{
				positionInList = i;
				break;
			}
		}

		var entry = this.paletteEntryWidgets.remove(positionInList);
		this.removeWidget(entry.display);
		this.removeWidget(entry.stateEdit);
		this.removeWidget(entry.weightEdit);
		this.removeWidget(entry.removeButton);

		if (this.displayIndexStart > 0)
			this.scrollUp();
		else
			this.repositionPaletteEntries();

		this.removeCooldown = 2;
	}

	private void repositionPaletteEntries()
	{
		int centerY = this.height / 2;
		int offset = 30;
		int totalVisible = Math.min(this.paletteEntryWidgets.size(), MAX_VISIBLE_AT_ONCE);
		float baseY = centerY - (totalVisible / 2.0F * offset);
		for (var entry : this.paletteEntryWidgets)
			entry.setVisible(false);
		for (int i = this.displayIndexStart; i < this.displayIndexStart + MAX_VISIBLE_AT_ONCE && i < this.paletteEntryWidgets.size(); i++)
		{
			int pos = i - this.displayIndexStart;
			var entry = this.paletteEntryWidgets.get(i);
			entry.setVisible(true);
			entry.move((int) (offset * pos + baseY));
		}

		if (this.paletteEntryWidgets.size() > MAX_VISIBLE_AT_ONCE)
		{
			this.scrollUpButton.active = this.displayIndexStart > 0;
			this.scrollDownButton.active = this.displayIndexStart < this.paletteEntryWidgets.size() - MAX_VISIBLE_AT_ONCE;
		}
		else
		{
			this.scrollDownButton.active = false;
			this.scrollUpButton.active = false;
		}
	}

	private void scrollUp()
	{
		if (this.displayIndexStart > 0)
			this.displayIndexStart--;
		this.repositionPaletteEntries();
	}

	private void scrollDown()
	{
		if (this.displayIndexStart < this.paletteEntryWidgets.size() - MAX_VISIBLE_AT_ONCE)
			this.displayIndexStart++;
		this.repositionPaletteEntries();
	}

	private class PaletteEntryGroup
	{
		final int id;
		final RenderWidget display;
		final TexturedEditBox stateEdit;
		final PropertyEditBox<Integer> weightEdit;
		final TooltipSpriteButton removeButton;
		private boolean visible = true;

		private PaletteEntryGroup(int id, RenderWidget display, TexturedEditBox stateEdit, PropertyEditBox<Integer> weightEdit, TooltipSpriteButton removeButton)
		{
			this.id = id;
			this.display = display;
			this.stateEdit = stateEdit;
			this.weightEdit = weightEdit;
			this.removeButton = removeButton;
		}

		public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
		{
			this.stateEdit.render(graphics, mouseX, mouseY, partialTick);
			this.weightEdit.render(graphics, mouseX, mouseY, partialTick);
			this.removeButton.render(graphics, mouseX, mouseY, partialTick);

			var screen = EditPaletteScreen.this;
			int centerX = screen.width / 2;
			int widgetY = this.stateEdit.getY();

			RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
			RenderSystem.enableBlend();
			graphics.blitSprite(RenderType::guiTextured, SGSprites.BUILDING_TOOL_BUTTON.enabled(), centerX - 163, widgetY, 24, 24);

			Optional<Pair<BlockState, Integer>> parsed = this.parse();
			if (parsed.isPresent())
			{
				BlockState state = parsed.get().getFirst();
				BlockState defaultState = state.getBlock().defaultBlockState();
				StringBuilder tooltip = new StringBuilder();
				tooltip.append(state.getBlock().getName().getString());
				for (var e : state.getValues().entrySet())
				{
					var key = e.getKey();
					var val = e.getValue();
					if (!defaultState.getValue(key).equals(val))
						tooltip.append("\n" + SGText.BULLET_POINT.getString() + getPropertyString(key, " = ", val));
				}
				this.display.setTooltip(Tooltip.create(Component.literal(tooltip.toString()).withStyle(ChatFormatting.GRAY)));
			}
			else
			{
				this.display.setTooltip(null);
			}

			this.display.render(graphics, mouseX, mouseY, partialTick);
		}

		public void setVisible(boolean visible)
		{
			this.display.setVisible(visible);
			this.stateEdit.visible = visible;
			this.weightEdit.visible = visible;
			this.removeButton.visible = visible;
			this.visible = visible;
		}

		public boolean isVisible()
		{
			return this.visible;
		}

		public void move(int newY)
		{
			this.display.setY(newY);
			this.stateEdit.setY(newY);
			this.weightEdit.setY(newY);
			this.removeButton.setY(newY + 4);
		}

		public Optional<Pair<BlockState, Integer>> parse()
		{
			try
			{
				BlockState state = BlockStateParser.parseForBlock(EditPaletteScreen.this.minecraft.level.holderLookup(Registries.BLOCK), this.stateEdit.getValue(), false).blockState();
				int weight = ToolModeProperty.WEIGHT.read(this.weightEdit.getValue());
				return Optional.of(Pair.of(state, weight));
			}
			catch (Exception e)
			{
				// Silently fail.
			}
			return Optional.empty();
		}
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
	{
		super.render(graphics, mouseX, mouseY, partialTick);

		for (int i = this.paletteEntryWidgets.size() - 1; i > -1; i--)
		{
			var entry = this.paletteEntryWidgets.get(i);
			if (entry.isVisible())
				entry.render(graphics, mouseX, mouseY, partialTick);
		}
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY)
	{
		if (super.mouseScrolled(mouseX, mouseY, deltaX, deltaY))
			return true;

		if (deltaY > 0)
		{
			this.scrollUp();
		}
		if (deltaY < 0)
		{
			this.scrollDown();
		}
		return true;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int clickType)
	{
		var passed = super.mouseClicked(mouseX, mouseY, clickType);
		if (!passed)
		{
			// Allow clicking a non-widget to deselect
			this.clearFocus();
		}
		return passed;
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers)
	{
		InputConstants.Key key = InputConstants.Type.KEYSYM.getOrCreate(keyCode);
		int escape = 256;
		int tab = 258;
		int backspace = 259;
		boolean pressedEnter = keyCode == 257 || keyCode == 335;

		for (var entry : this.paletteEntryWidgets)
		{
			if (entry.stateEdit.isFocused())
			{
				if (escape == keyCode)
				{
					entry.stateEdit.setFocused(false);
				}
				if (pressedEnter)
				{
					this.back();
					return true;
				}
				if (tab == keyCode)
				{
					super.keyPressed(keyCode, scanCode, modifiers);
					return true;
				}
				return entry.stateEdit.keyPressed(keyCode, scanCode, modifiers);
			}

			if (entry.weightEdit.isFocused())
			{
				if (escape == keyCode)
				{
					entry.weightEdit.setFocused(false);
				}
				if (pressedEnter)
				{
					this.back();
					return true;
				}
				if (tab == keyCode)
				{
					super.keyPressed(keyCode, scanCode, modifiers);
					return true;
				}
				return entry.weightEdit.keyPressed(keyCode, scanCode, modifiers);
			}
		}

		if (escape == keyCode || this.minecraft.options.keyInventory.isActiveAndMatches(key) || ClientProxy.BUILDING_TOOL_KEY.get().isActiveAndMatches(key) || pressedEnter || backspace == keyCode)
		{
			this.back();
			return true;
		}

		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	private void back()
	{
		this.parent.setPalette(getPalette());
		BuildingToolScreen.playClickSound();
		this.minecraft.setScreen(this.parent);
	}
	
	private SimpleWeightedRandomList<BlockState> getPalette()
	{
		var statesBuilder = SimpleWeightedRandomList.<BlockState>builder();
		for (var entry : this.paletteEntryWidgets)
		{
			Optional<Pair<BlockState, Integer>> opData = entry.parse();
			if (opData.isPresent())
			{
				var data = opData.get();
				if (data.getFirst().getBlock().isEnabled(this.minecraft.level.enabledFeatures()))
					statesBuilder.add(data.getFirst(), data.getSecond());
			}
		}
		return statesBuilder.build();
	}

	@Override
	public void tick()
	{
		this.parent.tick();

		if (this.removeCooldown > 0)
			this.removeCooldown--;
	}

	@Override
	public boolean isPauseScreen()
	{
		return false;
	}

	@Override
	public void changeFocus(ComponentPath path)
	{
		super.changeFocus(path);
	}
}
