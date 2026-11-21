package com.legacy.structure_gel.core.mixin.common;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.core.Vec3i;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

@Mixin(StructureTemplate.class)
public interface StructureTemplateAccessor
{
	@Accessor("palettes")
	abstract List<StructureTemplate.Palette> getPalettes(); // This is an ArrayList normally

	@Accessor("entityInfoList")
	abstract List<StructureTemplate.StructureEntityInfo> getEntityInfoList(); // This is an ArrayList normally

	@Accessor("size")
	abstract Vec3i getSize();
}
