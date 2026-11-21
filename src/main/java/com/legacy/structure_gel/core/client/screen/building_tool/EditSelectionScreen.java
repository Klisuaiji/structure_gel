package com.legacy.structure_gel.core.client.screen.building_tool;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.legacy.structure_gel.api.client.gui.LayeredScreen;
import com.legacy.structure_gel.core.client.ClientProxy;
import com.legacy.structure_gel.core.client.SGSprites;
import com.legacy.structure_gel.core.client.screen.building_tool.BuildingToolScreen.DescriptionRenderer;
import com.legacy.structure_gel.core.client.widget.PropertyEditBox;
import com.legacy.structure_gel.core.client.widget.TooltipSpriteButton;
import com.legacy.structure_gel.core.item.building_tool.BuildingToolItem;
import com.legacy.structure_gel.core.item.building_tool.SmartBoundingBox;
import com.legacy.structure_gel.core.item.building_tool.ToolModeProperty;
import com.legacy.structure_gel.core.registry.SGRegistry;
import com.legacy.structure_gel.core.util.SGText;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.datafixers.util.Pair;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class EditSelectionScreen extends LayeredScreen
{
	protected static final Component CLEAR_SELECTION_TEXT = Component.translatable("info.structure_gel.building_tool.clear_selection");
	protected static final String CLEAR_SELECTION_DESC = "info.structure_gel.building_tool.clear_selection.description";
	protected static final String EXPAND_FACING_DESC = "info.structure_gel.building_tool.expand_facing.description";
	protected static final String EXPAND_TEXT = "info.structure_gel.building_tool.expand";
	protected static final String EXPAND_DESC = "info.structure_gel.building_tool.expand.description";
	protected static final String SHRINK_TEXT = "info.structure_gel.building_tool.shrink";
	protected static final String SHRINK_DESC = "info.structure_gel.building_tool.shrink.description";

	protected static final Component FACING = Component.translatable("info.structure_gel.building_tool.facing");
	protected static final Component FRONT = Component.translatable("info.structure_gel.building_tool.front");
	protected static final Component BACK = Component.translatable("info.structure_gel.building_tool.back");
	protected static final Component LEFT = Component.translatable("info.structure_gel.building_tool.left");
	protected static final Component RIGHT = Component.translatable("info.structure_gel.building_tool.right");
	protected static final Component TOP = Component.translatable("info.structure_gel.building_tool.top");
	protected static final Component BOTTOM = Component.translatable("info.structure_gel.building_tool.bottom");

	private final BuildingToolScreen parent;
	protected final List<Renderable> widgetsToRender = new ArrayList<>(2);
	private SmartBoundingBox bounds;
	private Vec3i negativePos;
	private Vec3i positivePos;
	private PropertyEditBox<Integer> xEditBox, yEditBox, zEditBox;

	public EditSelectionScreen(BuildingToolScreen parent)
	{
		super(Component.empty());
		this.parent = parent;
		Optional<BlockPos> posA = BuildingToolItem.getPos(this.parent.stack, 0);
		Optional<BlockPos> posB = BuildingToolItem.getPos(this.parent.stack, 1);

		if (posA.isPresent() && posB.isPresent())
		{
			this.bounds = SmartBoundingBox.fromCorners(posA.get(), posB.get());
		}
		else if (posA.isPresent() && posB.isEmpty())
		{
			this.bounds = SmartBoundingBox.fromCorners(posA.get(), posA.get());
		}
		else if (posA.isEmpty() && posB.isPresent())
		{
			this.bounds = SmartBoundingBox.fromCorners(posB.get(), posB.get());
		}
		else
		{
			BlockPos playerPos = Minecraft.getInstance().player.blockPosition();
			this.bounds = SmartBoundingBox.fromCorners(playerPos, playerPos);
		}

		this.negativePos = this.bounds.getMin();
		this.positivePos = this.bounds.getMax();
	}

	@Override
	protected void init()
	{
		this.widgetsToRender.clear();
		int centerX = this.width / 2;
		int centerY = this.height / 2;
		int texX = centerX - 100;
		int texY = centerY - 100;

		int circleWidth = 40;
		int w = 16;

		String shiftKey = SGText.keybindString(this.minecraft.options.keyShift);
		// Back
		this.widgetsToRender.add(this.addWidget(new TooltipSpriteButton(texX + 21, texY + 21, circleWidth, circleWidth, SGSprites.EXIT_SELECTION_EDIT, b -> this.back()).onTooltip((button, poseStack, mouseX, mouseY) -> this.renderDescription(button, BuildingToolScreen.GO_BACK_TEXT, BuildingToolScreen.GO_BACK_DESC, SGText.keybindString(this.minecraft.options.keyDrop)))));

		// Clear
		this.widgetsToRender.add(this.addWidget(new TooltipSpriteButton(texX + 21, texY + 139, circleWidth, circleWidth, SGSprites.CLEAR_SELECTION, b -> this.clearSelection()).onTooltip((button, poseStack, mouseX, mouseY) -> this.renderDescription(button, CLEAR_SELECTION_TEXT, CLEAR_SELECTION_DESC))));

		// Expand facing
		this.widgetsToRender.add(this.addWidget(new TooltipSpriteButton(texX + 80, texY + 80, circleWidth, circleWidth, SGSprites.EXPAND_SELECTION, b -> this.expandFacing()).onTooltip((button, poseStack, mouseX, mouseY) -> this.renderDescription(button, component(EXPAND_TEXT, FACING), EXPAND_FACING_DESC, shiftKey))));

		// Direction expansions
		this.widgetsToRender.add(this.addWidget(new TooltipSpriteButton(texX + 92, texY + 14, w, w, SGSprites.ADD, b -> this.expand(Direction.NORTH)).onTooltip((button, poseStack, mouseX, mouseY) -> this.renderDescription(button, component(EXPAND_TEXT, FRONT), EXPAND_DESC, shiftKey))));
		this.widgetsToRender.add(this.addWidget(new TooltipSpriteButton(texX + 92, texY + 34, w, w, SGSprites.SUBTRACT, b -> this.shrink(Direction.NORTH)).onTooltip((button, poseStack, mouseX, mouseY) -> this.renderDescription(button, component(SHRINK_TEXT, FRONT), SHRINK_DESC, shiftKey))));

		this.widgetsToRender.add(this.addWidget(new TooltipSpriteButton(texX + 92, texY + 171, w, w, SGSprites.ADD, b -> this.expand(Direction.SOUTH)).onTooltip((button, poseStack, mouseX, mouseY) -> this.renderDescription(button, component(EXPAND_TEXT, BACK), EXPAND_DESC, shiftKey))));
		this.widgetsToRender.add(this.addWidget(new TooltipSpriteButton(texX + 92, texY + 150, w, w, SGSprites.SUBTRACT, b -> this.shrink(Direction.SOUTH)).onTooltip((button, poseStack, mouseX, mouseY) -> this.renderDescription(button, component(SHRINK_TEXT, BACK), SHRINK_DESC, shiftKey))));

		this.widgetsToRender.add(this.addWidget(new TooltipSpriteButton(texX + 171, texY + 92, w, w, SGSprites.ADD, b -> this.expand(Direction.EAST)).onTooltip((button, poseStack, mouseX, mouseY) -> this.renderDescription(button, component(EXPAND_TEXT, RIGHT), EXPAND_DESC, shiftKey))));
		this.widgetsToRender.add(this.addWidget(new TooltipSpriteButton(texX + 150, texY + 92, w, w, SGSprites.SUBTRACT, b -> this.shrink(Direction.EAST)).onTooltip((button, poseStack, mouseX, mouseY) -> this.renderDescription(button, component(SHRINK_TEXT, RIGHT), SHRINK_DESC, shiftKey))));

		this.widgetsToRender.add(this.addWidget(new TooltipSpriteButton(texX + 13, texY + 92, w, w, SGSprites.ADD, b -> this.expand(Direction.WEST)).onTooltip((button, poseStack, mouseX, mouseY) -> this.renderDescription(button, component(EXPAND_TEXT, LEFT), EXPAND_DESC, shiftKey))));
		this.widgetsToRender.add(this.addWidget(new TooltipSpriteButton(texX + 34, texY + 92, w, w, SGSprites.SUBTRACT, b -> this.shrink(Direction.WEST)).onTooltip((button, poseStack, mouseX, mouseY) -> this.renderDescription(button, component(SHRINK_TEXT, LEFT), SHRINK_DESC, shiftKey))));

		this.widgetsToRender.add(this.addWidget(new TooltipSpriteButton(texX + 151, texY + 136, w, w, SGSprites.ADD, b -> this.expand(Direction.UP)).onTooltip((button, poseStack, mouseX, mouseY) -> this.renderDescription(button, component(EXPAND_TEXT, TOP), EXPAND_DESC, shiftKey))));
		this.widgetsToRender.add(this.addWidget(new TooltipSpriteButton(texX + 171, texY + 136, w, w, SGSprites.SUBTRACT, b -> this.shrink(Direction.UP)).onTooltip((button, poseStack, mouseX, mouseY) -> this.renderDescription(button, component(SHRINK_TEXT, TOP), SHRINK_DESC, shiftKey))));

		this.widgetsToRender.add(this.addWidget(new TooltipSpriteButton(texX + 151, texY + 180, w, w, SGSprites.ADD, b -> this.expand(Direction.DOWN)).onTooltip((button, poseStack, mouseX, mouseY) -> this.renderDescription(button, component(EXPAND_TEXT, BOTTOM), EXPAND_DESC, shiftKey))));
		this.widgetsToRender.add(this.addWidget(new TooltipSpriteButton(texX + 171, texY + 180, w, w, SGSprites.SUBTRACT, b -> this.shrink(Direction.DOWN)).onTooltip((button, poseStack, mouseX, mouseY) -> this.renderDescription(button, component(SHRINK_TEXT, BOTTOM), SHRINK_DESC, shiftKey))));

		int leftPropY = BuildingToolScreen.PROPERTIES_START_Y;

		ToolModeProperty.NumberProp<Integer> xProp = ToolModeProperty.X_SIZE;
		xEditBox = new PropertyEditBox<>(this.minecraft.font, centerX - 210, centerY - leftPropY, xProp, 0);
		xEditBox.setValue(this.bounds.getXSpan());
		xEditBox.setResponder(s ->
		{
			if (xProp.canRead(s))
			{
				this.positivePos = new Vec3i(this.negativePos.getX() + xProp.read(s) - 1, this.positivePos.getY(), this.positivePos.getZ());
				this.updateBounds(false);
			}
		});
		this.widgetsToRender.add(this.addWidget(xEditBox));
		leftPropY -= BuildingToolScreen.PROPERTIES_SPACING_Y;

		ToolModeProperty.NumberProp<Integer> yProp = ToolModeProperty.Y_SIZE;
		yEditBox = new PropertyEditBox<>(this.minecraft.font, centerX - 210, centerY - leftPropY, yProp, 0);
		yEditBox.setValue(this.bounds.getYSpan());
		yEditBox.setResponder(s ->
		{
			if (yProp.canRead(s))
			{
				this.positivePos = new Vec3i(this.positivePos.getX(), this.negativePos.getY() + yProp.read(s) - 1, this.positivePos.getZ());
				this.updateBounds(false);
			}
		});
		this.widgetsToRender.add(this.addWidget(yEditBox));
		leftPropY -= BuildingToolScreen.PROPERTIES_SPACING_Y;

		ToolModeProperty.NumberProp<Integer> zProp = ToolModeProperty.Z_SIZE;
		zEditBox = new PropertyEditBox<>(this.minecraft.font, centerX - 210, centerY - leftPropY, zProp, 0);
		zEditBox.setValue(this.bounds.getZSpan());
		zEditBox.setResponder(s ->
		{
			if (zProp.canRead(s))
			{
				this.positivePos = new Vec3i(this.positivePos.getX(), this.positivePos.getY(), this.negativePos.getZ() + zProp.read(s) - 1);
				this.updateBounds(false);
			}
		});
		this.widgetsToRender.add(this.addWidget(zEditBox));
		leftPropY -= BuildingToolScreen.PROPERTIES_SPACING_Y;
	}

	private Component component(String key, Object... args)
	{
		return Component.translatable(key, args);
	}

	private void expand(Direction direction)
	{
		direction = rotatedByPlayer(direction);
		if (direction.getAxisDirection() == Direction.AxisDirection.POSITIVE)
			this.positivePos = this.positivePos.offset(direction.getUnitVec3i());
		else
			this.negativePos = this.negativePos.offset(direction.getUnitVec3i());

		if (Screen.hasShiftDown())
		{
			Direction opposite = direction.getOpposite();
			if (opposite.getAxisDirection() == Direction.AxisDirection.POSITIVE)
				this.positivePos = this.positivePos.subtract(opposite.getUnitVec3i());
			else
				this.negativePos = this.negativePos.subtract(opposite.getUnitVec3i());
		}

		this.updateBounds(true);
	}

	private void shrink(Direction direction)
	{
		direction = rotatedByPlayer(direction);
		if (direction.getAxisDirection() == Direction.AxisDirection.POSITIVE)
			this.positivePos = this.positivePos.subtract(direction.getUnitVec3i());
		else
			this.negativePos = this.negativePos.subtract(direction.getUnitVec3i());

		if (Screen.hasShiftDown())
		{
			Direction opposite = direction.getOpposite();
			if (opposite.getAxisDirection() == Direction.AxisDirection.POSITIVE)
				this.positivePos = this.positivePos.offset(opposite.getUnitVec3i());
			else
				this.negativePos = this.negativePos.offset(opposite.getUnitVec3i());
		}

		this.updateBounds(true);
	}

	private Direction rotatedByPlayer(Direction direction)
	{
		if (direction.getAxis() != Direction.Axis.Y)
		{
			Direction facingDir = this.minecraft.player.getDirection();
			return Direction.from2DDataValue((direction.get2DDataValue() + facingDir.get2DDataValue()) % 4).getOpposite();
		}
		return direction;
	}

	private void expandFacing()
	{
		Direction direction = Direction.orderedByNearest(this.minecraft.player)[0];
		if (direction.getAxisDirection() == Direction.AxisDirection.POSITIVE)
			this.positivePos = this.positivePos.offset(direction.getUnitVec3i());
		else
			this.negativePos = this.negativePos.offset(direction.getUnitVec3i());

		if (Screen.hasShiftDown())
		{
			Direction opposite = direction.getOpposite();
			if (opposite.getAxisDirection() == Direction.AxisDirection.POSITIVE)
				this.positivePos = this.positivePos.subtract(opposite.getUnitVec3i());
			else
				this.negativePos = this.negativePos.subtract(opposite.getUnitVec3i());
		}

		this.updateBounds(true);
	}

	private void clearSelection()
	{
		this.parent.clearPoses = true;
		this.close();
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
	{
		super.render(graphics, mouseX, mouseY, partialTick);

		int arrowW = 54;
		int arrowH = 72;
		int topBottomW = 51;
		int topBottomH = 33;
		// front
		this.drawHoverable(graphics, SGSprites.SELECTION_NORTH, mouseX, mouseY, 73, 0, arrowW, arrowH);

		// back
		this.drawHoverable(graphics, SGSprites.SELECTION_SOUTH, mouseX, mouseY, 73, 128, arrowW, arrowH);

		// left
		this.drawHoverable(graphics, SGSprites.SELECTION_WEST, mouseX, mouseY, 0, 73, arrowH, arrowW);

		// right
		this.drawHoverable(graphics, SGSprites.SELECTION_EAST, mouseX, mouseY, 128, 73, arrowH, arrowW);

		// top
		this.drawHoverable(graphics, SGSprites.SELECTION_UP, mouseX, mouseY, 143, 132, topBottomW, topBottomH);

		// bottom
		this.drawHoverable(graphics, SGSprites.SELECTION_DOWN, mouseX, mouseY, 143, 167, topBottomW, topBottomH);

		// Translucent widgets
		this.widgetsToRender.forEach(w ->
		{
			w.render(graphics, mouseX, mouseY, partialTick);
			RenderSystem.enableBlend();
		});

		// Tooltip Description
		if (this.descriptionRenderer.isPresent())
		{
			this.descriptionRenderer.get().render(graphics);
			this.descriptionRenderer = Optional.empty();
		}

		Component component = this.parent.mode.getHudMessage(this.parent.stack);
		if (component != null)
		{
			int length = this.font.width(component);
			graphics.drawString(this.font, component, this.width / 2 - length / 2, this.height / 2 - 112, SGText.WHITE);
		}
	}

	private void drawHoverable(GuiGraphics graphics, WidgetSprites sprites, int mouseX, int mouseY, int x, int y, int width, int height)
	{
		int centerX = this.width / 2;
		int centerY = this.height / 2;
		int texX = centerX - 100;
		int texY = centerY - 100;

		boolean hovered = mouseX >= texX + x && mouseX <= texX + x + width && mouseY >= texY + y && mouseY <= texY + y + height;

		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		RenderSystem.enableBlend();

		graphics.blitSprite(RenderType::guiTextured, sprites.get(true, hovered), texX + x, texY + y, width, height);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int clickType)
	{
		return super.mouseClicked(mouseX, mouseY, clickType);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers)
	{
		InputConstants.Key key = InputConstants.Type.KEYSYM.getOrCreate(keyCode);
		int escape = 256;
		int backspace = 259;
		boolean pressedEnter = keyCode == 257 || keyCode == 335;

		var consumed = super.keyPressed(keyCode, scanCode, modifiers);

		if (!consumed && (backspace == keyCode || this.minecraft.options.keyDrop.isActiveAndMatches(key)))
		{
			this.back();
			BuildingToolScreen.playClickSound();
			return true;
		}
		if (escape == keyCode || this.minecraft.options.keyInventory.isActiveAndMatches(key) || ClientProxy.BUILDING_TOOL_KEY.get().isActiveAndMatches(key) || pressedEnter)
		{
			this.close();
			return true;
		}

		return consumed;
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

	public void close()
	{
		this.onClose();
		this.parent.onClose();
		BuildingToolScreen.lastScreenState = 1;
		this.minecraft.setScreen(null);
	}

	public void back()
	{
		this.onClose();
		BuildingToolScreen.lastScreenState = 0;
		this.minecraft.setScreen(this.parent);
	}

	@Override
	public void onClose()
	{
		SmartBoundingBox bb = updateBounds(false);

		if (this.parent.selectedPosA.isPresent() && this.parent.selectedPosB.isPresent() && !SmartBoundingBox.fromCorners(this.parent.selectedPosA.get(), this.parent.selectedPosB.get()).equals(bb))
			this.parent.usedEditSelectionScreen = true;

		// Update the bounds in the parent screen so that the item is updated server side
		this.parent.selectedPosA = Optional.of(new BlockPos(bb.getPosA()));
		this.parent.selectedPosB = Optional.of(new BlockPos(bb.getPosB()));
	}

	public SmartBoundingBox updateBounds(boolean updateEditBox)
	{
		SmartBoundingBox bb = SmartBoundingBox.from(this.bounds);

		bb.setMin(this.negativePos);
		bb.setMax(this.positivePos);

		// Update the size text boxes
		if (updateEditBox)
		{
			this.xEditBox.setValue(bb.getXSpan());
			this.yEditBox.setValue(bb.getYSpan());
			this.zEditBox.setValue(bb.getZSpan());

		}

		// Update item client only
		ItemStack tool = this.minecraft.player.getItemInHand(this.parent.hand);
		if (tool.is(SGRegistry.Items.BUILDING_TOOL.get()))
		{
			BuildingToolItem.setPos(tool, 0, new BlockPos(bb.getPosA()));
			BuildingToolItem.setPos(tool, 1, new BlockPos(bb.getPosB()));
		}

		return bb;
	}

	@Override
	public void tick()
	{
		this.parent.tick();
	}

	@Override
	public boolean isPauseScreen()
	{
		return false;
	}
}
