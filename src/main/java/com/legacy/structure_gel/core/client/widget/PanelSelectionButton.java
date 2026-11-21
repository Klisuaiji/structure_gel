package com.legacy.structure_gel.core.client.widget;

import java.util.List;

import com.legacy.structure_gel.core.client.SGSprites;
import com.legacy.structure_gel.core.item.building_tool.ToolModeProperty;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class PanelSelectionButton<T extends ToolModeProperty.IPanelSelection> extends PropertyImageButton<T>
{
	private final Panel panel;

	public PanelSelectionButton(int x, int y, WidgetSprites sprites, PropertyImageButton.OnPress<PropertyImageButton<T>> onPress, Component name, ToolModeProperty.SelectionProp<T> property, T currentVal)
	{
		this(x, y, PropertyImageButton.DEFAULT_WIDTH, PropertyImageButton.DEFAULT_HEIGHT, sprites, onPress, name, property, currentVal);
	}

	public PanelSelectionButton(int x, int y, int width, int height, WidgetSprites sprites, PropertyImageButton.OnPress<PropertyImageButton<T>> onPress, Component name, ToolModeProperty.SelectionProp<T> property, T currentVal)
	{
		super(x, y, width, height, sprites, onPress, name, property, currentVal);
		this.panel = new Panel(x + 10, y + 25, Component.literal("selection_panel"), property.getAllValues(), currentVal);
	}

	public Panel getPanel()
	{
		return this.panel;
	}

	private void setValue(int index)
	{
		List<T> vals = this.property.getAllValues();
		index = Mth.clamp(index, 0, vals.size() - 1);
		this.currentValue = vals.get(index);

		this.onPress.onPress(this);
	}

	@Override
	public boolean isHoveredOrFocused()
	{
		return super.isHoveredOrFocused() || this.panel.visible;
	}

	@Override
	public void onPress()
	{
		this.panel.visible = !this.panel.visible;
	}

	public class Panel extends AbstractWidget
	{
		static final int BUTTON_SIZE = 20;
		static final int BUTTON_PADDING = 1;
		static final int BUTTON_COLUMNS = 4;
		final List<T> values;
		final WidgetSprites[] textures;
		final ResourceLocation backgroundTexture;
		final int backgroundWidth, backgroundHeight;

		public Panel(int x, int y, Component name, List<T> values, ToolModeProperty.IPanelSelection panelSelectionProp)
		{
			super(x, y, BUTTON_SIZE, BUTTON_SIZE, name);
			this.values = values;
			this.textures = new WidgetSprites[values.size()];
			for (int i = 0; i < values.size(); i++)
			{
				ResourceLocation enabled = panelSelectionProp.getSpriteFolder().withSuffix("/" + values.get(i).getSerializedName());
				ResourceLocation highlighted = panelSelectionProp.getSpriteFolder().withSuffix("/" + values.get(i).getSerializedName() + SGSprites.HIGHLIGHTED_SUFFIX);
				this.textures[i] = new WidgetSprites(enabled, enabled, highlighted, highlighted);
			}
			this.backgroundTexture = panelSelectionProp.getSpriteFolder().withSuffix("/background");

			int s = BUTTON_SIZE;
			this.width = BUTTON_COLUMNS * s;
			this.height = (Math.max(1, values.size() / BUTTON_COLUMNS) + (values.size() % BUTTON_COLUMNS > 0 ? 1 : 0)) * s;
			s += BUTTON_PADDING;
			this.backgroundWidth = BUTTON_COLUMNS * s;
			this.backgroundHeight = (Math.max(1, values.size() / BUTTON_COLUMNS) + (values.size() % BUTTON_COLUMNS > 0 ? 1 : 0)) * s;

			this.visible = false;
		}

		@Override
		protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
		{
			PoseStack poseStack = graphics.pose();
			poseStack.pushPose();
			poseStack.translate(0, 0, 10);

			int texWidth = BUTTON_SIZE + BUTTON_PADDING;
			graphics.blitSprite(RenderType::guiTextured, this.backgroundTexture, this.getX() - 5, this.getY() - 5, this.backgroundWidth + 9, this.backgroundHeight + 9);
			
			for (int i = 0; i < this.values.size(); i++)
			{
				int x = this.getX() + (i % BUTTON_COLUMNS) * texWidth;
				int y = this.getY() + (i / BUTTON_COLUMNS) * texWidth;
				boolean isHovered = mouseX >= x && mouseX <= x + BUTTON_SIZE && mouseY >= y && mouseY <= y + BUTTON_SIZE;
				graphics.blitSprite(RenderType::guiTextured, this.textures[i].get(true, isHovered), x, y, BUTTON_SIZE, BUTTON_SIZE);
				if (isHovered)
					graphics.renderTooltip(Minecraft.getInstance().font, PanelSelectionButton.this.property.getValueComponent(this.values.get(i)), mouseX, mouseY);
			}

			poseStack.popPose();
		}

		@Override
		protected void updateWidgetNarration(NarrationElementOutput narration)
		{
		}

		@Override
		public void onClick(double mouseX, double mouseY)
		{
			int texWidth = BUTTON_SIZE + BUTTON_PADDING;
			int column = (((int) mouseX - this.getX()) / texWidth);
			int row = (((int) mouseY - this.getY()) / texWidth);
			int index = row * BUTTON_COLUMNS + column;

			PanelSelectionButton.this.setValue(index);

			this.visible = false;
		}
	}
}
