package com.legacy.structure_gel.core.client.widget;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.legacy.structure_gel.api.client.gui.LayerWidgetHolder.LayerAwareWidget;
import com.legacy.structure_gel.core.util.SGText;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.neoforged.neoforge.common.util.Lazy;

public class SuggestionEditBox extends EditBox
{
	private final SuggestionComponent<?> suggestions;

	public <T> SuggestionEditBox(Font font, int x, int y, int width, int height, Component name, int maxSuggestionLines, Supplier<Collection<T>> suggestions, Function<T, String> toString)
	{
		super(font, x, y, width, height, name);
		this.suggestions = new SuggestionComponent<>(this, suggestions, toString, maxSuggestionLines);
		this.setResponder(s ->
		{
		});
	}

	public SuggestionEditBox(Font font, int x, int y, int width, int height, Component name, int maxSuggestionLines, Collection<?> suggestions)
	{
		this(font, x, y, width, height, name, maxSuggestionLines, () -> suggestions, Object::toString);
	}

	@Override
	public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks)
	{
		super.renderWidget(graphics, mouseX, mouseY, partialTicks);
		if (this.isFocused() && this.isActive())
		{
			this.suggestions.x = this.getX() - 1;
			this.suggestions.y = this.getY() + +this.getHeight() + 1;
			this.suggestions.render(graphics, mouseX, mouseY, partialTicks);
		}
	}

	@Override
	public void setFocused(boolean isFocused)
	{
		super.setFocused(isFocused);
		if (isFocused)
			this.suggestions.update(this.getValue());
	}

	@Override
	public boolean isMouseOver(double mouseX, double mouseY)
	{
		boolean isOverText = super.isMouseOver(mouseX, mouseY);
		if (this.isFocused() && (isOverText || this.suggestions.isMouseOver(mouseX, mouseY)))
			return true;
		return isOverText;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY)
	{
		if (this.isFocused() && this.suggestions.mouseScrolled(mouseX, mouseY, deltaX, deltaY) && this.isActive())
			return true;
		return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int clickType)
	{
		if (this.isFocused() && this.suggestions.isMouseOver(mouseX, mouseY) && this.isActive())
		{
			return this.suggestions.mouseClicked(mouseX, mouseY, clickType);
		}
		return super.mouseClicked(mouseX, mouseY, clickType);
	}

	@Override
	public boolean keyPressed(int keyCode, int keyB, int modifiers)
	{
		if (this.isActive())
		{
			if (keyCode == 256)
			{
				if (this.isFocused())
				{
					this.setFocused(false);
					return true;
				}
			}
			if (this.suggestions.keyPressed(keyCode, keyB, modifiers))
				return true;
		}
		return super.keyPressed(keyCode, keyB, modifiers);
	}

	@Override
	public void setResponder(Consumer<String> responder)
	{
		super.setResponder(s ->
		{
			responder.accept(s);
			this.suggestions.update(this.getValue());
		});
	}

	@Override
	public boolean isHoveredOrFocused()
	{
		return super.isHoveredOrFocused() && LayerAwareWidget.isTop(this);
	}

	public static class SuggestionComponent<T> implements Renderable, GuiEventListener
	{
		private final Minecraft mc = Minecraft.getInstance();
		private final EditBox parent;
		private int maxLines;
		private final int absoluteMaxLines;
		private final Supplier<Collection<T>> allOptions;
		private final Function<T, String> toStringFunc;
		public int x;
		public int y;
		private int height = 10;
		private int width = 10;
		private List<String> currentOptions = new ArrayList<>();
		private int lineRenderStart = 0;
		private int selectedLine = 0;

		public SuggestionComponent(EditBox parent, Supplier<Collection<T>> suggestions, Function<T, String> toString, int maxLines)
		{
			this.parent = parent;
			this.x = parent.getX() - 1;
			this.y = parent.getY() + parent.getHeight() + 1;
			this.allOptions = suggestions;
			this.toStringFunc = toString;
			this.absoluteMaxLines = maxLines;
			this.maxLines = maxLines;
			this.update("");
		}

		public SuggestionComponent(EditBox parent, Collection<T> suggestions, Function<T, String> toString, int maxLines)
		{
			this(parent, Lazy.of(() -> suggestions), toString, maxLines);
		}

		@Override
		public boolean isFocused()
		{
			return false;
		}

		@Override
		public void setFocused(boolean isFocused)
		{
		}

		public List<String> retrieveAll()
		{
			var all = this.allOptions.get().stream().map(this.toStringFunc).collect(Collectors.toList());
			this.maxLines = Math.min(all.size(), this.absoluteMaxLines);
			return all;
		}

		public void update(String input)
		{
			this.currentOptions.clear();
			List<String> all = this.retrieveAll();

			if (input.isEmpty())
			{
				this.currentOptions.addAll(all);
			}
			else
			{
				all.stream().filter(s -> s.startsWith(input) || (s.contains(":") && s.substring(s.indexOf(":") + 1).startsWith(input))).forEach(this.currentOptions::add);
				all.stream().filter(s -> !this.currentOptions.contains(s)).filter(s -> s.contains(input)).forEach(this.currentOptions::add);
			}
			this.currentOptions.sort(String::compareToIgnoreCase);

			// If the name of a current option is a perfect match or starts with the input string, put it to the top. Fixes issues where inputting "stone" produces "blackstone" as the first option.
			int size = this.currentOptions.size();
			List<String> putToTop = new ArrayList<>();
			for (int i = 0; i < size; i++)
			{
				String s = this.currentOptions.get(i);
				ResourceLocation loc = ResourceLocation.tryParse(s);
				if (loc != null)
				{
					if (loc.getPath().startsWith(input) || loc.getPath().equals(input) || s.equals(input))
						putToTop.add(putToTop.size(), s);
				}
				else
				{
					if (s.startsWith(input))
						putToTop.add(putToTop.size(), s);
				}
			}
			this.currentOptions.removeAll(putToTop);
			this.currentOptions.addAll(0, putToTop);

			// Adjust max render width based on max text length
			int maxWidth = 0;
			for (String op : this.currentOptions)
			{
				int w = this.mc.font.width(op);
				if (w > maxWidth)
					maxWidth = w;
			}
			this.width = maxWidth + 2; // 2 is a horizontal buffer

			int lines = Math.min(this.maxLines, this.currentOptions.size());
			this.height = (this.mc.font.lineHeight * lines) + (lines * 2) + 4; // 2 is space between lines

			this.selectedLine = 0;
			this.lineRenderStart = 0;
		}

		@Nullable
		public String getSelected()
		{
			if (this.selectedLine < this.currentOptions.size() && this.selectedLine > -1)
				return this.currentOptions.get(this.selectedLine);
			return null;
		}

		@Override
		public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks)
		{
			PoseStack poseStack = graphics.pose();
			int size = this.currentOptions.size();
			if (size < 1)
				return;

			if (this.maxLines > 0)
			{
				poseStack.pushPose();
				poseStack.translate(0, 0, 10);
				// Black box
				graphics.fill(this.x, this.y, this.x + this.width, this.y + this.height, FastColor.ARGB32.color(200, 10, 10, 10));

				// Options
				Font font = this.mc.font;
				for (int line = 0; line < this.maxLines; line++)
				{
					int renderLine = line + this.lineRenderStart;
					if (renderLine < size)
					{
						graphics.drawString(font, this.currentOptions.get(renderLine), this.x + 1, this.y + 4 + (line * (font.lineHeight + 2)), renderLine == this.selectedLine ? ChatFormatting.AQUA.getColor() : ChatFormatting.DARK_GREEN.getColor());
					}
				}

				// Scroll bar
				if (size > this.maxLines)
				{
					int startX = this.x + this.width;
					int h = 6;
					int startY = this.y + (int) ((this.selectedLine / (double) size) * (this.height - h));
					graphics.fill(startX, startY, startX + 2, startY + this.mc.font.lineHeight + h, Integer.MAX_VALUE);
				}

				poseStack.popPose();
			}

			// Edit box overlay
			poseStack.pushPose();
			poseStack.translate(0, 0, -10);
			graphics.drawString(this.mc.font, this.getSelected(), this.x + 5, this.y - this.parent.getHeight() / 2 - 5, SGText.TRANSPARENT_GRAY);
			poseStack.popPose();
		}

		@Override
		public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY)
		{
			if (this.currentOptions.size() > 1)
			{
				int dir = deltaY < 0 ? -1 : 1;
				if (dir < 0)
				{
					this.moveSelectorDown();
				}
				else if (dir > 0)
				{
					this.moveSelectorUp();
				}
			}
			return true;
		}

		@Override
		public boolean keyPressed(int keyCode, int keyB, int modifiers)
		{
			if (keyCode == 258) // tab
			{
				if (!this.parent.getValue().isBlank())
				{
					String selectedVal = this.getSelected();
					if (selectedVal != null && !this.parent.getValue().equals(selectedVal))
					{
						this.parent.setValue(selectedVal);
						return true;
					}
				}
			}
			else if (keyCode == 264) // down arrow
			{
				this.moveSelectorDown();
				return true;
			}
			else if (keyCode == 265) // up arrow
			{
				this.moveSelectorUp();
				return true;
			}
			return false;
		}

		@Override
		public boolean mouseClicked(double mouseX, double mouseY, int clickType)
		{
			int clickedLine = (int) ((mouseY - this.y - 2) / (this.mc.font.lineHeight + 2));

			this.selectedLine = Math.min(Math.max(0, clickedLine), this.maxLines - 1) + this.lineRenderStart;
			String selected = this.getSelected();
			if (selected != null)
			{
				this.parent.setValue(selected);
				return true;
			}
			return false;
		}

		@Override
		public boolean isMouseOver(double mouseX, double mouseY)
		{
			return mouseX > this.x && mouseX < this.x + this.width && mouseY > this.y && mouseY < this.y + this.height;
		}

		public void moveSelectorDown()
		{
			if (this.selectedLine < this.currentOptions.size() - 1)
			{
				this.selectedLine++;

				if (this.selectedLine > this.lineRenderStart + this.maxLines - 1)
				{
					this.lineRenderStart++;
				}
			}
			else
			{
				this.selectedLine = 0;
				this.lineRenderStart = 0;
			}
		}

		public void moveSelectorUp()
		{
			if (this.selectedLine > 0)
			{
				this.selectedLine--;

				if (this.selectedLine < this.lineRenderStart)
					this.lineRenderStart--;
			}
			else
			{
				this.selectedLine = this.currentOptions.size() - 1;
				if (this.selectedLine > this.lineRenderStart + this.maxLines - 1)
				{
					this.lineRenderStart = this.currentOptions.size() - this.maxLines;
				}
			}
		}
	}
}