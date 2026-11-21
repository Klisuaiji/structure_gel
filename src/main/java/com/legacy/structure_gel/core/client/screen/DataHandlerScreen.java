package com.legacy.structure_gel.core.client.screen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.legacy.structure_gel.api.data_handler.DataHandlerType;
import com.legacy.structure_gel.api.registry.StructureGelRegistries;
import com.legacy.structure_gel.core.block.DataHandlerBlock;
import com.legacy.structure_gel.core.block_entity.DataHandlerBlockEntity;
import com.legacy.structure_gel.core.block_entity.DataHandlerBlockEntity.RawHandler;
import com.legacy.structure_gel.core.client.SGSprites;
import com.legacy.structure_gel.core.client.widget.DataParamsList;
import com.legacy.structure_gel.core.client.widget.SuggestionEditBox;
import com.legacy.structure_gel.core.client.widget.TooltipSpriteButton;
import com.legacy.structure_gel.core.network.SGPacketHandler;
import com.legacy.structure_gel.core.network.c_to_s.UpdateDataHandlerPacket;
import com.legacy.structure_gel.core.util.SGText;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.random.Weight;
import net.minecraft.world.phys.Vec3;

public class DataHandlerScreen extends Screen
{
	private static final String APPLY_AT_HEIGHTMAP_KEY = "gui.structure_gel.data_handler.apply_at_heightmap";
	private final List<RawHandler> handlers = new ArrayList<>();
	private final List<Map<ResourceLocation, List<DataParamsList.Entry>>> dataParamsCache = new ArrayList<>();
	private int visibleIndex = 0;
	private final DataHandlerBlockEntity dataHandler;
	private boolean useGravity = false, waterlogged = false, connectToBlocks = false, markPostProcessing = false;
	private SuggestionEditBox typeEdit;
	private DataParamsList dataParamsList;
	public EditBox weightEdit, nameEdit, xOffsetEdit, yOffsetEdit, zOffsetEdit;
	public Button doneButton, generateButton, nextButton, prevButton;
	public TooltipSpriteButton toggleGravityButton, toggleWaterloggedButton, toggleConnectToBlocksButton,
			togglePostProcessingButton;

	public DataHandlerScreen(DataHandlerBlockEntity dataHandler)
	{
		super(Component.empty());
		this.dataHandler = dataHandler;
		this.handlers.addAll(dataHandler.getHandlers().unwrap());
		this.useGravity = dataHandler.useGravity();
		this.markPostProcessing = dataHandler.shouldMarkPostProcessing();
		this.waterlogged = dataHandler.getBlockState().getValue(DataHandlerBlock.WATERLOGGED);
		this.connectToBlocks = dataHandler.getBlockState().getValue(DataHandlerBlock.CONNECT_TO_BLOCKS);

		for (int i = 0; i < this.handlers.size(); i++)
			this.dataParamsCache.add(new HashMap<>());

		// There should always be at least one
		if (this.handlers.isEmpty())
			this.handlers.add(RawHandler.EMPTY);
		if (this.dataParamsCache.isEmpty())
			this.dataParamsCache.add(new HashMap<>());
	}

	@Override
	public void tick()
	{
		this.dataParamsList.tick();

		this.doneButton.active = this.allowDoneButton();
		this.generateButton.active = this.allowGenerateButton();
		boolean allValid = this.generateButton.active && this.generateButton.active;
		this.prevButton.active = this.visibleIndex > 0 && (this.typeEdit.getValue().isEmpty() || allValid);
		this.nextButton.active = allValid;
	}

	private void onDone(UpdateDataHandlerPacket.Mode mode)
	{
		this.beforePageChange();
		this.sendToServer(mode);
		this.minecraft.setScreen((Screen) null);
	}

	private void onCancel()
	{
		this.minecraft.setScreen((Screen) null);
	}

