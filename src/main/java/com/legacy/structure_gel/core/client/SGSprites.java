package com.legacy.structure_gel.core.client;

import com.legacy.structure_gel.core.StructureGelMod;

import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.resources.ResourceLocation;

public interface SGSprites
{
	String HIGHLIGHTED_SUFFIX = "_highlighted";
	String DISABLED_SUFFIX = "_disabled";

	ResourceLocation HUD_ITEM_SLOT = sprite(Folder.HUD, "item_slot");

	WidgetSprites BUILDING_TOOL_BUTTON = button(Folder.BUILDING_TOOL, "button");
	WidgetSprites UNDO_BUTTON = button(Folder.BUILDING_TOOL, "undo");
	WidgetSprites REDO_BUTTON = button(Folder.BUILDING_TOOL, "redo");
	WidgetSprites EDIT_SELECTION = button(Folder.BUILDING_TOOL, "edit_selection");
	WidgetSprites SET_POS_0 = button(Folder.BUILDING_TOOL, "set_pos_0");
	WidgetSprites SET_POS_1 = button(Folder.BUILDING_TOOL, "set_pos_1");
	WidgetSprites EXPORT_PALETTE = button(Folder.BUILDING_TOOL, "export_palette");
	WidgetSprites ADD = button(Folder.BUILDING_TOOL, "add");
	WidgetSprites SUBTRACT = button(Folder.BUILDING_TOOL, "subtract");
	WidgetSprites UP = button(Folder.BUILDING_TOOL, "up");
	WidgetSprites DOWN = button(Folder.BUILDING_TOOL, "down");

	WidgetSprites EXPAND_SELECTION = button(Folder.BUILDING_TOOL, "expand_selection");
	WidgetSprites EXIT_SELECTION_EDIT = button(Folder.BUILDING_TOOL, "exit_selection_edit");
	WidgetSprites CLEAR_SELECTION = button(Folder.BUILDING_TOOL, "clear_selection");
	WidgetSprites SELECTION_NORTH = button(Folder.BUILDING_TOOL, "selection_north");
	WidgetSprites SELECTION_SOUTH = button(Folder.BUILDING_TOOL, "selection_south");
	WidgetSprites SELECTION_EAST = button(Folder.BUILDING_TOOL, "selection_east");
	WidgetSprites SELECTION_WEST = button(Folder.BUILDING_TOOL, "selection_west");
	WidgetSprites SELECTION_UP = button(Folder.BUILDING_TOOL, "selection_up");
	WidgetSprites SELECTION_DOWN = button(Folder.BUILDING_TOOL, "selection_down");

	WidgetSprites RADIAL_CENTER = button(Folder.BUILDING_TOOL_RADIAL, "center");
	WidgetSprites RADIAL_CARDINAL = button(Folder.BUILDING_TOOL_RADIAL, "cardinal", "cardinal_selected", "cardinal" + HIGHLIGHTED_SUFFIX);
	WidgetSprites RADIAL_DIAGONAL = button(Folder.BUILDING_TOOL_RADIAL, "diagonal", "diagonal_selected", "diagonal" + HIGHLIGHTED_SUFFIX);
	
	WidgetSprites LEFT_ARROW = button(Folder.DATA_HANDLER, "left_arrow");
	WidgetSprites RIGHT_ARROW = button(Folder.DATA_HANDLER, "right_arrow");
	WidgetSprites SETTINGS = button(Folder.DATA_HANDLER, "settings");
	WidgetSprites USE_GRAVITY_TRUE = button(Folder.DATA_HANDLER, "use_gravity_true");
	WidgetSprites USE_GRAVITY_FALSE = button(Folder.DATA_HANDLER, "use_gravity_false");
	WidgetSprites WATERLOGGED_TRUE = button(Folder.DATA_HANDLER, "waterlogged_true");
	WidgetSprites WATERLOGGED_FALSE = button(Folder.DATA_HANDLER, "waterlogged_false");
	WidgetSprites CONNECT_TO_BLOCKS_TRUE = button(Folder.DATA_HANDLER, "connect_to_blocks_true");
	WidgetSprites CONNECT_TO_BLOCKS_FALSE = button(Folder.DATA_HANDLER, "connect_to_blocks_false");
	WidgetSprites POST_PROCESSING_TRUE = button(Folder.DATA_HANDLER, "post_processing_true");
	WidgetSprites POST_PROCESSING_FALSE = button(Folder.DATA_HANDLER, "post_processing_false");

	private static ResourceLocation sprite(String name)
	{
		return sprite(Folder.NONE, name);
	}

	private static ResourceLocation sprite(Folder folder, String name)
	{
		return StructureGelMod.locate(folder.prefix(name));
	}

	private static WidgetSprites button(Folder folder, String name)
	{
		return button(folder, name, name, name + HIGHLIGHTED_SUFFIX, name + HIGHLIGHTED_SUFFIX);
	}

	private static WidgetSprites button(Folder folder, String enabled, String disabled, String enabledFocused, String disabledFocused)
	{
		return new WidgetSprites(sprite(folder, enabled), sprite(folder, disabled), sprite(folder, enabledFocused), sprite(folder, disabledFocused));
	}

	private static WidgetSprites button(Folder folder, String enabled, String disabled, String enabledFocused)
	{
		return button(folder, enabled, disabled, enabledFocused, disabled);
	}

	static enum Folder
	{
		NONE(""),
		BUILDING_TOOL("building_tool"),
		BUILDING_TOOL_RADIAL("building_tool/radial"),
		DATA_HANDLER("data_handler"),
		HUD("hud");

		final String name;

		Folder(String name)
		{
			this.name = name;
		}

		String prefix(String text)
		{
			if (name.isBlank())
				return text;
			return name + "/" + text;
		}
	}
}
