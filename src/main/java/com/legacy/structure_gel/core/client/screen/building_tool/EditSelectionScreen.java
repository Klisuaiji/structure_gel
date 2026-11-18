package com.legacy.structure_gel.core.client.screen.building_tool;

import com.legacy.structure_gel.api.client.gui.LayeredScreen;
import com.legacy.structure_gel.core.client.ClientProxy;
import com.legacy.structure_gel.core.client.widget.PropertyEditBox;
import com.legacy.structure_gel.core.client.widget.TooltipImageButton;
import com.legacy.structure_gel.core.item.building_tool.BuildingToolItem;
import com.legacy.structure_gel.core.item.building_tool.SmartBoundingBox;
import com.legacy.structure_gel.core.item.building_tool.ToolModeProperty;
import com.legacy.structure_gel.core.registry.SGRegistry;
import com.legacy.structure_gel.core.util.SGText;
import com.mojang.blaze3d.platform.InputConstants.Key;
import com.mojang.blaze3d.platform.InputConstants.Type;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.datafixers.util.Pair;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class EditSelectionScreen extends LayeredScreen {
   protected static final Component CLEAR_SELECTION_TEXT = Component.translatable("info.structure_gel.building_tool.clear_selection");
   protected static final String CLEAR_SELECTION_DESC = "info.structure_gel.building_tool.clear_selection.description";
   protected static final String EXPAND_FACING_DESC = "info.structure_gel.building_tool.expand_facing.description";
   protected static final String EXPAND_TEXT = "info.structure_gel.building_tool.expand";
   protected static final String EXPAND_DESC = "info.structure_gel.building_tool.expand.description";
   protected static final String SHRINK_TEXT = "info.structure_gel.building_tool.shrink";
   protected static final String SHRINK_DESC = "info.structure_gel.building_tool.shrink.description";
   protected static final Component FACING = Component.translatable("info.structure_gel.building_tool.facing");
   protected static final Component FRONT = Component.translatable("info.structure_gel.building_tool.front");
   protected static final Component BACK = Component.translatable("info.structure_gel.building_tool.back");
   protected static final Component LEFT = Component.translatable("info.structure_gel.building_tool.left");
   protected static final Component RIGHT = Component.translatable("info.structure_gel.building_tool.right");
   protected static final Component TOP = Component.translatable("info.structure_gel.building_tool.top");
   protected static final Component BOTTOM = Component.translatable("info.structure_gel.building_tool.bottom");
   private final BuildingToolScreen parent;
   protected final List<Renderable> widgetsToRender = new ArrayList<>(2);
   private SmartBoundingBox bounds;
   private Vec3i negativePos;
   private Vec3i positivePos;
   private PropertyEditBox<Integer> xEditBox;
   private PropertyEditBox<Integer> yEditBox;
   private PropertyEditBox<Integer> zEditBox;
   private Optional<BuildingToolScreen.DescriptionRenderer> descriptionRenderer = Optional.empty();

   public EditSelectionScreen(BuildingToolScreen parent) {
      super(Component.empty());
      this.parent = parent;
      Optional<BlockPos> posA = BuildingToolItem.getPos(this.parent.stack, 0);
      Optional<BlockPos> posB = BuildingToolItem.getPos(this.parent.stack, 1);
      if (posA.isPresent() && posB.isPresent()) {
         this.bounds = SmartBoundingBox.fromCorners((Vec3i)posA.get(), (Vec3i)posB.get());
      } else if (posA.isPresent() && posB.isEmpty()) {
         this.bounds = SmartBoundingBox.fromCorners((Vec3i)posA.get(), (Vec3i)posA.get());
      } else if (posA.isEmpty() && posB.isPresent()) {
         this.bounds = SmartBoundingBox.fromCorners((Vec3i)posB.get(), (Vec3i)posB.get());
      } else {
         BlockPos playerPos = Minecraft.getInstance().player.blockPosition();
         this.bounds = SmartBoundingBox.fromCorners(playerPos, playerPos);
      }

      this.negativePos = this.bounds.getMin();
      this.positivePos = this.bounds.getMax();
   }

   protected void init() {
      this.widgetsToRender.clear();
      int texSize = 256;
      int centerX = this.width / 2;
      int centerY = this.height / 2;
      int texX = centerX - 100;
      int texY = centerY - 100;
      int circleWidth = 40;
      int w = 16;
      String shiftKey = SGText.keybindString(this.minecraft.options.keyShift);
      this.widgetsToRender
         .add(
            this.addWidget(
               new TooltipImageButton(
                     texX + 21, texY + 21, circleWidth, circleWidth, 112, 112, circleWidth, BuildingToolScreen.WIDGETS, texSize, texSize, b -> this.back()
                  )
                  .onTooltip(
                     (button, poseStack, mouseX, mouseY) -> this.renderDescription(
                        button,
                        BuildingToolScreen.GO_BACK_TEXT,
                        "info.structure_gel.building_tool.go_back.description",
                        SGText.keybindString(this.minecraft.options.keyDrop)
                     )
                  )
            )
         );
      this.widgetsToRender
         .add(
            this.addWidget(
               new TooltipImageButton(
                     texX + 21,
                     texY + 139,
                     circleWidth,
                     circleWidth,
                     72,
                     112,
                     circleWidth,
                     BuildingToolScreen.WIDGETS,
                     texSize,
                     texSize,
                     b -> this.clearSelection()
                  )
                  .onTooltip(
                     (button, poseStack, mouseX, mouseY) -> this.renderDescription(
                        button, CLEAR_SELECTION_TEXT, "info.structure_gel.building_tool.clear_selection.description"
                     )
                  )
            )
         );
      this.widgetsToRender
         .add(
            this.addWidget(
               new TooltipImageButton(
                     texX + 80,
                     texY + 80,
                     circleWidth,
                     circleWidth,
                     32,
                     112,
                     circleWidth,
                     BuildingToolScreen.WIDGETS,
                     texSize,
                     texSize,
                     b -> this.expandFacing()
                  )
                  .onTooltip(
                     (button, poseStack, mouseX, mouseY) -> this.renderDescription(
                        button,
                        this.component("info.structure_gel.building_tool.expand", FACING),
                        "info.structure_gel.building_tool.expand_facing.description",
                        shiftKey
                     )
                  )
            )
         );
      this.widgetsToRender
         .add(
            this.addWidget(
               new TooltipImageButton(texX + 92, texY + 14, w, w, 0, 112, w, BuildingToolScreen.WIDGETS, texSize, texSize, b -> this.expand(Direction.NORTH))
                  .onTooltip(
                     (button, poseStack, mouseX, mouseY) -> this.renderDescription(
                        button,
                        this.component("info.structure_gel.building_tool.expand", FRONT),
                        "info.structure_gel.building_tool.expand.description",
                        shiftKey
                     )
                  )
            )
         );
      this.widgetsToRender
         .add(
            this.addWidget(
               new TooltipImageButton(
                     texX + 92, texY + 34, w, w, 0 + w, 112, w, BuildingToolScreen.WIDGETS, texSize, texSize, b -> this.shrink(Direction.NORTH)
                  )
                  .onTooltip(
                     (button, poseStack, mouseX, mouseY) -> this.renderDescription(
                        button,
                        this.component("info.structure_gel.building_tool.shrink", FRONT),
                        "info.structure_gel.building_tool.shrink.description",
                        shiftKey
                     )
                  )
            )
         );
      this.widgetsToRender
         .add(
            this.addWidget(
               new TooltipImageButton(texX + 92, texY + 171, w, w, 0, 112, w, BuildingToolScreen.WIDGETS, texSize, texSize, b -> this.expand(Direction.SOUTH))
                  .onTooltip(
                     (button, poseStack, mouseX, mouseY) -> this.renderDescription(
                        button,
                        this.component("info.structure_gel.building_tool.expand", BACK),
                        "info.structure_gel.building_tool.expand.description",
                        shiftKey
                     )
                  )
            )
         );
      this.widgetsToRender
         .add(
            this.addWidget(
               new TooltipImageButton(
                     texX + 92, texY + 150, w, w, 0 + w, 112, w, BuildingToolScreen.WIDGETS, texSize, texSize, b -> this.shrink(Direction.SOUTH)
                  )
                  .onTooltip(
                     (button, poseStack, mouseX, mouseY) -> this.renderDescription(
                        button,
                        this.component("info.structure_gel.building_tool.shrink", BACK),
                        "info.structure_gel.building_tool.shrink.description",
                        shiftKey
                     )
                  )
            )
         );
      this.widgetsToRender
         .add(
            this.addWidget(
               new TooltipImageButton(texX + 171, texY + 92, w, w, 0, 112, w, BuildingToolScreen.WIDGETS, texSize, texSize, b -> this.expand(Direction.EAST))
                  .onTooltip(
                     (button, poseStack, mouseX, mouseY) -> this.renderDescription(
                        button,
                        this.component("info.structure_gel.building_tool.expand", RIGHT),
                        "info.structure_gel.building_tool.expand.description",
                        shiftKey
                     )
                  )
            )
         );
      this.widgetsToRender
         .add(
            this.addWidget(
               new TooltipImageButton(
                     texX + 150, texY + 92, w, w, 0 + w, 112, w, BuildingToolScreen.WIDGETS, texSize, texSize, b -> this.shrink(Direction.EAST)
                  )
                  .onTooltip(
                     (button, poseStack, mouseX, mouseY) -> this.renderDescription(
                        button,
                        this.component("info.structure_gel.building_tool.shrink", RIGHT),
                        "info.structure_gel.building_tool.shrink.description",
                        shiftKey
                     )
                  )
            )
         );
      this.widgetsToRender
         .add(
            this.addWidget(
               new TooltipImageButton(texX + 13, texY + 92, w, w, 0, 112, w, BuildingToolScreen.WIDGETS, texSize, texSize, b -> this.expand(Direction.WEST))
                  .onTooltip(
                     (button, poseStack, mouseX, mouseY) -> this.renderDescription(
                        button,
                        this.component("info.structure_gel.building_tool.expand", LEFT),
                        "info.structure_gel.building_tool.expand.description",
                        shiftKey
                     )
                  )
            )
         );
      this.widgetsToRender
         .add(
            this.addWidget(
               new TooltipImageButton(texX + 34, texY + 92, w, w, 0 + w, 112, w, BuildingToolScreen.WIDGETS, texSize, texSize, b -> this.shrink(Direction.WEST))
                  .onTooltip(
                     (button, poseStack, mouseX, mouseY) -> this.renderDescription(
                        button,
                        this.component("info.structure_gel.building_tool.shrink", LEFT),
                        "info.structure_gel.building_tool.shrink.description",
                        shiftKey
                     )
                  )
            )
         );
      this.widgetsToRender
         .add(
            this.addWidget(
               new TooltipImageButton(texX + 151, texY + 136, w, w, 0, 112, w, BuildingToolScreen.WIDGETS, texSize, texSize, b -> this.expand(Direction.UP))
                  .onTooltip(
                     (button, poseStack, mouseX, mouseY) -> this.renderDescription(
                        button, this.component("info.structure_gel.building_tool.expand", TOP), "info.structure_gel.building_tool.expand.description", shiftKey
                     )
                  )
            )
         );
      this.widgetsToRender
         .add(
            this.addWidget(
               new TooltipImageButton(texX + 171, texY + 136, w, w, 0 + w, 112, w, BuildingToolScreen.WIDGETS, texSize, texSize, b -> this.shrink(Direction.UP))
                  .onTooltip(
                     (button, poseStack, mouseX, mouseY) -> this.renderDescription(
                        button, this.component("info.structure_gel.building_tool.shrink", TOP), "info.structure_gel.building_tool.shrink.description", shiftKey
                     )
                  )
            )
         );
      this.widgetsToRender
         .add(
            this.addWidget(
               new TooltipImageButton(texX + 151, texY + 180, w, w, 0, 112, w, BuildingToolScreen.WIDGETS, texSize, texSize, b -> this.expand(Direction.DOWN))
                  .onTooltip(
                     (button, poseStack, mouseX, mouseY) -> this.renderDescription(
                        button,
                        this.component("info.structure_gel.building_tool.expand", BOTTOM),
                        "info.structure_gel.building_tool.expand.description",
                        shiftKey
                     )
                  )
            )
         );
      this.widgetsToRender
         .add(
            this.addWidget(
               new TooltipImageButton(
                     texX + 171, texY + 180, w, w, 0 + w, 112, w, BuildingToolScreen.WIDGETS, texSize, texSize, b -> this.shrink(Direction.DOWN)
                  )
                  .onTooltip(
                     (button, poseStack, mouseX, mouseY) -> this.renderDescription(
                        button,
                        this.component("info.structure_gel.building_tool.shrink", BOTTOM),
                        "info.structure_gel.building_tool.shrink.description",
                        shiftKey
                     )
                  )
            )
         );
      int leftPropY = 80;
      ToolModeProperty.NumberProp<Integer> xProp = ToolModeProperty.X_SIZE;
      this.xEditBox = new PropertyEditBox<>(this.minecraft.font, centerX - 210, centerY - leftPropY, xProp, 0);
      this.xEditBox.setValue(this.bounds.getXSpan());
      this.xEditBox.setResponder(s -> {
         if (xProp.canRead(s)) {
            this.positivePos = new Vec3i(this.negativePos.getX() + xProp.read(s) - 1, this.positivePos.getY(), this.positivePos.getZ());
            this.updateBounds(false);
         }
      });
      this.widgetsToRender.add(this.addWidget(this.xEditBox));
      leftPropY -= 30;
      ToolModeProperty.NumberProp<Integer> yProp = ToolModeProperty.Y_SIZE;
      this.yEditBox = new PropertyEditBox<>(this.minecraft.font, centerX - 210, centerY - leftPropY, yProp, 0);
      this.yEditBox.setValue(this.bounds.getYSpan());
      this.yEditBox.setResponder(s -> {
         if (yProp.canRead(s)) {
            this.positivePos = new Vec3i(this.positivePos.getX(), this.negativePos.getY() + yProp.read(s) - 1, this.positivePos.getZ());
            this.updateBounds(false);
         }
      });
      this.widgetsToRender.add(this.addWidget(this.yEditBox));
      leftPropY -= 30;
      ToolModeProperty.NumberProp<Integer> zProp = ToolModeProperty.Z_SIZE;
      this.zEditBox = new PropertyEditBox<>(this.minecraft.font, centerX - 210, centerY - leftPropY, zProp, 0);
      this.zEditBox.setValue(this.bounds.getZSpan());
      this.zEditBox.setResponder(s -> {
         if (zProp.canRead(s)) {
            this.positivePos = new Vec3i(this.positivePos.getX(), this.positivePos.getY(), this.negativePos.getZ() + zProp.read(s) - 1);
            this.updateBounds(false);
         }
      });
      this.widgetsToRender.add(this.addWidget(this.zEditBox));
      leftPropY -= 30;
   }

   private Component component(String key, Object... args) {
      return Component.translatable(key, args);
   }

   private void expand(Direction direction) {
      direction = this.rotatedByPlayer(direction);
      if (direction.getAxisDirection() == AxisDirection.POSITIVE) {
         this.positivePos = this.positivePos.offset(direction.getNormal());
      } else {
         this.negativePos = this.negativePos.offset(direction.getNormal());
      }

      if (Screen.hasShiftDown()) {
         Direction opposite = direction.getOpposite();
         if (opposite.getAxisDirection() == AxisDirection.POSITIVE) {
            this.positivePos = this.positivePos.subtract(opposite.getNormal());
         } else {
            this.negativePos = this.negativePos.subtract(opposite.getNormal());
         }
      }

      this.updateBounds(true);
   }

   private void shrink(Direction direction) {
      direction = this.rotatedByPlayer(direction);
      if (direction.getAxisDirection() == AxisDirection.POSITIVE) {
         this.positivePos = this.positivePos.subtract(direction.getNormal());
      } else {
         this.negativePos = this.negativePos.subtract(direction.getNormal());
      }

      if (Screen.hasShiftDown()) {
         Direction opposite = direction.getOpposite();
         if (opposite.getAxisDirection() == AxisDirection.POSITIVE) {
            this.positivePos = this.positivePos.offset(opposite.getNormal());
         } else {
            this.negativePos = this.negativePos.offset(opposite.getNormal());
         }
      }

      this.updateBounds(true);
   }

   private Direction rotatedByPlayer(Direction direction) {
      if (direction.getAxis() != Axis.Y) {
         Direction facingDir = this.minecraft.player.getDirection();
         return Direction.from2DDataValue((direction.get2DDataValue() + facingDir.get2DDataValue()) % 4).getOpposite();
      } else {
         return direction;
      }
   }

   private void expandFacing() {
      Direction direction = Direction.orderedByNearest(this.minecraft.player)[0];
      if (direction.getAxisDirection() == AxisDirection.POSITIVE) {
         this.positivePos = this.positivePos.offset(direction.getNormal());
      } else {
         this.negativePos = this.negativePos.offset(direction.getNormal());
      }

      if (Screen.hasShiftDown()) {
         Direction opposite = direction.getOpposite();
         if (opposite.getAxisDirection() == AxisDirection.POSITIVE) {
            this.positivePos = this.positivePos.subtract(opposite.getNormal());
         } else {
            this.negativePos = this.negativePos.subtract(opposite.getNormal());
         }
      }

      this.updateBounds(true);
   }

   private void clearSelection() {
      this.parent.clearPoses = true;
      this.close();
   }

   @Override
   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      super.render(graphics, mouseX, mouseY, partialTick);
      int arrowW = 54;
      int arrowH = 72;
      int topBottomW = 51;
      int topBottomH = 33;
      this.drawHoverable(graphics, mouseX, mouseY, 73, 0, arrowW, arrowH);
      this.drawHoverable(graphics, mouseX, mouseY, 73, 128, arrowW, arrowH);
      this.drawHoverable(graphics, mouseX, mouseY, 0, 73, arrowH, arrowW);
      this.drawHoverable(graphics, mouseX, mouseY, 128, 73, arrowH, arrowW);
      this.drawHoverable(graphics, mouseX, mouseY, 143, 132, topBottomW, topBottomH);
      this.drawHoverable(graphics, mouseX, mouseY, 143, 167, topBottomW, topBottomH);
      this.widgetsToRender.forEach(w -> {
         w.render(graphics, mouseX, mouseY, partialTick);
         RenderSystem.enableBlend();
      });
      if (this.descriptionRenderer.isPresent()) {
         this.descriptionRenderer.get().render(graphics);
         this.descriptionRenderer = Optional.empty();
      }

      Component component = this.parent.mode.getHudMessage(this.parent.stack);
      if (component != null) {
         int length = this.font.width(component);
         graphics.drawString(this.font, component, this.width / 2 - length / 2, this.height / 2 - 112, 14737632);
      }
   }

   private void drawHoverable(GuiGraphics graphics, int mouseX, int mouseY, int u, int v, int width, int height) {
      int centerX = this.width / 2;
      int centerY = this.height / 2;
      int texX = centerX - 100;
      int texY = centerY - 100;
      ResourceLocation texture;
      if (mouseX >= texX + u && mouseX <= texX + u + width && mouseY >= texY + v && mouseY <= texY + v + height) {
         texture = BuildingToolScreen.ARROWS_SELECTED;
      } else {
         texture = BuildingToolScreen.ARROWS;
      }

      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.enableBlend();
      graphics.blit(texture, texX + u, texY + v, u, v, width, height);
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int clickType) {
      return super.mouseClicked(mouseX, mouseY, clickType);
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      Key key = Type.KEYSYM.getOrCreate(keyCode);
      int escape = 256;
      int backspace = 259;
      boolean pressedEnter = keyCode == 257 || keyCode == 335;
      boolean consumed = super.keyPressed(keyCode, scanCode, modifiers);
      if (consumed || backspace != keyCode && !this.minecraft.options.keyDrop.isActiveAndMatches(key)) {
         if (escape != keyCode
            && !this.minecraft.options.keyInventory.isActiveAndMatches(key)
            && !((KeyMapping)ClientProxy.BUILDING_TOOL_KEY.get()).isActiveAndMatches(key)
            && !pressedEnter) {
            return consumed;
         } else {
            this.close();
            return true;
         }
      } else {
         this.back();
         BuildingToolScreen.playClickSound();
         return true;
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

   public void close() {
      this.onClose();
      this.parent.onClose();
      BuildingToolScreen.lastScreenState = 1;
      this.minecraft.setScreen(null);
   }

   public void back() {
      this.onClose();
      BuildingToolScreen.lastScreenState = 0;
      this.minecraft.setScreen(this.parent);
   }

   @Override
   public void onClose() {
      SmartBoundingBox bb = this.updateBounds(false);
      if (this.parent.selectedPosA.isPresent()
         && this.parent.selectedPosB.isPresent()
         && !SmartBoundingBox.fromCorners((Vec3i)this.parent.selectedPosA.get(), (Vec3i)this.parent.selectedPosB.get()).equals(bb)) {
         this.parent.usedEditSelectionScreen = true;
      }

      this.parent.selectedPosA = Optional.of(new BlockPos(bb.getPosA()));
      this.parent.selectedPosB = Optional.of(new BlockPos(bb.getPosB()));
   }

   public SmartBoundingBox updateBounds(boolean updateEditBox) {
      SmartBoundingBox bb = SmartBoundingBox.from(this.bounds);
      bb.setMin(this.negativePos);
      bb.setMax(this.positivePos);
      if (updateEditBox) {
         this.xEditBox.setValue(bb.getXSpan());
         this.yEditBox.setValue(bb.getYSpan());
         this.zEditBox.setValue(bb.getZSpan());
      }

      ItemStack tool = this.minecraft.player.getItemInHand(this.parent.hand);
      if (tool.is(SGRegistry.Items.BUILDING_TOOL.get())) {
         BuildingToolItem.setPos(tool, 0, new BlockPos(bb.getPosA()));
         BuildingToolItem.setPos(tool, 1, new BlockPos(bb.getPosB()));
      }

      return bb;
   }

   public void tick() {
      this.parent.tick();
   }

   public boolean isPauseScreen() {
      return false;
   }
}
