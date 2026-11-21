package com.legacy.structure_gel.core.client.item_model_properties;

import com.legacy.structure_gel.core.item.building_tool.BuildingToolItem;
import com.legacy.structure_gel.core.item.building_tool.BuildingToolMode;
import com.mojang.serialization.MapCodec;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.select.SelectItemModelProperty;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class BuildingToolModeProperty implements SelectItemModelProperty<BuildingToolMode>
{
	public static final SelectItemModelProperty.Type<BuildingToolModeProperty, BuildingToolMode> TYPE = SelectItemModelProperty.Type.create(MapCodec.unit(new BuildingToolModeProperty()), BuildingToolMode.CODEC);

	@Override
	public BuildingToolMode get(ItemStack stack, ClientLevel level, LivingEntity entity, int seed, ItemDisplayContext context)
	{
		return BuildingToolItem.getMode(stack);
	}

	@Override
	public SelectItemModelProperty.Type<? extends SelectItemModelProperty<BuildingToolMode>, BuildingToolMode> type()
	{
		return TYPE;
	}
}
