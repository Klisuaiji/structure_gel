package com.legacy.structure_gel.core.events;

import com.legacy.blue_skies.compat.*;
import com.legacy.structure_gel.api.block.GelPortalBlock;
import com.legacy.structure_gel.core.StructureGelMod;
import com.legacy.structure_gel.core.capability.entity.GelEntity;
import com.legacy.structure_gel.core.client.ClientProxy;
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
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Either;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.PlainTextContents.LiteralContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.properties.StructureMode;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.LogicalSide;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.neoforged.neoforge.client.event.RegisterGuiOverlaysEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage;
import net.neoforged.neoforge.client.event.RenderTooltipEvent.GatherComponents;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;
import com.legacy.blue_skies.compat.LegacyGuiOverlay;
import net.neoforged.neoforge.common.util.Lazy;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import org.joml.Matrix4f;

@Internal
public class SGClientEvents {
   @EventBusSubscriber(modid = "structure_gel", bus = Bus.FORGE, value = Dist.CLIENT)
   protected static class ForgeBus {
      @SubscribeEvent
      protected static void onPlaySound(PlaySoundEvent event) {
         SoundInstance originalSound = event.getOriginalSound();
         if (originalSound != null) {
            ResourceLocation name = originalSound.getLocation();
            GelPortalBlock portal = GelEntity.getPortalClient();
            if (portal != null && name != null) {
               SoundInstance newSound = null;
               if (name.equals(SoundEvents.PORTAL_TRAVEL.getLocation())) {
                  newSound = portal.getTravelSound();
               } else if (name.equals(SoundEvents.PORTAL_TRIGGER.getLocation())) {
                  newSound = portal.getTriggerSound();
               }

               if (newSound != null) {
                  event.setSound(newSound);
               }
            }
         }
      }

      @SubscribeEvent
      protected static void clientPlayerTick(PlayerTickEvent.Post event) {
         if (event.getEntity().level().isClientSide()) {
            GelEntity.clientTick();
         }
      }

      @SubscribeEvent
      protected static void onRenderLevel(RenderLevelStageEvent event) {
         if (event.getStage() == Stage.AFTER_PARTICLES) {
            Minecraft mc = Minecraft.getInstance();
            PoseStack poseStack = event.getPoseStack();
            Matrix4f matrix = event.getProjectionMatrix();
            Vec3 cam = event.getCamera().getPosition();
            double camX = cam.x;
            double camY = cam.y;
            double camZ = cam.z;
            StructureBoundsRenderer.render(mc, poseStack, matrix, camX, camY, camZ);
            BuildingToolRenderer.render(mc, poseStack, matrix, camX, camY, camZ);
         }
      }

      @SubscribeEvent
      protected static void onLogin(PlayerLoggedInEvent event) {
         StructureGelMod.proxy.setViewBounds(false);
         StructureGelMod.proxy.clearToolRenderCache();
         StructureGelMod.proxy.clearRegistryKeyCache();
      }

      @SubscribeEvent
      public static void gatherTooltipComponents(GatherComponents event) {
         ItemStack stack = event.getItemStack();
         if (stack.is(SGRegistry.Items.BUILDING_TOOL.get())) {
            Optional<TooltipComponent> tooltipImage = BlockPaletteItem.getPalleteTooltip(stack, Minecraft.getInstance().level);
            if (tooltipImage.isPresent()) {
               List<Either<FormattedText, TooltipComponent>> elements = event.getTooltipElements();
               int targetIndex = -1;

               for (int i = 0; i < elements.size(); i++) {
                  Either<FormattedText, TooltipComponent> either = elements.get(i);
                  if (either.left().isPresent()
                     && either.left().get() instanceof MutableComponent mutableComponent
                     && mutableComponent.getContents() instanceof LiteralContents literal
                     && BuildingToolItem.BLOCK_PALETTE_TOOLTIP.equals(literal.text())) {
                     targetIndex = i;
                     break;
                  }
               }

               if (targetIndex > -1) {
                  event.getTooltipElements().set(targetIndex, Either.right(tooltipImage.get()));
               }
            }
         }
      }
   }

   @EventBusSubscriber(modid = "structure_gel", bus = Bus.MOD, value = Dist.CLIENT)
   protected static class ModBus {
      @SubscribeEvent
      protected static void clientInit(FMLClientSetupEvent event) {
         event.enqueueWork(
            () -> {
               ItemProperties.register(Items.STRUCTURE_BLOCK, StructureGelMod.locate("mode"), (stack, level, entity, layer) -> {
                  CompoundTag tag = LegacyNbt.get(stack);
                  if (tag != null && tag.contains("BlockEntityTag", 10)) {
                     CompoundTag blockEntityTag = tag.getCompound("BlockEntityTag");
                     if (blockEntityTag.contains("mode", 8)) {
                        return StructureMode.valueOf(blockEntityTag.getString("mode")).ordinal();
                     }
                  }

                  return -1.0F;
               });
               ItemProperties.register(
                  SGRegistry.Items.BUILDING_TOOL.get(),
                  StructureGelMod.locate("mode"),
                  (stack, level, entity, layer) -> BuildingToolItem.getMode(stack).modelIndex()
               );
            }
         );
      }

      @SubscribeEvent
      protected static void registerKeyMappings(RegisterKeyMappingsEvent event) {
         for (Lazy<KeyMapping> mapping : ClientProxy.KEY_MAPPINGS) {
            event.register((KeyMapping)mapping.get());
         }
      }

      @SubscribeEvent
      protected static void registerRenderers(RegisterRenderers event) {
         event.registerBlockEntityRenderer(BlockEntityType.JIGSAW, GelJigsawRenderer::new);
         event.registerBlockEntityRenderer(SGRegistry.BlockEntities.DATA_HANDLER.get(), DataHandlerRenderer::new);
         event.registerBlockEntityRenderer(SGRegistry.BlockEntities.DYNAMIC_SPAWNER.get(), DynamicSpawnerRenderer::new);
      }

      @SubscribeEvent
      protected static void registerGuiOverlays(RegisterGuiOverlaysEvent event) {
         event.registerAbove(LegacyGuiOverlay.ITEM_NAME.id(), StructureGelMod.locate("building_tool_overlay"), BuildingToolOverlay::render);
      }

      @SubscribeEvent
      public static void registerTooltips(RegisterClientTooltipComponentFactoriesEvent event) {
         event.register(PaletteTooltip.class, ClientPaletteTooltip::new);
      }
   }
}
