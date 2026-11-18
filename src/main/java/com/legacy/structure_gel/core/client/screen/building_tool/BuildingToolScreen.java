package com.legacy.structure_gel.core.client.screen.building_tool;

import com.legacy.structure_gel.api.client.gui.LayeredScreen;
import com.legacy.structure_gel.core.StructureGelMod;
import com.legacy.structure_gel.core.client.ClientProxy;
import com.legacy.structure_gel.core.client.renderers.BuildingToolRenderer;
import com.legacy.structure_gel.core.client.widget.MultiWidget;
import com.legacy.structure_gel.core.client.widget.PanelSelectionButton;
import com.legacy.structure_gel.core.client.widget.PropertyEditBox;
import com.legacy.structure_gel.core.client.widget.PropertyImageButton;
import com.legacy.structure_gel.core.client.widget.TooltipImageButton;
import com.legacy.structure_gel.core.item.building_tool.BuildingToolItem;
import com.legacy.structure_gel.core.item.building_tool.BuildingToolMode;
import com.legacy.structure_gel.core.item.building_tool.BuildingToolModes;
import com.legacy.structure_gel.core.item.building_tool.ToolModeProperty;
import com.legacy.structure_gel.core.network.SGPacketHandler;
import com.legacy.structure_gel.core.network.bi_directional.UpdateBuildingToolPacket;
import com.legacy.structure_gel.core.network.c_to_s.ActionHistoryPacket;
import com.legacy.structure_gel.core.registry.SGRegistry;
import com.legacy.structure_gel.core.util.SGText;
import com.mojang.blaze3d.platform.InputConstants.Key;
import com.mojang.blaze3d.platform.InputConstants.Type;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.util.random.WeightedEntry.Wrapper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;

