package com.legacy.structure_gel.core.item.building_tool;

import com.legacy.structure_gel.api.registry.SGSimpleRegistry;
import com.legacy.structure_gel.core.StructureGelMod;
import com.legacy.structure_gel.core.item.building_tool.modes.CloneTool;
import com.legacy.structure_gel.core.item.building_tool.modes.ExtendTool;
import com.legacy.structure_gel.core.item.building_tool.modes.FillTool;
import com.legacy.structure_gel.core.item.building_tool.modes.FloodTool;
import com.legacy.structure_gel.core.item.building_tool.modes.LineTool;
import com.legacy.structure_gel.core.item.building_tool.modes.MoveTool;
import com.legacy.structure_gel.core.item.building_tool.modes.NoneTool;
import com.legacy.structure_gel.core.item.building_tool.modes.ReplaceTool;
import com.legacy.structure_gel.core.item.building_tool.modes.ShapeTool;
import net.minecraft.resources.ResourceLocation;

public class BuildingToolModes {
   public static final SGSimpleRegistry<ResourceLocation, BuildingToolMode> REGISTRY = new SGSimpleRegistry<>(
      StructureGelMod.locate("modes"), () -> BuildingToolModes.NONE, null
   );
   public static final BuildingToolMode NONE = register(new NoneTool("none", 0));
   public static final BuildingToolMode REPLACE = register(new ReplaceTool("replace", 1));
   public static final BuildingToolMode FILL = register(new FillTool("fill", 2));
   public static final BuildingToolMode CLONE = register(new CloneTool("clone", 3));
   public static final BuildingToolMode FLOOD = register(new FloodTool("flood", 4));
   public static final BuildingToolMode EXTEND = register(new ExtendTool("extend", 5));
   public static final BuildingToolMode MOVE = register(new MoveTool("move", 6));
   public static final BuildingToolMode LINE = register(new LineTool("line", 7));
   public static final BuildingToolMode SHAPE = register(new ShapeTool("shape", 8));

   public static void init() {
      REGISTRY.init();
   }

   protected static <T extends BuildingToolMode> T register(T mode) {
      REGISTRY.register(mode.name, mode);
      return mode;
   }
}
