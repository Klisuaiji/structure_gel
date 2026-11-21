package com.legacy.structure_gel.core.client.screen.building_tool;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nullable;

import com.legacy.structure_gel.api.client.gui.LayeredScreen;
import com.legacy.structure_gel.core.client.ClientProxy;
import com.legacy.structure_gel.core.client.SGSprites;
import com.legacy.structure_gel.core.client.renderers.BuildingToolRenderer;
import com.legacy.structure_gel.core.client.widget.MultiWidget;
import com.legacy.structure_gel.core.client.widget.PanelSelectionButton;
import com.legacy.structure_gel.core.client.widget.PropertyEditBox;
import com.legacy.structure_gel.core.client.widget.PropertyImageButton;
import com.legacy.structure_gel.core.client.widget.TooltipSpriteButton;
import com.legacy.structure_gel.core.data_components.BlockPalette;
import com.legacy.structure_gel.core.item.building_tool.BuildingToolItem;
import com.legacy.structure_gel.core.item.building_tool.BuildingToolMode;
import com.legacy.structure_gel.core.item.building_tool.BuildingToolModes;
import com.legacy.structure_gel.core.item.building_tool.ToolModeProperty;
import com.legacy.structure_gel.core.item.building_tool.ToolModeProperty.BooleanProperty;
import com.legacy.structure_gel.core.network.SGPacketHandler;
import com.legacy.structure_gel.core.network.bi_directional.UpdateBuildingToolPacket;
import com.legacy.structure_gel.core.network.c_to_s.ActionHistoryPacket;
import com.legacy.structure_gel.core.registry.SGRegistry;
import com.legacy.structure_gel.core.util.SGText;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import com.mojang.math.Axis;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.random.WeightedEntry.Wrapper;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class BuildingToolScreen extends LayeredScreen
{
	protected static final Component GO_BACK_TEXT = Component.translatable("info.structure_gel.building_tool.go_back");
	protected static final String GO_BACK_DESC = "info.structure_gel.building_tool.go_back.description";

	public static final String UNDO_KEY = "info.structure_gel.building_tool.undo";
	protected static final Component UNDO_TEXT = Component.translatable(UNDO_KEY);
	protected static final String UNDO_DESC = "info.structure_gel.building_tool.undo.description";
	public static final String REDO_KEY = "info.structure_gel.building_tool.redo";
	protected static final Component REDO_TEXT = Component.translatable(REDO_KEY);
	protected static final String REDO_DESC = "info.structure_gel.building_tool.redo.description";
	public static final String COPY_KEY = "info.structure_gel.building_tool.copy";
	public static final String PASTE_KEY = "info.structure_gel.building_tool.paste";
	public static final String DELETE_KEY = "info.structure_gel.building_tool.delete";
	protected static final Component SELECT_POS_1_TEXT = Component.translatable("info.structure_gel.building_tool.select_pos_1");
	protected static final String SELECT_POS_1_DESC = "info.structure_gel.building_tool.select_pos_1.description";
	protected static final Component SELECT_POS_2_TEXT = Component.translatable("info.structure_gel.building_tool.select_pos_2");
	protected static final String SELECT_POS_2_DESC = "info.structure_gel.building_tool.select_pos_2.description";
	protected static final Component EDIT_SELECTION_TEXT = Component.translatable("info.structure_gel.building_tool.edit_selection");
	protected static final String EDIT_SELECTION_DESC = "info.structure_gel.building_tool.edit_selection.description";
	protected static final Component BLOCK_PALETTE_TEXT = Component.translatable("info.structure_gel.building_tool.block_palette");
	protected static final String BLOCK_PALETTE_DESC = "info.structure_gel.building_tool.block_palette.description";

	public static final int TEXT_COLOR = 13487565, HOVERED_TEXT_COLOR = 16777215;
	public static final int PROPERTIES_START_Y = 80, PROPERTIES_SPACING_Y = 30;

	private final List<RadialSlice> radialSlices = Util.make(new ArrayList<>(8), list ->
	{
		float d = 22.5F;
		int i = 45;
		list.add(new RadialSlice(360 - d, d, SGSprites.RADIAL_CARDINAL, BuildingToolModes.EXTEND)); // Top
		list.add(new RadialSlice(d, d += i, SGSprites.RADIAL_DIAGONAL, BuildingToolModes.FLOOD));
		list.add(new RadialSlice(d, d += i, SGSprites.RADIAL_CARDINAL, BuildingToolModes.FILL)); // Right
		list.add(new RadialSlice(d, d += i, SGSprites.RADIAL_DIAGONAL, BuildingToolModes.LINE));
		list.add(new RadialSlice(d, d += i, SGSprites.RADIAL_CARDINAL, BuildingToolModes.CLONE)); // Bottom
		list.add(new RadialSlice(d, d += i, SGSprites.RADIAL_DIAGONAL, BuildingToolModes.MOVE));
		list.add(new RadialSlice(d, d += i, SGSprites.RADIAL_CARDINAL, BuildingToolModes.REPLACE)); // Left
		list.add(new RadialSlice(d, d += i, SGSprites.RADIAL_DIAGONAL, BuildingToolModes.SHAPE));
	});

	protected final ItemStack stack;
	protected final InteractionHand hand;
	protected BuildingToolMode mode = BuildingToolModes.NONE;
	protected final Map<ToolModeProperty<?>, Object> properties = new HashMap<>();
	protected Optional<BlockPos> selectedPosA = Optional.empty();
	protected Optional<BlockPos> selectedPosB = Optional.empty();
	protected boolean usedEditSelectionScreen = false;
	protected boolean clearPoses = false;
	private Optional<WeightedRandomList<Wrapper<BlockState>>> palette = Optional.empty();
	protected Optional<ItemStack> selectedStateRender = Optional.empty();
	/**
	 * 0-7: Radial slices
	 * 
	 * -1: None
	 * 
	 * -2: Center
	 */
	protected int hoveredRadial = -1;
	protected int selectedRadial = -1;
	protected int tick = 0;
	protected final List<Renderable> widgetsToRender = new ArrayList<>(2);
	protected final MultiWidget propertiesWidget = new MultiWidget();
	protected PropertyEditBox<Integer> reachDistanceWidget;
	protected PropertyImageButton<ToolModeProperty.BooleanProperty> blockUpdateWidget;

	/**
	 * 0 = BuildingToolScreen
	 * 
	 * 1 = EditSelection
	 */
	public static int lastScreenState = 0;

	public BuildingToolScreen(ItemStack stack, InteractionHand hand)
	{
		super(Component.empty());
		this.stack = stack;
		this.hand = hand;
		this.mode = BuildingToolItem.getMode(stack);
		this.setupProperties(this.mode);

		for (int i = 0; i < this.radialSlices.size(); i++)
		{
			if (this.radialSlices.get(i).mode == this.mode)
			{
				this.selectedRadial = i;
				break;
			}
		}

		this.selectedPosA = BuildingToolItem.getPos(stack, 0);
		this.selectedPosB = BuildingToolItem.getPos(stack, 1);
	}

	@Override
	protected void init()
	{
		if (this.palette.isEmpty())
			this.palette = Optional.of(BuildingToolItem.getPalette(stack, this.minecraft.level).blocks());
		this.widgetsToRender.clear();
		int undoH = 16;
		int undoW = 25;
		int centerX = this.width / 2;
		int centerY = this.height / 2;
		this.widgetsToRender.add(this.addWidget(new TooltipSpriteButton(centerX - 187, centerY - 100, undoW, undoH, SGSprites.UNDO_BUTTON, button -> SGPacketHandler.sendToServer(ActionHistoryPacket.undo(false)), UNDO_TEXT).onTooltip((button, poseStack, mouseX, mouseY) -> this.renderDescription(button, UNDO_TEXT, UNDO_DESC, SGRegistry.Items.BUILDING_TOOL.get().getDefaultInstance().getHoverName(), SGText.keybindString(ClientProxy.UNDO_KEY.get())))));
		this.widgetsToRender.add(this.addWidget(new TooltipSpriteButton(centerX - 158, centerY - 100, undoW, undoH, SGSprites.REDO_BUTTON, button -> SGPacketHandler.sendToServer(ActionHistoryPacket.redo(false)), REDO_TEXT).onTooltip((button, poseStack, mouseX, mouseY) -> this.renderDescription(button, REDO_TEXT, REDO_DESC, SGRegistry.Items.BUILDING_TOOL.get().getDefaultInstance().getHoverName(), SGText.keybindString(ClientProxy.REDO_KEY.get())))));
		this.addWidget(this.propertiesWidget);
		this.setupPropertiesWidgets(this.mode);

		int leftPropY = PROPERTIES_START_Y;
		var reachProp = ToolModeProperty.REACH_DISTANCE;
		String oldReach = this.reachDistanceWidget == null ? Integer.toString(BuildingToolItem.getReachDistanceModifier(this.stack)) : this.reachDistanceWidget.getValue();
		this.reachDistanceWidget = this.addWidget(new PropertyEditBox<>(this.minecraft.font, centerX + 110, centerY - leftPropY, reachProp, BuildingToolItem.getReachDistanceModifier(this.stack)));
		this.reachDistanceWidget.setValue(oldReach);
		this.reachDistanceWidget.onTooltip((box, poseStack, mouseX, mouseY) ->
		{
			this.renderDescription(box, reachProp.getNameComponent(), List.of(Pair.of(ToolModeProperty.RANGE_KEY, new Object[] { reachProp.min().toString(), reachProp.max().toString() }), Pair.of(reachProp.getDescKey(), new Object[0])));
		});
		leftPropY -= PROPERTIES_SPACING_Y;

		var blockUpdateProp = ToolModeProperty.CAUSE_BLOCK_UPDATES;
		BooleanProperty oldBlockUpdate = this.blockUpdateWidget == null ? BooleanProperty.from(BuildingToolItem.causesBlockUpdates(this.stack)) : this.blockUpdateWidget.getSelectedValue();
		this.blockUpdateWidget = this.addWidget(new PropertyImageButton<>(centerX + 110, centerY - leftPropY, SGSprites.BUILDING_TOOL_BUTTON, b ->
		{
		}, blockUpdateProp.getNameComponent(), blockUpdateProp, oldBlockUpdate));
		this.blockUpdateWidget.onTooltip((box, poseStack, mouseX, mouseY) ->
		{
			this.renderDescription(box, blockUpdateProp.getNameComponent(), List.of(Pair.of(blockUpdateProp.getDescKey(), new Object[0])));
		});
		leftPropY -= PROPERTIES_SPACING_Y;

		this.updateRenderStack();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void setupProperties(BuildingToolMode mode)
	{
		this.properties.clear();
		if (BuildingToolItem.getMode(this.stack).equals(this.mode))
		{
			for (ToolModeProperty property : mode.getProperties().values())
				this.properties.put(property, BuildingToolItem.getProperty(this.stack, property));
		}
		else
		{
			for (ToolModeProperty property : mode.getProperties().values())
				this.properties.put(property, property.getDefaultValue());
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void setupPropertiesWidgets(BuildingToolMode mode)
	{
		int centerX = this.width / 2;
		int centerY = this.height / 2;
		this.propertiesWidget.clear();

		// Position selection buttons
		if (mode instanceof BuildingToolMode.ForCorners)
		{
			int setPosW = 20;
			int buttonSpread = setPosW + 8;
			// Set pos 1 button
			this.propertiesWidget.addListener(new TooltipSpriteButton(centerX + 150 - buttonSpread, centerY - 104, setPosW, setPosW, SGSprites.SET_POS_0, button ->
			{
				this.selectedPosA = Optional.ofNullable(this.minecraft.player.blockPosition());
				this.usedEditSelectionScreen = false;
				this.close();
			}, SELECT_POS_1_TEXT).onTooltip((button, poseStack, mouseX, mouseY) -> this.renderDescription(button, SELECT_POS_1_TEXT, SELECT_POS_1_DESC)));

			// Set pos 2 button
			this.propertiesWidget.addListener(new TooltipSpriteButton(centerX + 150, centerY - 104, setPosW, setPosW, SGSprites.SET_POS_1, button ->
			{
				this.selectedPosB = Optional.ofNullable(this.minecraft.player.blockPosition());
				this.usedEditSelectionScreen = false;
				this.close();
			}, SELECT_POS_2_TEXT).onTooltip((button, poseStack, mouseX, mouseY) -> this.renderDescription(button, SELECT_POS_2_TEXT, SELECT_POS_2_DESC)));

			// Edit selection button
			var editButton = new TooltipSpriteButton(centerX + 150 + buttonSpread, centerY - 104, setPosW, setPosW, SGSprites.EDIT_SELECTION, button ->
			{
				this.minecraft.setScreen(new EditSelectionScreen(this));
			}, EDIT_SELECTION_TEXT).onTooltip((button, poseStack, mouseX, mouseY) -> this.renderDescription(button, EDIT_SELECTION_TEXT, EDIT_SELECTION_DESC, SGText.keybindString(this.minecraft.options.keyDrop)));
			this.propertiesWidget.addListener(editButton);
		}

		// Tool mode specifc properties
		int y = PROPERTIES_START_Y;
		for (Map.Entry<String, ToolModeProperty<?>> entry : mode.getProperties().entrySet())
		{
			ToolModeProperty prop = entry.getValue();
			Object currentVal = this.properties.get(prop);
			if (prop instanceof ToolModeProperty.SelectionProp<?> selectionProp && currentVal instanceof StringRepresentable)
			{
				if (currentVal instanceof ToolModeProperty.IPanelSelection panelProp)
				{
					PanelSelectionButton panelButton = new PanelSelectionButton(centerX - 210, centerY - y, SGSprites.BUILDING_TOOL_BUTTON, button ->
					{
						this.properties.put(button.getProperty(), button.getSelectedValue());
					}, prop.getNameComponent(), selectionProp, panelProp);
					panelButton.onTooltip((button, poseStack, mouseX, mouseY) ->
					{
						this.renderDescription(button, prop.getNameComponent(), prop.getDescKey());
					});

					this.propertiesWidget.addListener(panelButton);
					this.propertiesWidget.addListener(1, panelButton.getPanel());
				}
				else
				{
					this.propertiesWidget.addListener(new PropertyImageButton(centerX - 210, centerY - y, SGSprites.BUILDING_TOOL_BUTTON, button ->
					{
						this.properties.put(button.getProperty(), button.getSelectedValue());
					}, prop.getNameComponent(), selectionProp, (StringRepresentable) currentVal).onTooltip((button, poseStack, mouseX, mouseY) ->
					{
						this.renderDescription(button, prop.getNameComponent(), prop.getDescKey());
					}));
				}
			}
			else
			{
				PropertyEditBox editBox = new PropertyEditBox(this.minecraft.font, centerX - 210, centerY - y, prop, currentVal);
				editBox.setResponder(s ->
				{
					this.properties.put(prop, prop.read(s));
				});
				editBox.onTooltip((box, poseStack, mouseX, mouseY) ->
				{
					if (prop instanceof ToolModeProperty.NumberProp numProp)
						this.renderDescription(box, prop.getNameComponent(), List.of(Pair.of(ToolModeProperty.RANGE_KEY, new Object[] { numProp.min().toString(), numProp.max().toString() }), Pair.of(prop.getDescKey(), new Object[0])));
					else
						this.renderDescription(box, prop.getNameComponent(), prop.getDescKey());
				});
				this.propertiesWidget.addListener(editBox);
			}
			y -= PROPERTIES_SPACING_Y;
		}
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
	{
		super.render(graphics, mouseX, mouseY, partialTick);

		PoseStack poseStack = graphics.pose();
		if (lastScreenState == 1)
		{
			this.minecraft.setScreen(new EditSelectionScreen(this));
			return;
		}

		float mixedTick = this.tick + this.minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(true);
		float animationTime = 2.5F;

		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		RenderSystem.enableBlend();
		int centerX = this.width / 2;
		int centerY = this.height / 2;
		double xCoord = (mouseX) - this.width / 2;
		double yCoord = (this.height - mouseY) - this.height / 2;
		// 0 - 180 & 0 - -180
		double theta = Math.atan2(xCoord, yCoord) * (180 / Math.PI);
		if (theta < 0)
			theta = 360 + theta;
		double dist = Math.sqrt(xCoord * xCoord + yCoord * yCoord);

		if (mixedTick < animationTime)
		{
			float scale = mixedTick / animationTime;
			float invscale = 1.0F - scale;
			poseStack.pushPose();
			poseStack.translate(this.width / 2 * invscale, this.height / 2 * invscale, 0);
			poseStack.scale(scale, scale, scale);
		}

		// Radials
		this.hoveredRadial = -1;
		for (int i = this.radialSlices.size() - 1; i > -1; i--)
		{
			RadialSlice slice = this.radialSlices.get(i);
			boolean isCardinal = i % 2 == 0;
			boolean hovered = false;
			float minDegree = slice.minDegree;
			float maxDegree = slice.maxDegree;
			boolean wrapAround = minDegree > maxDegree;
			if (wrapAround ? ((theta > 180 ? theta > minDegree : theta > minDegree - 360) && (theta > 180 ? theta < maxDegree + 360 : theta < maxDegree)) : (theta > minDegree && theta < maxDegree))
			{
				if (dist > 30 && dist < 110)
				{
					hovered = true;
					this.hoveredRadial = i;
				}
			}

			// Radial
			PoseStack pose = graphics.pose();
			pose.pushPose();
			pose.translate(centerX, centerY, 0);
			float animTime = 7.0F;
			float scale = Mth.clamp(mixedTick / animTime, 0F, 1F);
			// Make it bounce
			scale = (float) (Math.sin(scale * (1.57 * 1.5)) * 1.4);
			
			pose.mulPose(Axis.ZP.rotationDegrees(45 * i + (!isCardinal ? -45 : 0)));
			float innerPercent = isCardinal ? 0.8F : 0.6F;
			float scaledPercent = 1.0F - innerPercent;
			Vec3 sliceMove = isCardinal ? new Vec3(0, -60, 0) : new Vec3(50, -50, 0);
			pose.translate(sliceMove.multiply(innerPercent, innerPercent, innerPercent).add(sliceMove.multiply(scaledPercent, scaledPercent, scaledPercent).multiply(scale, scale, scale)));
			int size = isCardinal ? 80 : 100;
			RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
			graphics.blitSprite(RenderType::guiTextured, slice.sprite.get(this.selectedRadial != i, hovered), -size / 2, -size / 2, size, size);
			pose.popPose();
			
			// Icon
			pose.pushPose();
			pose.translate(centerX, centerY, 0);
			float rot = 45 * i;
			pose.mulPose(Axis.ZP.rotationDegrees(rot));
			innerPercent = isCardinal ? 0.5F : 0.3F;
			scaledPercent = 1.0F - innerPercent;
			Vec3 iconMove = new Vec3(0, -84 + 16, 0);
			pose.translate(iconMove.multiply(innerPercent, innerPercent, innerPercent).add(iconMove.multiply(scaledPercent, scaledPercent, scaledPercent).multiply(scale, scale, scale)));
			pose.mulPose(Axis.ZP.rotationDegrees(-rot));
			RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
			if (!(this.hoveredRadial == i || this.selectedRadial == i))
				RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.75F);
			graphics.blit(RenderType::guiTextured, slice.mode.getIconTexture(), -16, -16, 0, 0, 32, 32, 32, 32);
			pose.popPose();
		}

		// Block palette button
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		if (dist < 21)
			this.hoveredRadial = -2;
		graphics.blitSprite(RenderType::guiTextured, SGSprites.RADIAL_CENTER.get(true, dist < 21 && this.mode.hasBlockPalette()), centerX - 20, centerY - 20, 40, 40);

		// Other widgets (undo, redo)
		this.widgetsToRender.forEach(widget ->
		{
			widget.render(graphics, mouseX, mouseY, partialTick);
			RenderSystem.enableBlend();
		});

		// Properties
		this.propertiesWidget.render(graphics, mouseX, mouseY, partialTick);
		this.reachDistanceWidget.render(graphics, mouseX, mouseY, partialTick);
		RenderSystem.enableBlend();
		this.blockUpdateWidget.render(graphics, mouseX, mouseY, partialTick);

		if (this.hoveredRadial >= 0)
		{
			var mode = this.radialSlices.get(this.hoveredRadial).mode;
			this.renderDescription(mouseX, mode.getComponent(), mode.getDescKey(), mode.getDescArgs());
		}
		else if (this.hoveredRadial == -2 && this.mode.hasBlockPalette())
		{
			this.renderDescription(mouseX, BLOCK_PALETTE_TEXT, BLOCK_PALETTE_DESC, SGText.keybindString(this.minecraft.options.keyPickItem), BuildingToolItem.getPaletteString(this.getPalette()));
		}

		// Render item in middle
		// Render this last to prevent transparency bugs
		if (this.selectedStateRender.isPresent() && this.mode.hasBlockPalette())
			graphics.renderItem(this.selectedStateRender.get(), centerX - 8, centerY - 8);

		// Tooltip Description
		if (this.descriptionRenderer.isPresent())
		{
			this.descriptionRenderer.get().render(graphics);
			this.descriptionRenderer = Optional.empty();
		}

		if (mixedTick < animationTime)
		{
			poseStack.popPose();
		}
	}

	private Optional<DescriptionRenderer> descriptionRenderer = Optional.empty();

	private void renderDescription(GuiEventListener widget, Component title, String descriptionKey, Object... descriptionArgs)
	{
		this.renderDescription(widget, title, List.of(Pair.of(descriptionKey, descriptionArgs)));
	}

	private void renderDescription(GuiEventListener widget, Component title, List<Pair<String, Object[]>> descriptionKeys)
	{
		this.renderDescription(widget.getRectangle().position().x(), title, descriptionKeys);
	}

	private void renderDescription(int x, Component title, String descriptionKey, Object... descriptionArgs)
	{
		this.renderDescription(x, title, List.of(Pair.of(descriptionKey, descriptionArgs)));
	}

	private void renderDescription(int x, Component title, List<Pair<String, Object[]>> descriptionKeys)
	{
		this.descriptionRenderer = Optional.of(new DescriptionRenderer(this, x, title, descriptionKeys));
	}

	public static class DescriptionRenderer
	{
		private final Screen screen;
		private final Component title;
		private final int x;
		private final List<Pair<String, Object[]>> descriptionKeys;

		public DescriptionRenderer(Screen screen, int x, Component title, List<Pair<String, Object[]>> descriptionKeys)
		{
			this.screen = screen;
			this.x = x;
			this.title = title;
			this.descriptionKeys = descriptionKeys;
		}

		@SuppressWarnings("resource")
		void render(GuiGraphics graphics)
		{
			PoseStack poseStack = graphics.pose();
			poseStack.pushPose();
			int centerX = this.screen.width / 2;
			int centerY = this.screen.height / 2;
			boolean renderRight = this.x - centerX < 110;
			List<FormattedCharSequence> lines = new LinkedList<>();
			lines.add(this.title.getVisualOrderText());
			int remainingWidth = Math.max(0, this.screen.width / 2 - 100 - 15);
			for (var desc : this.descriptionKeys)
			{
				Object[] args = desc.getSecond();
				lines.addAll(Minecraft.getInstance().font.split(SGText.applyKeybindFilter(Component.translatable(desc.getFirst(), args)), remainingWidth));
			}
			graphics.renderTooltip(Minecraft.getInstance().font, lines, renderRight ? centerX + 100 : 0, centerY - 74);
			poseStack.popPose();
		}
	}

	private int itemStackRenderIndex = 0;

	public void updateRenderStack()
	{
		Optional<ItemStack> stack = Optional.empty();
		var statesList = this.getPalette().unwrap();
		if (statesList.isEmpty())
		{
			stack = Optional.of(Items.BUCKET.getDefaultInstance());
		}
		else
		{
			int i = this.itemStackRenderIndex % statesList.size();
			stack = Optional.of(BuildingToolItem.getItemForBlock(statesList.get(i).data(), this.minecraft.level));
		}
		this.selectedStateRender = stack;

		this.itemStackRenderIndex++;
	}

	@Override
	public void tick()
	{
		if (this.tick % 20 == 0)
		{
			this.updateRenderStack();
		}
		this.tick++;
	}

	public Optional<ItemStack> getItemForBlock(String val)
	{
		try
		{
			return Optional.of(BuildingToolItem.getItemForBlock(BlockStateParser.parseForBlock(this.minecraft.level.holderLookup(Registries.BLOCK), val, false).blockState(), this.minecraft.level));
		}
		catch (Exception e)
		{
			return Optional.empty();
		}
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int clickType)
	{
		if (Screen.hasControlDown() && ClientProxy.UNDO_KEY.get().getKey().getValue() == clickType)
		{
			SGPacketHandler.sendToServer(ActionHistoryPacket.undo(true));
			return true;
		}
		if (Screen.hasControlDown() && ClientProxy.REDO_KEY.get().getKey().getValue() == clickType)
		{
			SGPacketHandler.sendToServer(ActionHistoryPacket.redo(true));
			return true;
		}
		if (ClientProxy.BUILDING_TOOL_KEY.get().getKey().getValue() == clickType)
		{
			this.close();
			return true;
		}

		if (this.hoveredRadial == -2 && this.mode.hasBlockPalette())
		{
			if (clickType == 0)
			{
				// If we click this middle, open the select state screen
				playClickSound();
				this.minecraft.setScreen(new EditPaletteScreen(this));
				return true;
			}
			else if (clickType == 2)
			{
				playClickSound();
				this.setPalette(WeightedRandomList.create());
				this.updateRenderStack();
				return true;
			}
		}
		else if (this.hoveredRadial > -1)
		{
			// Handle clicking a radial
			this.setMode(this.hoveredRadial);
			return true;
		}

		return super.mouseClicked(mouseX, mouseY, clickType);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers)
	{
		InputConstants.Key key = InputConstants.Type.KEYSYM.getOrCreate(keyCode);
		boolean pressedEnter = keyCode == 257 || keyCode == 335;

		if (ClientProxy.UNDO_KEY.get().isActiveAndMatches(key))
		{
			SGPacketHandler.sendToServer(ActionHistoryPacket.undo(true));
			return true;
		}
		if (ClientProxy.REDO_KEY.get().isActiveAndMatches(key))
		{
			SGPacketHandler.sendToServer(ActionHistoryPacket.redo(true));
			return true;
		}

		if (this.minecraft.options.keyDrop.isActiveAndMatches(key))
		{
			playClickSound();
			this.minecraft.setScreen(new EditSelectionScreen(this));
		}

		if (this.minecraft.options.keyInventory.isActiveAndMatches(key) || ClientProxy.BUILDING_TOOL_KEY.get().isActiveAndMatches(key) || pressedEnter)
		{
			this.close();
			return true;
		}

		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	public void setMode(int radial)
	{
		BuildingToolMode newMode = this.radialSlices.get(radial).mode;
		playClickSound();
		if (this.mode != newMode)
		{
			this.mode = newMode;
			this.selectedRadial = radial;
			this.setupProperties(this.mode);
			this.setupPropertiesWidgets(this.mode);
		}
		else
		{
			this.close();
		}
	}

	public void close()
	{
		this.minecraft.setScreen(null);
		this.onClose();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public void onClose()
	{
		BuildingToolRenderer.needsUpdated();
		Minecraft mc = this.minecraft;
		LocalPlayer player = mc.player;
		ItemStack stack = this.stack;
		if (stack.is(SGRegistry.Items.BUILDING_TOOL.get()))
		{
			BuildingToolItem.setMode(stack, this.mode);
			for (var entry : this.properties.entrySet())
			{
				ToolModeProperty prop = entry.getKey();
				BuildingToolItem.setProperty(stack, prop, entry.getValue());
			}
			if (this.mode instanceof BuildingToolMode.ForCorners cornerMode)
			{
				if (this.selectedPosA.isPresent())
					cornerMode.setPositionA(player, this.selectedPosA.get(), stack, !this.usedEditSelectionScreen);
				if (this.selectedPosB.isPresent())
					cornerMode.setPositionB(player, this.selectedPosB.get(), stack, !this.usedEditSelectionScreen);
			}
			if (this.clearPoses)
				this.mode.clearPoses(stack, player);
			BlockPalette palette = BlockPalette.of(this.getPalette());
			BuildingToolItem.setPalette(stack, palette);
			player.swing(this.hand);
			int reach = ToolModeProperty.REACH_DISTANCE.read(this.reachDistanceWidget.getValue());
			BuildingToolItem.setReachDistanceModifier(stack, reach);
			boolean causeBlockUpdates = this.blockUpdateWidget.getSelectedValue().value();
			BuildingToolItem.setCausesBlockUpdates(stack, causeBlockUpdates);
			this.mode.onSelect(stack, mc.level, player);
			SGPacketHandler.sendToServer(UpdateBuildingToolPacket.builder().hand(hand).mode(mode).properties(properties).posA(selectedPosA).posB(selectedPosB).clearPoses(clearPoses).palette(palette).reachDistance(reach).causeBlockUpdates(causeBlockUpdates).build());
		}
		lastScreenState = 0;
		super.onClose();
	}

	@Override
	public boolean isPauseScreen()
	{
		return false;
	}

	public static void playClickSound()
	{
		Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
	}

	public WeightedRandomList<Wrapper<BlockState>> getPalette()
	{
		return this.palette.orElse(WeightedRandomList.create());
	}

	public void setPalette(@Nullable WeightedRandomList<Wrapper<BlockState>> palette)
	{
		this.palette = Optional.ofNullable(palette);
	}

	private static record RadialSlice(float minDegree, float maxDegree, WidgetSprites sprite, BuildingToolMode mode)
	{
	}
}