public class BuildingToolScreen extends LayeredScreen {
   private static final String FOLDER = "textures/gui/building_tool/";
   private static final ResourceLocation RADIAL_A = StructureGelMod.locate("textures/gui/building_tool/radial_a.png");
   private static final ResourceLocation RADIAL_B = StructureGelMod.locate("textures/gui/building_tool/radial_b.png");
   private static final ResourceLocation RADIAL_SELECTED_A = StructureGelMod.locate("textures/gui/building_tool/radial_selected_a.png");
   private static final ResourceLocation RADIAL_SELECTED_B = StructureGelMod.locate("textures/gui/building_tool/radial_selected_b.png");
   private static final ResourceLocation RADIAL_CURRENT_A = StructureGelMod.locate("textures/gui/building_tool/radial_current_a.png");
   private static final ResourceLocation RADIAL_CURRENT_B = StructureGelMod.locate("textures/gui/building_tool/radial_current_b.png");
   public static final ResourceLocation WIDGETS = StructureGelMod.locate("textures/gui/building_tool/widgets.png");
   public static final ResourceLocation ARROWS = StructureGelMod.locate("textures/gui/building_tool/arrows.png");
   public static final ResourceLocation ARROWS_SELECTED = StructureGelMod.locate("textures/gui/building_tool/arrows_selected.png");
   protected static final Component GO_BACK_TEXT = Component.translatable("info.structure_gel.building_tool.go_back");
   protected static final String GO_BACK_DESC = "info.structure_gel.building_tool.go_back.description";
   public static final String UNDO_KEY = "info.structure_gel.building_tool.undo";
   protected static final Component UNDO_TEXT = Component.translatable("info.structure_gel.building_tool.undo");
   protected static final String UNDO_DESC = "info.structure_gel.building_tool.undo.description";
   public static final String REDO_KEY = "info.structure_gel.building_tool.redo";
   protected static final Component REDO_TEXT = Component.translatable("info.structure_gel.building_tool.redo");
   protected static final String REDO_DESC = "info.structure_gel.building_tool.redo.description";
   public static final String COPY_KEY = "info.structure_gel.building_tool.copy";
   public static final String PASTE_KEY = "info.structure_gel.building_tool.paste";
   public static final String DELETE_KEY = "info.structure_gel.building_tool.delete";
   protected static final Component SELECT_POS_1_TEXT = Component.translatable("info.structure_gel.building_tool.select_pos_1");
   protected static final String SELECT_POS_1_DESC = "info.structure_gel.building_tool.select_pos_1.description";
   protected static final Component SELECT_POS_2_TEXT = Component.translatable("info.structure_gel.building_tool.select_pos_2");
   protected static final String SELECT_POS_2_DESC = "info.structure_gel.building_tool.select_pos_2.description";
   protected static final Component EDIT_SELECTION_TEXT = Component.translatable("info.structure_gel.building_tool.edit_selection");
   protected static final String EDIT_SELECTION_DESC = "info.structure_gel.building_tool.edit_selection.description";
   protected static final Component BLOCK_PALETTE_TEXT = Component.translatable("info.structure_gel.building_tool.block_palette");
   protected static final String BLOCK_PALETTE_DESC = "info.structure_gel.building_tool.block_palette.description";
   public static final int TEXT_COLOR = 13487565;
   public static final int HOVERED_TEXT_COLOR = 16777215;
   public static final int PROPERTIES_START_Y = 80;
   public static final int PROPERTIES_SPACING_Y = 30;
   private final List<BuildingToolScreen.RadialSlice> radialSlices = (List<BuildingToolScreen.RadialSlice>)Util.make(
      new ArrayList(8),
      list -> {
         float d = 22.5F;
         int ix = 45;
         list.add(
            new BuildingToolScreen.RadialSlice(360.0F - d, d, RADIAL_A, RADIAL_SELECTED_A, RADIAL_CURRENT_A, 60, 0, 80, 80, 84, 15, BuildingToolModes.EXTEND)
         );
         float var3x;
         list.add(
            new BuildingToolScreen.RadialSlice(
               d, var3x = d + ix, RADIAL_B, RADIAL_SELECTED_B, RADIAL_CURRENT_B, 100, 0, 100, 100, 133, 35, BuildingToolModes.FLOOD
            )
         );
         list.add(
            new BuildingToolScreen.RadialSlice(
               var3x, d = var3x + ix, RADIAL_A, RADIAL_SELECTED_A, RADIAL_CURRENT_A, 120, 60, 80, 80, 153, 84, BuildingToolModes.FILL
            )
         );
         float var5;
         list.add(
            new BuildingToolScreen.RadialSlice(
               d, var5 = d + ix, RADIAL_B, RADIAL_SELECTED_B, RADIAL_CURRENT_B, 100, 100, 100, 100, 133, 133, BuildingToolModes.LINE
            )
         );
         list.add(
            new BuildingToolScreen.RadialSlice(
               var5, d = var5 + ix, RADIAL_A, RADIAL_SELECTED_A, RADIAL_CURRENT_A, 60, 120, 80, 80, 84, 153, BuildingToolModes.CLONE
            )
         );
         float var7;
         list.add(
            new BuildingToolScreen.RadialSlice(
               d, var7 = d + ix, RADIAL_B, RADIAL_SELECTED_B, RADIAL_CURRENT_B, 0, 100, 100, 100, 35, 133, BuildingToolModes.MOVE
            )
         );
         list.add(
            new BuildingToolScreen.RadialSlice(
               var7, d = var7 + ix, RADIAL_A, RADIAL_SELECTED_A, RADIAL_CURRENT_A, 0, 60, 80, 80, 15, 84, BuildingToolModes.REPLACE
            )
         );
         float var9;
         list.add(
            new BuildingToolScreen.RadialSlice(d, var9 = d + ix, RADIAL_B, RADIAL_SELECTED_B, RADIAL_CURRENT_B, 0, 0, 100, 100, 35, 35, BuildingToolModes.SHAPE)
         );
      }
   );
   protected final ItemStack stack;
   protected final InteractionHand hand;
   protected BuildingToolMode mode = BuildingToolModes.NONE;
   protected final Map<ToolModeProperty<?>, Object> properties = new HashMap<>();
   protected Optional<BlockPos> selectedPosA = Optional.empty();
   protected Optional<BlockPos> selectedPosB = Optional.empty();
   protected boolean usedEditSelectionScreen = false;
   protected boolean clearPoses = false;
   private Optional<WeightedRandomList<Wrapper<BlockState>>> palette = Optional.empty();
   protected Optional<ItemStack> selectedStateRender = Optional.empty();
   protected int hoveredRadial = -1;
   protected int selectedRadial = -1;
   protected int tick = 0;
   protected final List<Renderable> widgetsToRender = new ArrayList<>(2);
   protected final MultiWidget propertiesWidget = new MultiWidget();
   protected PropertyEditBox<Integer> reachDistanceWidget;
   protected PropertyImageButton<ToolModeProperty.BooleanProperty> blockUpdateWidget;
   public static int lastScreenState = 0;
   private Optional<BuildingToolScreen.DescriptionRenderer> descriptionRenderer = Optional.empty();
   private int itemStackRenderIndex = 0;

