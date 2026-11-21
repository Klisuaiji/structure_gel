package com.legacy.structure_gel.core.client;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.legacy.structure_gel.api.block.GelPortalBlock;
import com.legacy.structure_gel.core.SGProxy;
import com.legacy.structure_gel.core.block_entity.DataHandlerBlockEntity;
import com.legacy.structure_gel.core.block_entity.DynamicSpawnerBlockEntity;
import com.legacy.structure_gel.core.capability.level.BuildingToolPlayerData;
import com.legacy.structure_gel.core.client.renderers.BuildingToolRenderer;
import com.legacy.structure_gel.core.client.renderers.StructureBoundsRenderer;
import com.legacy.structure_gel.core.client.screen.DataHandlerScreen;
import com.legacy.structure_gel.core.client.screen.DynamicSpawnerScreen;
import com.legacy.structure_gel.core.client.screen.building_tool.BuildingToolScreen;
import com.legacy.structure_gel.core.data_components.CloneRegion;
import com.legacy.structure_gel.core.registry.SGRegistry;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import net.neoforged.neoforge.common.util.Lazy;

public class ClientProxy extends SGProxy
{
	private boolean shouldViewBounds = false;
	public static final List<Lazy<KeyMapping>> KEY_MAPPINGS = new ArrayList<>(5);
	public static final Lazy<KeyMapping> UNDO_KEY = keyBind(() -> new KeyMapping(BuildingToolScreen.UNDO_KEY, KeyConflictContext.UNIVERSAL, KeyModifier.CONTROL, InputConstants.Type.KEYSYM.getOrCreate(InputConstants.KEY_Z), SGRegistry.Items.BUILDING_TOOL.get().getDescriptionId()));
	public static final Lazy<KeyMapping> REDO_KEY = keyBind(() -> new KeyMapping(BuildingToolScreen.REDO_KEY, KeyConflictContext.UNIVERSAL, KeyModifier.CONTROL, InputConstants.Type.KEYSYM.getOrCreate(InputConstants.KEY_Y), SGRegistry.Items.BUILDING_TOOL.get().getDescriptionId()));
	public static final Lazy<KeyMapping> COPY_KEY = keyBind(() -> new KeyMapping(BuildingToolScreen.COPY_KEY, KeyConflictContext.UNIVERSAL, KeyModifier.CONTROL, InputConstants.Type.KEYSYM.getOrCreate(InputConstants.KEY_C), SGRegistry.Items.BUILDING_TOOL.get().getDescriptionId()));
	public static final Lazy<KeyMapping> PASTE_KEY = keyBind(() -> new KeyMapping(BuildingToolScreen.PASTE_KEY, KeyConflictContext.UNIVERSAL, KeyModifier.CONTROL, InputConstants.Type.KEYSYM.getOrCreate(InputConstants.KEY_V), SGRegistry.Items.BUILDING_TOOL.get().getDescriptionId()));
	public static final Lazy<KeyMapping> DELETE_KEY = keyBind(() -> new KeyMapping(BuildingToolScreen.DELETE_KEY, KeyConflictContext.UNIVERSAL, KeyModifier.NONE, InputConstants.Type.KEYSYM.getOrCreate(InputConstants.KEY_DELETE), SGRegistry.Items.BUILDING_TOOL.get().getDescriptionId()));
	public static final Lazy<KeyMapping> BUILDING_TOOL_KEY = keyBind(() -> new KeyMapping("key.structure_gel.open_building_tool_gui", KeyConflictContext.UNIVERSAL, KeyModifier.NONE, InputConstants.Type.KEYSYM.getOrCreate(InputConstants.KEY_R), SGRegistry.Items.BUILDING_TOOL.get().getDescriptionId()));

	public static GelPortalBlock lastPortal = null;
	
	private static Lazy<KeyMapping> keyBind(Supplier<KeyMapping> key)
	{
		Lazy<KeyMapping> mapping = Lazy.of(key);
		KEY_MAPPINGS.add(mapping);
		return mapping;
	}

	@Override
	public void openDataHandlerScreen(DataHandlerBlockEntity dataHandler)
	{
		Minecraft.getInstance().setScreen(new DataHandlerScreen(dataHandler));
	}

	@Override
	public void openDynamicSpawnerScreen(DynamicSpawnerBlockEntity spawner)
	{
		Minecraft.getInstance().setScreen(new DynamicSpawnerScreen(spawner));
	}

	@Override
	public void openBuildingToolScreen(ItemStack stack, InteractionHand hand)
	{
		Minecraft mc = Minecraft.getInstance();
		if (mc.screen == null)
		{
			mc.player.swing(hand);
			Minecraft.getInstance().setScreen(new BuildingToolScreen(stack, hand));
		}
	}

	@Override
	public void setViewBounds(boolean shouldViewBounds)
	{
		Minecraft mc = Minecraft.getInstance();
		var chat = mc.gui.getChat();
		if (shouldViewBounds)
		{
			if (this.shouldViewBounds)
				chat.addMessage(Component.literal("Refreshing structure bounding box rendering..."));
			else
				chat.addMessage(Component.literal("Enabled structure bounding box rendering"));
		}
		else
		{
			StructureBoundsRenderer.clear();

			if (this.shouldViewBounds)
				chat.addMessage(Component.literal("Disabled structure bounding box rendering"));
		}

		this.shouldViewBounds = shouldViewBounds;
	}

	@Override
	public boolean shouldViewBounds()
	{
		return this.shouldViewBounds;
	}

	@Override
	public void clearToolRenderCache()
	{
		BuildingToolRenderer.clear();
	}

	private static BuildingToolPlayerData buildingToolClientData;

	@Override
	@Nullable
	public BuildingToolPlayerData getBuildingToolData(@Nullable ServerLevel level, String playerName)
	{
		var data = super.getBuildingToolData(level, playerName);
		return data != null ? data : this.clientBuildingToolData();
	}
	
	@Nullable
	public BuildingToolPlayerData getBuildingToolData(@Nullable MinecraftServer server, CloneRegion cloneRegion)
	{
		var data = super.getBuildingToolData(server, cloneRegion);
		return data != null ? data : this.clientBuildingToolData();
	}
	
	private BuildingToolPlayerData clientBuildingToolData()
	{
		if (buildingToolClientData == null)
			buildingToolClientData = new BuildingToolPlayerData.BuildingToolClientData(Minecraft.getInstance().player.getGameProfile().getName());
		return buildingToolClientData;
	}
	
	@Override
	public void clearRegistryKeyCache()
	{
		ClientUtil.REGISTRY_KEYS.clear();
	}
}
