package com.legacy.structure_gel.core.data_handler.handlers;

import java.util.EnumMap;

import javax.annotation.Nullable;

import com.legacy.structure_gel.api.data_handler.LootTableAlias;
import com.legacy.structure_gel.api.data_handler.handlers.ProbabilityDataHandler;
import com.legacy.structure_gel.api.data_handler.parsing.DataMap;
import com.legacy.structure_gel.api.data_handler.parsing.DataParser;
import com.legacy.structure_gel.api.entity.EntityAccessHelper;
import com.legacy.structure_gel.api.registry.StructureGelRegistries;
import com.legacy.structure_gel.core.SGAccessor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;

public class EntityHandler extends ProbabilityDataHandler<EntityHandler>
{
	public static final DataParser.Factory PARSER = DataParser.of(builder ->
	{
		//addTestFields(builder);

		builder.add("entity", Registries.ENTITY_TYPE, EntityType.PIG);

		builder.add("helmet", (ItemStack) null);
		builder.add("chestplate", (ItemStack) null);
		builder.add("leggings", (ItemStack) null);
		builder.add("boots", (ItemStack) null);
		builder.add("main_hand", (ItemStack) null);
		builder.add("off_hand", (ItemStack) null);

		builder.add("loot", (ResourceLocation) null).setSuggestions(StructureGelRegistries.LOOT_TABLE_ALIAS::keySet, ResourceLocation::toString);

		builder.add("persistent", true);
		builder.add("spawn_chance", 1.0F, 0.0F, 1.0F);

		builder.add("tag", new CompoundTag());
	});

	private final ResourceKey<EntityType<?>> entityType;
	private final EnumMap<EquipmentSlot, ItemStack> equipment = new EnumMap<>(EquipmentSlot.class);
	@Nullable
	private final ResourceLocation loot;
	private final boolean persistent;
	private final CompoundTag tag;

	public EntityHandler(DataMap data)
	{
		super(data, "spawn_chance");
		this.entityType = data.get("entity", Registries.ENTITY_TYPE);

		this.equipment.put(EquipmentSlot.HEAD, data.get("helmet", ItemStack.class));
		this.equipment.put(EquipmentSlot.CHEST, data.get("chestplate", ItemStack.class));
		this.equipment.put(EquipmentSlot.LEGS, data.get("leggings", ItemStack.class));
		this.equipment.put(EquipmentSlot.FEET, data.get("boots", ItemStack.class));
		this.equipment.put(EquipmentSlot.MAINHAND, data.get("main_hand", ItemStack.class));
		this.equipment.put(EquipmentSlot.OFFHAND, data.get("off_hand", ItemStack.class));

		this.loot = data.get("loot", ResourceLocation.class);

		this.persistent = data.get("persistent", Boolean.class);

		this.tag = data.get("tag", CompoundTag.class);
	}

	@Override
	protected void handle(Context context)
	{
		WorldGenLevel level = context.getLevel();
		BlockState dataHandler = context.getDataHandlerState();
		Vec3 pos = context.getPos();

		CompoundTag entityTag = this.tag.copy();
		entityTag.putString("id", this.entityType.location().toString());
		Entity entity = EntityType.loadEntityRecursive(entityTag, level.getLevel(), EntitySpawnReason.STRUCTURE, e ->
		{
			Direction facing = dataHandler.hasProperty(BlockStateProperties.FACING) ? dataHandler.getValue(BlockStateProperties.FACING) : Direction.SOUTH;
			float yRot = facing.getAxis() != Direction.Axis.Y ? facing.toYRot() : e.getYRot();
			e.moveTo(pos.x(), pos.y(), pos.z(), yRot, e.getXRot());

			if (e instanceof HangingEntity)
				SGAccessor.HANGING_ENTITY_SET_DIRECTION.invoke((HangingEntity) e, facing);

			return e;
		});

		if (entity == null)
			return;

		if (entity instanceof Mob)
		{
			Mob mobEntity = (Mob) entity;
			EventHooks.finalizeMobSpawn(mobEntity, level, level.getCurrentDifficultyAt(BlockPos.containing(pos)), EntitySpawnReason.STRUCTURE, null);

			if (this.loot != null)
				EntityAccessHelper.setDeathLootTable(mobEntity, LootTableAlias.getLootTable(this.loot));

			if (this.persistent)
				mobEntity.setPersistenceRequired();
		}

		for (var entry : this.equipment.entrySet())
		{
			ItemStack stack = entry.getValue();
			if (stack != null)
			{
				if (entity instanceof LivingEntity living)
					living.setItemSlot(entry.getKey(), stack);
				if (entity instanceof ItemFrame itemFrame)
					itemFrame.setItem(stack, false);
			}
		}

		level.addFreshEntityWithPassengers(entity);
	}

	// Testing purposes only.
	private static void addTestFields(DataParser.Builder builder)
	{
		builder.add("test_true", true);
		builder.add("test_false", false);

		builder.addEnum("axis_test", Direction.Axis.class, Direction.Axis.Y);
		builder.addEnum("enum_test", EquipmentSlot.class, EquipmentSlot.OFFHAND);

		ItemStack helm = Items.CHAINMAIL_HELMET.getDefaultInstance();
		helm.set(DataComponents.ITEM_MODEL, ResourceLocation.tryParse("stick"));
		helm.set(DataComponents.DAMAGE, 55);
		helm.set(DataComponents.CUSTOM_NAME, Component.literal("oh yeah"));
		CompoundTag customData = new CompoundTag();
		customData.putString("string", "Text");
		customData.putInt("int", 2);
		helm.set(DataComponents.CUSTOM_DATA, CustomData.of(customData));
		builder.add("test_item", helm);
	}
}