   public BuildingToolScreen(ItemStack stack, InteractionHand hand) {
      super(Component.empty());
      this.stack = stack;
      this.hand = hand;
      this.mode = BuildingToolItem.getMode(stack);
      this.setupProperties(this.mode);

      for (int i = 0; i < this.radialSlices.size(); i++) {
         if (this.radialSlices.get(i).mode == this.mode) {
            this.selectedRadial = i;
            break;
         }
      }

      this.selectedPosA = BuildingToolItem.getPos(stack, 0);
      this.selectedPosB = BuildingToolItem.getPos(stack, 1);
   }

   protected void init() {
      if (this.palette.isEmpty()) {
         this.palette = Optional.of(BuildingToolItem.getPalette(this.stack, this.minecraft.level));
      }

      this.widgetsToRender.clear();
      int undoH = 16;
      int undoW = 25;
      int texSize = 256;
      int centerX = this.width / 2;
      int centerY = this.height / 2;
      this.widgetsToRender
         .add(
            this.addWidget(
               new TooltipImageButton(
                     centerX - 187,
                     centerY - 100,
                     undoW,
                     undoH,
                     0,
                     0,
                     undoH,
                     WIDGETS,
                     texSize,
                     texSize,
                     button -> SGPacketHandler.sendToServer(ActionHistoryPacket.undo(false)),
                     UNDO_TEXT
                  )
                  .onTooltip(
                     (button, poseStack, mouseX, mouseY) -> this.renderDescription(
                        button,
                        UNDO_TEXT,
                        "info.structure_gel.building_tool.undo.description",
                        SGRegistry.Items.BUILDING_TOOL.get().getDefaultInstance().getHoverName(),
                        SGText.keybindString((KeyMapping)ClientProxy.UNDO_KEY.get())
                     )
                  )
            )
         );
      this.widgetsToRender
         .add(
            this.addWidget(
               new TooltipImageButton(
                     centerX - 158,
                     centerY - 100,
                     undoW,
                     undoH,
                     undoW,
                     0,
                     undoH,
                     WIDGETS,
                     texSize,
                     texSize,
                     button -> SGPacketHandler.sendToServer(ActionHistoryPacket.redo(false)),
                     REDO_TEXT
                  )
                  .onTooltip(
                     (button, poseStack, mouseX, mouseY) -> this.renderDescription(
                        button,
                        REDO_TEXT,
                        "info.structure_gel.building_tool.redo.description",
                        SGRegistry.Items.BUILDING_TOOL.get().getDefaultInstance().getHoverName(),
                        SGText.keybindString((KeyMapping)ClientProxy.REDO_KEY.get())
                     )
                  )
            )
         );
      this.addWidget(this.propertiesWidget);
      this.setupPropertiesWidgets(this.mode);
      int leftPropY = 80;
      ToolModeProperty.NumberProp<Integer> reachProp = ToolModeProperty.REACH_DISTANCE;
      String oldReach = this.reachDistanceWidget == null
         ? Integer.toString(BuildingToolItem.getReachDistanceModifier(this.stack))
         : this.reachDistanceWidget.getValue();
      this.reachDistanceWidget = this.addWidget(
         new PropertyEditBox<>(this.minecraft.font, centerX + 110, centerY - leftPropY, reachProp, BuildingToolItem.getReachDistanceModifier(this.stack))
      );
      this.reachDistanceWidget.setValue(oldReach);
      this.reachDistanceWidget
         .onTooltip(
            (box, poseStack, mouseX, mouseY) -> this.renderDescription(
               box,
               reachProp.getNameComponent(),
               List.of(
                  Pair.of("info.structure_gel.building_tool.property_range", new Object[]{reachProp.min().toString(), reachProp.max().toString()}),
                  Pair.of(reachProp.getDescKey(), new Object[0])
               )
            )
         );
      leftPropY -= 30;
      ToolModeProperty.SelectionProp<ToolModeProperty.BooleanProperty> blockUpdateProp = ToolModeProperty.CAUSE_BLOCK_UPDATES;
      ToolModeProperty.BooleanProperty oldBlockUpdate = this.blockUpdateWidget == null
         ? ToolModeProperty.BooleanProperty.from(BuildingToolItem.causesBlockUpdates(this.stack))
         : this.blockUpdateWidget.getSelectedValue();
      this.blockUpdateWidget = this.addWidget(
         new PropertyImageButton<>(centerX + 110, centerY - leftPropY, b -> {}, blockUpdateProp.getNameComponent(), blockUpdateProp, oldBlockUpdate)
      );
      this.blockUpdateWidget
         .onTooltip(
            (box, poseStack, mouseX, mouseY) -> this.renderDescription(
               box, blockUpdateProp.getNameComponent(), List.of(Pair.of(blockUpdateProp.getDescKey(), new Object[0]))
            )
         );
      leftPropY -= 30;
      this.updateRenderStack();
   }