	private void sendToServer(UpdateDataHandlerPacket.Mode mode)
	{
		Vec3 offsetVec;
		try
		{
			offsetVec = new Vec3(Double.valueOf(this.xOffsetEdit.getValue()), Double.valueOf(this.yOffsetEdit.getValue()), Double.valueOf(this.zOffsetEdit.getValue()));
		}
		catch (NumberFormatException e)
		{
			offsetVec = Vec3.ZERO;
		}
		SGPacketHandler.sendToServer(new UpdateDataHandlerPacket(mode, this.dataHandler.getBlockPos(), this.handlers, this.nameEdit.getValue(), this.useGravity, this.waterlogged, offsetVec, this.connectToBlocks, this.markPostProcessing));
	}

	@Override
	public void onClose()
	{
		this.onCancel();
	}

	@Override
	protected void init()
	{
		int centerX = this.width / 2;
		int centerY = this.height / 2;

		int buttonHeight = 20;
		this.typeEdit = new SuggestionEditBox(this.font, centerX - 152, 25, 215, buttonHeight, SGText.TYPE_LABEL, 10, StructureGelRegistries.DATA_HANDLER_TYPE.keySet());
		this.typeEdit.setMaxLength(128);
		this.typeEdit.setResponder(s -> this.updateWidgets());
		this.addWidget(this.typeEdit);

		this.weightEdit = new EditBox(this.font, this.typeEdit.getX() + this.typeEdit.getWidth() + 7, this.typeEdit.getY(), 303 - this.typeEdit.getWidth() - 6, this.typeEdit.getHeight(), Component.empty());
		this.weightEdit.setMaxLength(8);
		this.weightEdit.setValue("" + 1);
		this.weightEdit.setResponder(s -> this.updateWidgets());
		this.addRenderableWidget(this.weightEdit);

		this.dataParamsList = new DataParamsList(this, this.minecraft, null);
		this.addWidget(this.dataParamsList);

		int totalRowWidth = 300;
		int bottomRowStart = centerX - totalRowWidth / 2;
		int bottomRowIndex = 0;
		int bottomRowWidth = totalRowWidth / 3;
		this.doneButton = this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button ->
		{
			this.onDone(UpdateDataHandlerPacket.Mode.SAVE);
		}).pos(bottomRowStart + bottomRowWidth * bottomRowIndex++, this.height - 30).size(bottomRowWidth, buttonHeight).build());

		this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button ->
		{
			this.onCancel();
		}).pos(bottomRowStart + bottomRowWidth * bottomRowIndex++, this.doneButton.getY()).size(bottomRowWidth, buttonHeight).build());

		this.generateButton = this.addRenderableWidget(Button.builder(Component.translatable("jigsaw_block.generate"), button ->
		{
			this.onDone(UpdateDataHandlerPacket.Mode.GENERATE);
		}).pos(bottomRowStart + bottomRowWidth * bottomRowIndex++, this.doneButton.getY()).size(bottomRowWidth, buttonHeight).build());

		int nextButtonWidth = 16;
		int nextButtonHeight = 32;
		int nextButtonXOffset = 170;
		this.prevButton = this.addRenderableWidget(new TooltipSpriteButton(centerX - (nextButtonXOffset + nextButtonWidth), centerY - (nextButtonHeight / 2), nextButtonWidth, nextButtonHeight, SGSprites.LEFT_ARROW, button ->
		{
			this.previous();
		}));

		this.nextButton = this.addRenderableWidget(new TooltipSpriteButton(centerX + nextButtonXOffset, centerY - (nextButtonHeight / 2), nextButtonWidth, nextButtonHeight, SGSprites.RIGHT_ARROW, button ->
		{
			this.next();
		}));

		int imageButtonWidth = 20;
		var propertiesButton = new TooltipSpriteButton(this.typeEdit.getX() - 35, this.typeEdit.getY() + 1, imageButtonWidth, imageButtonWidth, SGSprites.SETTINGS, button ->
		{
			this.toggleProperties();
		});
		propertiesButton.setTooltip(Tooltip.create(SGText.PROPERTIES_LABEL));
		this.addRenderableWidget(propertiesButton);

		int toggleButtonSpacing = imageButtonWidth + 5;
		int toggleButtonX = this.typeEdit.getX();

		this.toggleGravityButton = new TooltipSpriteButton(toggleButtonX, propertiesButton.getY(), imageButtonWidth, imageButtonWidth, () -> this.useGravity ? SGSprites.USE_GRAVITY_TRUE : SGSprites.USE_GRAVITY_FALSE, button ->
		{
			this.toggleGravity();
		});
		this.toggleGravityButton.setTooltip(Tooltip.create(Component.translatable(APPLY_AT_HEIGHTMAP_KEY, this.useGravity)));
		this.toggleWidget(this.toggleGravityButton, false);
		this.addRenderableWidget(this.toggleGravityButton);
		toggleButtonX += toggleButtonSpacing;

		this.toggleWaterloggedButton = new TooltipSpriteButton(toggleButtonX, propertiesButton.getY(), imageButtonWidth, imageButtonWidth, () -> this.waterlogged ? SGSprites.WATERLOGGED_TRUE : SGSprites.WATERLOGGED_FALSE, button ->
		{
			this.toggleWaterlogged();
		});
		this.toggleWaterloggedButton.setTooltip(Tooltip.create(Component.translatable(SGText.WATERLOGGED_KEY, this.waterlogged)));
		this.toggleWidget(this.toggleWaterloggedButton, false);
		this.addRenderableWidget(this.toggleWaterloggedButton);
		toggleButtonX += toggleButtonSpacing;

		this.toggleConnectToBlocksButton = new TooltipSpriteButton(toggleButtonX, propertiesButton.getY(), imageButtonWidth, imageButtonWidth, () -> this.connectToBlocks ? SGSprites.CONNECT_TO_BLOCKS_TRUE : SGSprites.CONNECT_TO_BLOCKS_FALSE, button ->
		{
			this.toggleConnectToBlocks();
		});
		this.toggleConnectToBlocksButton.setTooltip(Tooltip.create(Component.translatable(SGText.CONNECT_TO_BLOCKS_KEY, this.connectToBlocks)));
		this.toggleWidget(this.toggleConnectToBlocksButton, false);
		this.addRenderableWidget(this.toggleConnectToBlocksButton);
		toggleButtonX += toggleButtonSpacing;

		this.togglePostProcessingButton = new TooltipSpriteButton(toggleButtonX, propertiesButton.getY(), imageButtonWidth, imageButtonWidth, () -> this.markPostProcessing ? SGSprites.POST_PROCESSING_TRUE : SGSprites.POST_PROCESSING_FALSE, button ->
		{
			this.togglePostProcessing();
		});
		this.togglePostProcessingButton.setTooltip(Tooltip.create(Component.translatable(SGText.MARK_POST_PROCESSING_KEY, this.markPostProcessing)));
		this.toggleWidget(this.togglePostProcessingButton, false);
		this.addRenderableWidget(this.togglePostProcessingButton);
		toggleButtonX += toggleButtonSpacing;

		this.nameEdit = new EditBox(this.font, this.typeEdit.getX(), this.typeEdit.getY() + 50, this.typeEdit.getWidth(), this.typeEdit.getHeight(), Component.empty());
		Component name = this.dataHandler.getCustomName();
		if (name != null)
			this.nameEdit.setValue(name.getString());
		this.toggleWidget(this.nameEdit, false);
		this.addWidget(this.nameEdit);

		Vec3 offset = this.dataHandler.getOffset();
		int offsetEditWidth = 30;
		int offsetEditSpacing = 20;
		this.xOffsetEdit = new EditBox(this.font, this.nameEdit.getX() + 10, this.nameEdit.getY() + 40, offsetEditWidth, this.typeEdit.getHeight(), Component.empty());
		this.xOffsetEdit.setValue(Double.toString(offset.x()));
		this.toggleWidget(this.xOffsetEdit, false);
		this.xOffsetEdit.setTooltip(Tooltip.create(SGText.LEFT_LABEL));
		this.addWidget(this.xOffsetEdit);

		this.yOffsetEdit = new EditBox(this.font, this.xOffsetEdit.getX() + this.xOffsetEdit.getWidth() + offsetEditSpacing, this.xOffsetEdit.getY(), offsetEditWidth, this.typeEdit.getHeight(), Component.empty());
		this.yOffsetEdit.setValue(Double.toString(offset.y()));
		this.toggleWidget(this.yOffsetEdit, false);
		this.yOffsetEdit.setTooltip(Tooltip.create(SGText.UP_LABEL));
		this.addWidget(this.yOffsetEdit);

		this.zOffsetEdit = new EditBox(this.font, this.yOffsetEdit.getX() + this.yOffsetEdit.getWidth() + offsetEditSpacing, this.yOffsetEdit.getY(), offsetEditWidth, this.typeEdit.getHeight(), Component.empty());
		this.zOffsetEdit.setValue(Double.toString(offset.z()));
		this.toggleWidget(this.zOffsetEdit, false);
		this.zOffsetEdit.setTooltip(Tooltip.create(SGText.FORWARD_LABEL));
		this.addWidget(this.zOffsetEdit);

		this.updateWidgets();
		this.injectData();
	}

	private void toggleProperties()
	{
		boolean active = !this.nameEdit.visible;

		// Default options
		toggleWidget(this.typeEdit, !active);
		toggleWidget(this.weightEdit, !active);
		toggleWidget(this.dataParamsList, !active);
		this.prevButton.visible = !active;
		this.nextButton.visible = !active;

		// Properties
		toggleWidget(this.nameEdit, active);

		toggleWidget(this.xOffsetEdit, active);
		toggleWidget(this.yOffsetEdit, active);
		toggleWidget(this.zOffsetEdit, active);

		toggleWidget(this.toggleGravityButton, active);
		toggleWidget(this.toggleWaterloggedButton, active);
		toggleWidget(this.toggleConnectToBlocksButton, active);
		toggleWidget(this.togglePostProcessingButton, active);

	}

	private void toggleWidget(AbstractWidget widget, boolean active)
	{
		widget.visible = active;
		widget.active = active;
		widget.setFocused(false);
		if (widget instanceof EditBox edit)
			edit.setEditable(active);
	}

	private void toggleGravity()
	{
		this.useGravity = !this.useGravity;
		this.toggleGravityButton.setTooltip(Tooltip.create(Component.translatable(APPLY_AT_HEIGHTMAP_KEY, this.useGravity)));
	}

	private void toggleWaterlogged()
	{
		this.waterlogged = !this.waterlogged;
		this.toggleWaterloggedButton.setTooltip(Tooltip.create(Component.translatable(SGText.WATERLOGGED_KEY, this.waterlogged)));
	}

	private void toggleConnectToBlocks()
	{
		this.connectToBlocks = !this.connectToBlocks;
		this.toggleConnectToBlocksButton.setTooltip(Tooltip.create(Component.translatable(SGText.CONNECT_TO_BLOCKS_KEY, this.connectToBlocks)));
	}

	private void togglePostProcessing()
	{
		this.markPostProcessing = !this.markPostProcessing;
		this.togglePostProcessingButton.setTooltip(Tooltip.create(Component.translatable(SGText.MARK_POST_PROCESSING_KEY, this.markPostProcessing)));
	}

	private void next()
	{
		int size = this.handlers.size();
		if (this.visibleIndex < size)
		{
			this.beforePageChange();
			this.visibleIndex++;
			if (this.visibleIndex == size)
			{
				this.handlers.add(RawHandler.EMPTY);
				this.dataParamsCache.add(new HashMap<>());
			}

			this.updateWidgets();
			this.injectData();
		}
	}

	private void previous()
	{
		if (this.visibleIndex > 0)
		{
			this.beforePageChange();
			this.visibleIndex--;

			this.updateWidgets();
			this.injectData();
		}
	}

	public boolean allowDoneButton()
	{
		return ResourceLocation.tryParse(this.typeEdit.getValue()) != null && this.dataParamsList.isValid() && this.isWeightValid();
	}

	public boolean allowGenerateButton()
	{
		return this.allowDoneButton() && StructureGelRegistries.DATA_HANDLER_TYPE.get(ResourceLocation.tryParse(this.typeEdit.getValue())) != null && this.dataParamsList.isValid() && this.isWeightValid() && !this.nameEdit.isVisible();
	}

	public boolean isWeightValid()
	{
		try
		{
			int weight = Integer.parseInt(this.weightEdit.getValue());
			if (weight < 1)
			{
				return false;
			}
		}
		catch (Exception e)
		{
			return false;
		}
		return true;
	}

	private void beforePageChange()
	{
		// Store current data
		Weight weight = Weight.of(Integer.parseInt(this.weightEdit.getValue()));

		ResourceLocation typeName = ResourceLocation.tryParse(this.typeEdit.getValue());

		LinkedHashMap<String, String> dataMap = new LinkedHashMap<>();
		for (DataParamsList.Entry entry : this.dataParamsList.children())
		{
			dataMap.put(entry.parser.key, entry.getValue());
		}

		if (this.visibleIndex < this.handlers.size())
			this.handlers.set(this.visibleIndex, new RawHandler(weight, typeName, dataMap));
	}

	private void injectData()
	{
		// Move data from current index into the input fields
		RawHandler handler = this.visibleIndex < this.handlers.size() ? this.handlers.get(this.visibleIndex) : RawHandler.EMPTY;

		this.weightEdit.setValue("" + handler.getWeight().asInt());

		ResourceLocation typeName = handler.typeName();
		this.typeEdit.setValue(typeName.getPath().isEmpty() ? "" : typeName.toString());

		LinkedHashMap<String, String> dataMap = handler.dataEntries();
		for (DataParamsList.Entry entry : this.dataParamsList.children())
		{
			String val = dataMap.get(entry.parser.key);
			if (val != null)
				entry.setValue(val);
		}
	}

	private void updateWidgets()
	{
		this.weightEdit.setTextColor(this.isWeightValid() ? SGText.WHITE : SGText.INVALID_TEXT_COLOR);

		String typeStr = this.typeEdit.getValue();
		boolean typeFlag = ResourceLocation.tryParse(this.typeEdit.getValue()) != null;
		this.typeEdit.setTextColor(typeFlag ? SGText.WHITE : SGText.INVALID_TEXT_COLOR);
		this.doneButton.active = this.allowDoneButton();
		this.generateButton.active = this.allowGenerateButton();
		if (!typeFlag)
		{
			this.dataParamsList.setEntries(null);
			return;
		}

		ResourceLocation typeName = ResourceLocation.tryParse(typeStr);
		DataHandlerType<?> type = StructureGelRegistries.DATA_HANDLER_TYPE.get(typeName);

		if (type != null)
		{
			var cache = this.dataParamsCache.get(this.visibleIndex);
			var cachedParams = cache.get(typeName);
			if (cachedParams == null)
			{
				this.dataParamsList.createEntries(type.getDataParser(this.minecraft.level.registryAccess()));
				cache.put(typeName, this.dataParamsList.childrenClone());
			}
			else
			{
				this.dataParamsList.setEntries(cachedParams);
			}
		}
		else
		{
			this.dataParamsList.setEntries(null);
		}

		boolean allValid = this.doneButton.active && this.generateButton.active;
		this.prevButton.active = this.visibleIndex > 0 && (this.typeEdit.getValue().isEmpty() || allValid);
		this.nextButton.active = allValid;
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks)
	{
		super.render(graphics, mouseX, mouseY, partialTicks);

		this.dataParamsList.render(graphics, mouseX, mouseY, partialTicks);

		if (this.typeEdit.isVisible())
		{
			graphics.drawString(this.font, SGText.TYPE_LABEL, this.typeEdit.getX(), this.typeEdit.getY() - 14, SGText.LABEL_COLOR);
			graphics.drawString(this.font, Component.translatable("info.structure_gel.building_tool.property.weight"), this.weightEdit.getX(), this.weightEdit.getY() - 14, SGText.LABEL_COLOR);
			this.typeEdit.render(graphics, mouseX, mouseY, partialTicks);
		}
		else
		{
			graphics.drawString(this.font, SGText.PROPERTIES_LABEL, this.typeEdit.getX(), this.typeEdit.getY() - 14, SGText.LABEL_COLOR);
		}

		if (this.nameEdit.isVisible())
		{
			graphics.drawString(this.font, SGText.DISPLAY_NAME_LABEL, this.nameEdit.getX(), this.nameEdit.getY() - 14, SGText.LABEL_COLOR);
			this.nameEdit.render(graphics, mouseX, mouseY, partialTicks);
		}

		int labelX = 11, labelY = 6;
		if (this.xOffsetEdit.isVisible())
		{
			graphics.drawString(this.font, SGText.OFFSET_LABEL, this.nameEdit.getX(), this.xOffsetEdit.getY() - 14, SGText.LABEL_COLOR);

			// Left
			graphics.drawString(this.font, Component.literal("\u2190"), this.xOffsetEdit.getX() - labelX, this.xOffsetEdit.getY() + labelY, SGText.LABEL_COLOR);
			this.xOffsetEdit.render(graphics, mouseX, mouseY, partialTicks);
		}

		if (this.yOffsetEdit.isVisible())
		{
			// Up
			graphics.drawString(this.font, Component.literal("\u2191"), this.yOffsetEdit.getX() - labelX, this.yOffsetEdit.getY() + labelY, SGText.LABEL_COLOR);
			this.yOffsetEdit.render(graphics, mouseX, mouseY, partialTicks);
		}

		if (this.zOffsetEdit.isVisible())
		{
			// Forward
			graphics.drawString(this.font, Component.literal("\u2199"), this.zOffsetEdit.getX() - labelX, this.zOffsetEdit.getY() + labelY, SGText.LABEL_COLOR);
			this.zOffsetEdit.render(graphics, mouseX, mouseY, partialTicks);
		}
	}

	@Override
	public void renderTransparentBackground(GuiGraphics graphics)
	{
		PoseStack pose = graphics.pose();
		pose.pushPose();
		pose.translate(0, 0, -2);
		super.renderTransparentBackground(graphics);
		pose.popPose();
	}

	public void renderTooltip(GuiGraphics graphics, List<Component> lines, int mouseX, int mouseY)
	{
		graphics.renderTooltip(font, lines, Optional.empty(), mouseX, mouseY);
	}

	@Override
	public void resize(Minecraft mc, int mouseX, int mouseY)
	{
		String type = this.typeEdit.getValue();

		this.init(mc, mouseX, mouseY);
		this.typeEdit.setValue(type);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int clickType)
	{
		if (this.dataParamsList.isActive())
		{
			for (DataParamsList.Entry entry : this.dataParamsList.children())
			{
				// If focused on a widget, attempt its mouse click call so we can click on the hovered suggestion
				if (entry.widget.isFocused() && entry.widget.mouseClicked(mouseX, mouseY, clickType))
				{
					return true;
				}
				else
				{
					entry.widget.setFocused(false);
				}
			}
		}
		return super.mouseClicked(mouseX, mouseY, clickType);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY)
	{
		return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
	}

	@Override
	public boolean keyPressed(int keyCode, int keyB, int modifiers)
	{
		if (this.typeEdit.isActive())
		{
			if (this.typeEdit.isFocused() && this.typeEdit.keyPressed(keyCode, keyB, modifiers))
			{
				return true;
			}
			for (DataParamsList.Entry entry : this.dataParamsList.children())
			{
				if (entry.widget.isFocused() && entry.widget.keyPressed(keyCode, keyB, modifiers))
				{
					return true;
				}
			}
		}
		if (super.keyPressed(keyCode, keyB, modifiers))
		{
			return true;
		}
		if (this.doneButton.active && (keyCode == 257 || keyCode == 335)) // enter keys
		{
			this.onDone(UpdateDataHandlerPacket.Mode.SAVE);
			return true;
		}

		return false;
	}
}
