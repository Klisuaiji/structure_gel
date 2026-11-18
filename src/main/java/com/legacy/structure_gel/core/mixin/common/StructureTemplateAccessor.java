package com.legacy.structure_gel.core.mixin.common;

import java.util.List;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.Palette;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureEntityInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({StructureTemplate.class})
public interface StructureTemplateAccessor {
   @Accessor("palettes")
   List<Palette> getPalettes();

   @Accessor("entityInfoList")
   List<StructureEntityInfo> getEntityInfoList();

   @Accessor("size")
   Vec3i getSize();
}