   private void setupProperties(BuildingToolMode mode) {
      this.properties.clear();
      if (BuildingToolItem.getMode(this.stack).equals(this.mode)) {
         for (ToolModeProperty property : mode.getProperties().values()) {
            this.properties.put(property, BuildingToolItem.getProperty(this.stack, property));
         }
      } else {
         for (ToolModeProperty property : mode.getProperties().values()) {
            this.properties.put(property, property.getDefaultValue());
         }
      }
   }

   private void setupPropertiesWidgets(BuildingToolMode mode) {
      int centerX = this.width / 2;
      int centerY = this.height / 2;
      int texSize = 256;
      this.propertiesWidget.clear();
      if (mode instanceof BuildingToolMode.ForCorners) {
         int setPosW = 20;
         int buttonSpread = setPosW + 8;
         this.propertiesWidget
            .addListener(
               new TooltipImageButton(centerX + 150 - buttonSpread, centerY - 104, setPosW, setPosW, 170, 0, setPosW, WIDGETS, texSize, texSize, button -> {
                     this.selectedPosA = Optional.ofNullable(this.minecraft.player.blockPosition());
                     this.usedEditSelectionScreen = false;
                     this.close();
                  }, SELECT_POS_1_TEXT)
                  .onTooltip(
                     (button, poseStack, mouseX, mouseY) -> this.renderDescription(
                        button, SELECT_POS_1_TEXT, "info.structure_gel.building_tool.select_pos_1.description"
                     )
                  )
            );
         this.propertiesWidget
            .addListener(
               new TooltipImageButton(centerX + 150, centerY - 104, setPosW, setPosW, 190, 0, setPosW, WIDGETS, texSize, texSize, button -> {
                     this.selectedPosB = Optional.ofNullable(this.minecraft.player.blockPosition());
                     this.usedEditSelectionScreen = false;
                     this.close();
                  }, SELECT_POS_2_TEXT)
                  .onTooltip(
                     (button, poseStack, mouseX, mouseY) -> this.renderDescription(
                        button, SELECT_POS_2_TEXT, "info.structure_gel.building_tool.select_pos_2.description"
                     )
                  )
            );
         TooltipImageButton editButton = new TooltipImageButton(
               centerX + 150 + buttonSpread,
               centerY - 104,
               setPosW,
               setPosW,
               150,
               0,
               setPosW,
               WIDGETS,
               texSize,
               texSize,
               button -> this.minecraft.setScreen(new EditSelectionScreen(this)),
               EDIT_SELECTION_TEXT
            )
            .onTooltip(
               (button, poseStack, mouseX, mouseY) -> this.renderDescription(
                  button,
                  EDIT_SELECTION_TEXT,
                  "info.structure_gel.building_tool.edit_selection.description",
                  SGText.keybindString(this.minecraft.options.keyDrop)
               )
            );
         this.propertiesWidget.addListener(editButton);
      }

      int y = 80;

      for (Entry<String, ToolModeProperty<?>> entry : mode.getProperties().entrySet()) {
         ToolModeProperty prop = entry.getValue();
         Object currentVal = this.properties.get(prop);
         if (!(prop instanceof ToolModeProperty.SelectionProp<?> selectionProp && currentVal instanceof StringRepresentable)) {
            PropertyEditBox editBox = new PropertyEditBox<>(this.minecraft.font, centerX - 210, centerY - y, prop, currentVal);
            editBox.setResponder(s -> this.properties.put(prop, prop.read(s)));
            editBox.onTooltip(
               (box, poseStack, mouseX, mouseY) -> {
                  if (prop instanceof ToolModeProperty.NumberProp numProp) {
                     this.renderDescription(
                        box,
                        prop.getNameComponent(),
                        List.of(
                           Pair.of("info.structure_gel.building_tool.property_range", new Object[]{numProp.min().toString(), numProp.max().toString()}),
                           Pair.of(prop.getDescKey(), new Object[0])
                        )
                     );
                  } else {
                     this.renderDescription(box, prop.getNameComponent(), prop.getDescKey());
                  }
               }
            );
            this.propertiesWidget.addListener(editBox);
         } else if (currentVal instanceof ToolModeProperty.IPanelSelection panelProp) {
            PanelSelectionButton panelButton = new PanelSelectionButton<>(
               centerX - 210,
               centerY - y,
               button -> this.properties.put(button.getProperty(), button.getSelectedValue()),
               prop.getNameComponent(),
               selectionProp,
               panelProp
            );
            panelButton.onTooltip((button, poseStack, mouseX, mouseY) -> this.renderDescription(button, prop.getNameComponent(), prop.getDescKey()));
            this.propertiesWidget.addListener(panelButton);
            this.propertiesWidget.addListener(1, panelButton.getPanel());
         } else {
            this.propertiesWidget
               .addListener(
                  new PropertyImageButton<>(
                        centerX - 210,
                        centerY - y,
                        button -> this.properties.put(button.getProperty(), button.getSelectedValue()),
                        prop.getNameComponent(),
                        selectionProp,
                        (StringRepresentable)currentVal
                     )
                     .onTooltip((button, poseStack, mouseX, mouseY) -> this.renderDescription(button, prop.getNameComponent(), prop.getDescKey()))
               );
         }

         y -= 30;
      }
   }

