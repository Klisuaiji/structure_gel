package com.legacy.structure_gel.core.client.widget;

import net.minecraft.client.gui.GuiGraphics;

public interface OnTooltip<T>
{
	void onTooltip(T widget, GuiGraphics graphics, int mouseX, int mouseY);
}