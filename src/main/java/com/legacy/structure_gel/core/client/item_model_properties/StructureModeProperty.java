package com.legacy.structure_gel.core.client.item_model_properties;

import com.mojang.serialization.MapCodec;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.select.SelectItemModelProperty;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.state.properties.StructureMode;

public class StructureModeProperty implements SelectItemModelProperty<StructureMode>
{
	public static final SelectItemModelProperty.Type<StructureModeProperty, StructureMode> TYPE = SelectItemModelProperty.Type.create(MapCodec.unit(new StructureModeProperty()), StringRepresentable.fromEnum(StructureMode::values));

	@Override
	public StructureMode get(ItemStack stack, ClientLevel level, LivingEntity entity, int seed, ItemDisplayContext context)
	{
		CustomData beData = stack.get(DataComponents.BLOCK_ENTITY_DATA);
		if (beData != null)
		{
			CompoundTag blockEntityTag = beData.copyTag();
			if (blockEntityTag.contains("mode", Tag.TAG_STRING))
			{
				return StructureMode.valueOf(blockEntityTag.getString("mode"));
			}
		}
		return StructureMode.LOAD;
	}

	@Override
	public SelectItemModelProperty.Type<? extends SelectItemModelProperty<StructureMode>, StructureMode> type()
	{
		return TYPE;
	}
}