   public void renderGrayBackground(GuiGraphics graphics, int width, int height) {
      float mixedTick = this.tick + this.minecraft.getFrameTime();
      float animationTime = 2.5F;
      int r = 30;
      int a = 150;
      int alpha = mixedTick < animationTime ? (int)(a * mixedTick / animationTime) : a;
      int rgb = ((alpha << 8 | r) << 8 | r) << 8 | r;
      int var13 = r / 3;
      int var14 = r / 3;
      int var15 = r / 3;
      int rgb2 = ((alpha << 8 | var13) << 8 | var14) << 8 | var15;
      graphics.fillGradient(0, 0, width, height, rgb, rgb2);
   }

   @Override
   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      super.render(graphics, mouseX, mouseY, partialTick);
      PoseStack poseStack = graphics.pose();
      if (lastScreenState == 1) {
         this.minecraft.setScreen(new EditSelectionScreen(this));
      } else {
         float mixedTick = this.tick + this.minecraft.getFrameTime();
         float animationTime = 2.5F;
         RenderSystem.setShader(GameRenderer::getPositionTexShader);
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         RenderSystem.enableBlend();
         int centerX = this.width / 2;
         int centerY = this.height / 2;
         double xCoord = mouseX - this.width / 2;
         double yCoord = this.height - mouseY - this.height / 2;
         double theta = Math.atan2(xCoord, yCoord) * (180.0 / Math.PI);
         if (theta < 0.0) {
            theta += 360.0;
         }

         double dist = Math.sqrt(xCoord * xCoord + yCoord * yCoord);
         if (mixedTick < animationTime) {
            float scale = mixedTick / animationTime;
            float invscale = 1.0F - scale;
            poseStack.pushPose();
            poseStack.translate(this.width / 2 * invscale, this.height / 2 * invscale, 0.0F);
            poseStack.scale(scale, scale, scale);
         }

         this.hoveredRadial = -1;

         for (int i = this.radialSlices.size() - 1; i > -1; i--) {
            BuildingToolScreen.RadialSlice slice = this.radialSlices.get(i);
            boolean hovered = false;
            float minDegree = slice.minDegree;
            float maxDegree = slice.maxDegree;
            boolean wrapAround = minDegree > maxDegree;
            if ((
                  wrapAround
                     ? (theta > 180.0 ? theta > minDegree : theta > minDegree - 360.0F) && (theta > 180.0 ? theta < maxDegree + 360.0F : theta < maxDegree)
                     : theta > minDegree && theta < maxDegree
               )
               && dist > 30.0
               && dist < 110.0) {
               hovered = true;
               this.hoveredRadial = i;
            }

            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            graphics.blit(
               slice.getTexture(hovered, this.selectedRadial == i), centerX - (100 - slice.x), centerY - (100 - slice.y), slice.x, slice.y, slice.u, slice.v
            );
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            if (this.hoveredRadial != i && this.selectedRadial != i) {
               RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.75F);
            }

            graphics.blit(slice.mode.getIconTexture(), centerX - (100 - slice.iconX), centerY - (100 - slice.iconY), 0.0F, 0.0F, 32, 32, 32, 32);
         }

         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         if (dist < 21.0) {
            this.hoveredRadial = -2;
         }

