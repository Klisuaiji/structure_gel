package com.legacy.structure_gel.core.events;

import org.joml.Matrix4f;

import com.legacy.structure_gel.core.StructureGelMod;
import com.legacy.structure_gel.core.client.ClientProxy;
import com.legacy.structure_gel.core.client.SGShaders;
import com.legacy.structure_gel.core.client.gui.ClientPaletteTooltip;
import com.legacy.structure_gel.core.client.renderers.BuildingToolRenderer;
import com.legacy.structure_gel.core.client.renderers.StructureBoundsRenderer;
import com.legacy.structure_gel.core.client.renderers.block_entity.DataHandlerRenderer;
import com.legacy.structure_gel.core.client.renderers.block_entity.DynamicSpawnerRenderer;
import com.legacy.structure_gel.core.client.renderers.block_entity.GelJigsawRenderer;
import com.legacy.structure_gel.core.client.screen.building_tool.BuildingToolOverlay;
import com.legacy.structure_gel.core.item.BlockPaletteItem;
import com.legacy.structure_gel.core.item.building_tool.BuildingToolItem;
import com.legacy.structure_gel.core.item.util.PaletteTooltip;
import com.legacy.structure_gel.core.registry.SGRegistry;
import com.legacy.structure_gel.core.util.Internal;
import com.mojang.datafixers.util.Either;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.PlainTextContents.LiteralContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@Internal
public class SGClientEvents
{
	@EventBusSubscriber(modid = StructureGelMod.MODID, value = Dist.CLIENT)
	protected static class ModBus
	{
		@SubscribeEvent
		protected static void registerKeyMappings(final RegisterKeyMappingsEvent event)
		{
			for (var mapping : ClientProxy.KEY_MAPPINGS)
				event.register(mapping.get());
		}

		@SubscribeEvent
		protected static void registerRenderers(final EntityRenderersEvent.RegisterRenderers event)
		{
			event.registerBlockEntityRenderer(BlockEntityType.JIGSAW, GelJigsawRenderer::new);
			event.registerBlockEntityRenderer(SGRegistry.BlockEntities.DATA_HANDLER.get(), DataHandlerRenderer::new);
			event.registerBlockEntityRenderer(SGRegistry.BlockEntities.DYNAMIC_SPAWNER.get(), DynamicSpawnerRenderer::new);
		}

		@SubscribeEvent
		protected static void registerGuiOverlays(final RegisterGuiLayersEvent event)
		{
			event.registerAbove(VanillaGuiLayers.SELECTED_ITEM_NAME, StructureGelMod.locate("building_tool_overlay"), BuildingToolOverlay::render);
		}

		@SubscribeEvent
		public static void registerTooltips(RegisterClientTooltipComponentFactoriesEvent event)
		{
			event.register(PaletteTooltip.class, ClientPaletteTooltip::new);
		}

		@SubscribeEvent
		public static void registerShaders(RegisterShadersEvent event)
		{
			SGShaders.init(event);
		}

		/*@SubscribeEvent
		protected static void trims(final com.legacy.structure_gel.api.events.RegisterArmorTrimTexturesEvent event)
		{
			event.registerSprite(new ResourceLocation("block/acacia_log"));
		}*/
	}

	@EventBusSubscriber(modid = StructureGelMod.MODID, value = Dist.CLIENT)
	protected static class ForgeBus
	{
		@SubscribeEvent
		protected static void onPlaySound(final PlaySoundEvent event)
		{
			SoundInstance originalSound = event.getOriginalSound();
			if (originalSound != null)
			{
				ResourceLocation name = originalSound.getLocation();
				if (name != null && ClientProxy.lastPortal != null)
				{
					SoundInstance newSound = null;
					if (name.equals(SoundEvents.PORTAL_TRAVEL.getLocation()))
						newSound = ClientProxy.lastPortal.getTravelSound();
					else if (name.equals(SoundEvents.PORTAL_TRIGGER.getLocation()))
						newSound = ClientProxy.lastPortal.getTriggerSound();

					if (newSound != null)
					{
						event.setSound(newSound);
					}
				}
			}
		}

		@SubscribeEvent
		protected static void onRenderLevel(final RenderLevelStageEvent event)
		{
			if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES)
			{
				Minecraft mc = Minecraft.getInstance();
				Matrix4f modelViewMatrix = event.getModelViewMatrix();
				Matrix4f projectionMatrix = event.getProjectionMatrix();
				var cam = event.getCamera().getPosition();
				double camX = cam.x;
				double camY = cam.y;
				double camZ = cam.z;
				StructureBoundsRenderer.render(mc, modelViewMatrix, projectionMatrix, camX, camY, camZ);
				BuildingToolRenderer.render(mc, modelViewMatrix, projectionMatrix, camX, camY, camZ);
			}
		}

		@SubscribeEvent
		protected static void onLogin(final PlayerEvent.PlayerLoggedInEvent event)
		{
			StructureGelMod.proxy.setViewBounds(false);
			StructureGelMod.proxy.clearToolRenderCache();
			StructureGelMod.proxy.clearRegistryKeyCache();
		}

		@SubscribeEvent
		public static void gatherTooltipComponents(RenderTooltipEvent.GatherComponents event)
		{
			ItemStack stack = event.getItemStack();
			if (stack.is(SGRegistry.Items.BUILDING_TOOL.get()))
			{
				var tooltipImage = BlockPaletteItem.getPalleteTooltip(stack, Minecraft.getInstance().level);
				if (tooltipImage.isPresent())
				{
					var elements = event.getTooltipElements();
					int targetIndex = -1;
					for (int i = 0; i < elements.size(); i++)
					{
						var either = elements.get(i);
						if (either.left().isPresent())
						{
							if (either.left().get() instanceof MutableComponent mutableComponent && mutableComponent.getContents() instanceof LiteralContents literal && BuildingToolItem.BLOCK_PALETTE_TOOLTIP.equals(literal.text()))
							{
								targetIndex = i;
								break;
							}
						}
					}
					if (targetIndex > -1)
					{
						// Replace the tooltip
						event.getTooltipElements().set(targetIndex, Either.right(tooltipImage.get()));
					}
				}
			}
		}
	}
}