         graphics.blit(WIDGETS, centerX - 20, centerY - 20, 0, dist < 21.0 && this.mode.hasBlockPalette() ? 72 : 32, 40, 40);
         this.widgetsToRender.forEach(widget -> {
            widget.render(graphics, mouseX, mouseY, partialTick);
            RenderSystem.enableBlend();
         });
         this.propertiesWidget.render(graphics, mouseX, mouseY, partialTick);
         this.reachDistanceWidget.render(graphics, mouseX, mouseY, partialTick);
         RenderSystem.enableBlend();
         this.blockUpdateWidget.render(graphics, mouseX, mouseY, partialTick);
         if (this.hoveredRadial >= 0) {
            BuildingToolMode mode = this.radialSlices.get(this.hoveredRadial).mode;
            this.renderDescription(mouseX, mode.getComponent(), mode.getDescKey(), mode.getDescArgs());
         } else if (this.hoveredRadial == -2 && this.mode.hasBlockPalette()) {
            this.renderDescription(
               mouseX,
               BLOCK_PALETTE_TEXT,
               "info.structure_gel.building_tool.block_palette.description",
               SGText.keybindString(this.minecraft.options.keyPickItem),
               BuildingToolItem.getPaletteString(this.getPalette())
            );
         }

         if (this.selectedStateRender.isPresent() && this.mode.hasBlockPalette()) {
            graphics.renderItem(this.selectedStateRender.get(), centerX - 8, centerY - 8);
         }

         if (this.descriptionRenderer.isPresent()) {
            this.descriptionRenderer.get().render(graphics);
            this.descriptionRenderer = Optional.empty();
         }

         if (mixedTick < animationTime) {
            poseStack.popPose();
         }
      }
   }

   private void renderDescription(GuiEventListener widget, Component title, String descriptionKey, Object... descriptionArgs) {
      this.renderDescription(widget, title, List.of(Pair.of(descriptionKey, descriptionArgs)));
   }

   private void renderDescription(GuiEventListener widget, Component title, List<Pair<String, Object[]>> descriptionKeys) {
      this.renderDescription(widget.getRectangle().position().x(), title, descriptionKeys);
   }

   private void renderDescription(int x, Component title, String descriptionKey, Object... descriptionArgs) {
      this.renderDescription(x, title, List.of(Pair.of(descriptionKey, descriptionArgs)));
   }

   private void renderDescription(int x, Component title, List<Pair<String, Object[]>> descriptionKeys) {
      this.descriptionRenderer = Optional.of(new BuildingToolScreen.DescriptionRenderer(this, x, title, descriptionKeys));
   }

   public void updateRenderStack() {
      Optional<ItemStack> stack = Optional.empty();
      List<Wrapper<BlockState>> statesList = this.getPalette().unwrap();
      if (statesList.isEmpty()) {
         stack = Optional.of(Items.BUCKET.getDefaultInstance());
      } else {
         int i = this.itemStackRenderIndex % statesList.size();
         stack = Optional.of(BuildingToolItem.getItemForBlock((BlockState)statesList.get(i).getData(), Minecraft.getInstance().level));
      }

      this.selectedStateRender = stack;
      this.itemStackRenderIndex++;
   }

   public void tick() {
      if (this.tick % 20 == 0) {
         this.updateRenderStack();
      }

      this.tick++;
   }

   public Optional<ItemStack> getItemForBlock(String val) {
      try {
         return Optional.of(
            BuildingToolItem.getItemForBlock(
               BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK.asLookup(), val, false).blockState(), Minecraft.getInstance().level
            )
         );
      } catch (Exception var3) {
         return Optional.empty();
      }
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int clickType) {
      if (Screen.hasControlDown() && ((KeyMapping)ClientProxy.UNDO_KEY.get()).getKey().getValue() == clickType) {
         SGPacketHandler.sendToServer(ActionHistoryPacket.undo(true));
         return true;
      } else if (Screen.hasControlDown() && ((KeyMapping)ClientProxy.REDO_KEY.get()).getKey().getValue() == clickType) {
         SGPacketHandler.sendToServer(ActionHistoryPacket.redo(true));
         return true;
      } else if (((KeyMapping)ClientProxy.BUILDING_TOOL_KEY.get()).getKey().getValue() == clickType) {
         this.close();
         return true;
      } else {
         if (this.hoveredRadial == -2 && this.mode.hasBlockPalette()) {
            if (clickType == 0) {
               playClickSound();
               this.minecraft.setScreen(new EditPaletteScreen(this));
               return true;
            }

            if (clickType == 2) {
               playClickSound();
               this.setPalette(WeightedRandomList.create());
               this.updateRenderStack();
               return true;
            }
         } else if (this.hoveredRadial > -1) {
            this.setMode(this.hoveredRadial);
            return true;
         }

         return super.mouseClicked(mouseX, mouseY, clickType);
      }
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      Key key = Type.KEYSYM.getOrCreate(keyCode);
      boolean pressedEnter = keyCode == 257 || keyCode == 335;
      if (((KeyMapping)ClientProxy.UNDO_KEY.get()).isActiveAndMatches(key)) {
         SGPacketHandler.sendToServer(ActionHistoryPacket.undo(true));
         return true;
      } else if (((KeyMapping)ClientProxy.REDO_KEY.get()).isActiveAndMatches(key)) {
         SGPacketHandler.sendToServer(ActionHistoryPacket.redo(true));
         return true;
      } else {
         if (this.minecraft.options.keyDrop.isActiveAndMatches(key)) {
            playClickSound();
            this.minecraft.setScreen(new EditSelectionScreen(this));
         }

         if (!this.minecraft.options.keyInventory.isActiveAndMatches(key)
            && !((KeyMapping)ClientProxy.BUILDING_TOOL_KEY.get()).isActiveAndMatches(key)
            && !pressedEnter) {
            return super.keyPressed(keyCode, scanCode, modifiers);
         } else {
            this.close();
            return true;
         }
      }
   }

   public void setMode(int radial) {
      BuildingToolMode newMode = this.radialSlices.get(radial).mode;
      playClickSound();
      if (this.mode != newMode) {
         this.mode = newMode;
         this.selectedRadial = radial;
         this.setupProperties(this.mode);
         this.setupPropertiesWidgets(this.mode);
      } else {
         this.close();
      }
   }

   public void close() {
      this.minecraft.setScreen(null);
      this.onClose();
   }

   @Override
   public void onClose() {
      BuildingToolRenderer.needsUpdated();
      Minecraft mc = this.minecraft;
      LocalPlayer player = mc.player;
      ItemStack stack = this.stack;
      if (stack.is(SGRegistry.Items.BUILDING_TOOL.get())) {
         BuildingToolItem.setMode(stack, this.mode);

         for (Entry<ToolModeProperty<?>, Object> entry : this.properties.entrySet()) {
            ToolModeProperty prop = entry.getKey();
            BuildingToolItem.setProperty(stack, prop, entry.getValue());
         }

         if (this.mode instanceof BuildingToolMode.ForCorners cornerMode) {
            if (this.selectedPosA.isPresent()) {
               cornerMode.setPositionA(player, this.selectedPosA.get(), stack, !this.usedEditSelectionScreen);
            }

            if (this.selectedPosB.isPresent()) {
               cornerMode.setPositionB(player, this.selectedPosB.get(), stack, !this.usedEditSelectionScreen);
            }
         }

         if (this.clearPoses) {
            this.mode.clearPoses(stack, player);
         }

         BuildingToolItem.setPalette(stack, this.getPalette());
         player.swing(this.hand);
         int reach = ToolModeProperty.REACH_DISTANCE.read(this.reachDistanceWidget.getValue());
         BuildingToolItem.setReachDistanceModifier(stack, reach);
         boolean causeBlockUpdates = this.blockUpdateWidget.getSelectedValue().value();
         BuildingToolItem.setCausesBlockUpdates(stack, causeBlockUpdates);
         this.mode.onSelect(stack, mc.level, player);
         SGPacketHandler.sendToServer(
            UpdateBuildingToolPacket.builder()
               .hand(this.hand)
               .mode(this.mode)
               .properties(this.properties)
               .posA(this.selectedPosA)
               .posB(this.selectedPosB)
               .clearPoses(this.clearPoses)
               .palette(this.getPalette())
               .reachDistance(reach)
               .causeBlockUpdates(causeBlockUpdates)
               .build()
         );
      }

      lastScreenState = 0;
      super.onClose();
   }

   public boolean isPauseScreen() {
      return false;
   }

   public static void playClickSound() {
      Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
   }

   public WeightedRandomList<Wrapper<BlockState>> getPalette() {
      return this.palette.orElse(WeightedRandomList.create());
   }

   public void setPalette(@Nullable WeightedRandomList<Wrapper<BlockState>> palette) {
      this.palette = Optional.ofNullable(palette);
   }

   public static class DescriptionRenderer {
      private final Screen screen;
      private final Component title;
      private final int x;
      private final List<Pair<String, Object[]>> descriptionKeys;

      public DescriptionRenderer(Screen screen, int x, Component title, List<Pair<String, Object[]>> descriptionKeys) {
         this.screen = screen;
         this.x = x;
         this.title = title;
         this.descriptionKeys = descriptionKeys;
      }

      void render(GuiGraphics graphics) {
         PoseStack poseStack = graphics.pose();
         poseStack.pushPose();
         int centerX = this.screen.width / 2;
         int centerY = this.screen.height / 2;
         boolean renderRight = this.x - centerX < 110;
         List<FormattedCharSequence> lines = new LinkedList<>();
         lines.add(this.title.getVisualOrderText());
         int remainingWidth = Math.max(0, this.screen.width / 2 - 100 - 15);

         for (Pair<String, Object[]> desc : this.descriptionKeys) {
            Object[] args = (Object[])desc.getSecond();
            lines.addAll(Minecraft.getInstance().font.split(SGText.applyKeybindFilter(Component.translatable((String)desc.getFirst(), args)), remainingWidth));
         }

         graphics.renderTooltip(Minecraft.getInstance().font, lines, renderRight ? centerX + 100 : 0, centerY - 74);
         poseStack.popPose();
      }
   }

   private record RadialSlice(
      float minDegree,
      float maxDegree,
      ResourceLocation texture,
      ResourceLocation hoveredTexture,
      ResourceLocation currentTexture,
      int x,
      int y,
      int u,
      int v,
      int iconX,
      int iconY,
      BuildingToolMode mode
   ) {
      public ResourceLocation getTexture(boolean hovered, boolean isSelected) {
         if (hovered) {
            return this.hoveredTexture;
         } else {
            return isSelected ? this.currentTexture : this.texture;
         }
      }
   }
}
