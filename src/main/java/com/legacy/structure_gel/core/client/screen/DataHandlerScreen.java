package com.legacy.structure_gel.core.client.screen;

import com.legacy.structure_gel.api.data_handler.DataHandlerType;
import com.legacy.structure_gel.api.registry.StructureGelRegistries;
import com.legacy.structure_gel.core.StructureGelMod;
import com.legacy.structure_gel.core.block.DataHandlerBlock;
import com.legacy.structure_gel.core.block_entity.DataHandlerBlockEntity;
import com.legacy.structure_gel.core.client.widget.DataParamsList;
import com.legacy.structure_gel.core.client.widget.SuggestionEditBox;
import com.legacy.structure_gel.core.client.widget.TooltipImageButton;
import com.legacy.structure_gel.core.network.SGPacketHandler;
import com.legacy.structure_gel.core.network.c_to_s.UpdateDataHandlerPacket;
import com.legacy.structure_gel.core.util.SGText;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.random.Weight;
import net.minecraft.world.phys.Vec3;

public class DataHandlerScreen extends Screen {
   private static final ResourceLocation WIDGETS = StructureGelMod.locate("textures/gui/widgets.png");
   private static final String APPLY_AT_HEIGHTMAP_KEY = "gui.structure_gel.data_handler.apply_at_heightmap";
   private final List<DataHandlerBlockEntity.RawHandler> handlers = new ArrayList<>();
   private final List<Map<ResourceLocation, List<DataParamsList.Entry>>> dataParamsCache = new ArrayList<>();
   private int visibleIndex = 0;
   private final DataHandlerBlockEntity dataHandler;
   private boolean useGravity = false;
   private boolean waterlogged = false;
   private boolean connectToBlocks = false;
   private boolean markPostProcessing = false;
   private SuggestionEditBox typeEdit;
   private DataParamsList dataParamsList;
   public EditBox weightEdit;
   public EditBox nameEdit;
   public EditBox xOffsetEdit;
   public EditBox yOffsetEdit;
   public EditBox zOffsetEdit;
   public Button doneButton;
   public Button generateButton;
   public Button nextButton;
   public Button prevButton;
   public TooltipImageButton toggleGravityButton;
   public TooltipImageButton toggleWaterloggedButton;
   public TooltipImageButton toggleConnectToBlocksButton;
   public TooltipImageButton togglePostProcessingButton;

   public DataHandlerScreen(DataHandlerBlockEntity dataHandler) {
      super(Component.empty());
      this.dataHandler = dataHandler;
      this.handlers.addAll(dataHandler.getHandlers().unwrap());
      this.useGravity = dataHandler.useGravity();
      this.markPostProcessing = dataHandler.shouldMarkPostProcessing();
      this.waterlogged = (Boolean)dataHandler.getBlockState().getValue(DataHandlerBlock.WATERLOGGED);
      this.connectToBlocks = (Boolean)dataHandler.getBlockState().getValue(DataHandlerBlock.CONNECT_TO_BLOCKS);

      for (int i = 0; i < this.handlers.size(); i++) {
         this.dataParamsCache.add(new HashMap<>());
      }

      if (this.handlers.isEmpty()) {
         this.handlers.add(DataHandlerBlockEntity.RawHandler.EMPTY);
      }

      if (this.dataParamsCache.isEmpty()) {
         this.dataParamsCache.add(new HashMap<>());
      }
   }

   public void tick() {
      this.dataParamsList.tick();
      this.doneButton.active = this.allowDoneButton();
      this.generateButton.active = this.allowGenerateButton();
      boolean allValid = this.generateButton.active && this.generateButton.active;
      this.prevButton.active = this.visibleIndex > 0 && (this.typeEdit.getValue().isEmpty() || allValid);
      this.nextButton.active = allValid;
   }

   private void onDone(byte flag) {
      this.beforePageChange();
      this.sendToServer(flag);
      this.minecraft.setScreen((Screen)null);
   }

   private void onCancel() {
      this.minecraft.setScreen((Screen)null);
   }

   private void sendToServer(byte flag) {
      Vec3 offsetVec;
      try {
         offsetVec = new Vec3(
            Double.valueOf(this.xOffsetEdit.getValue()), Double.valueOf(this.yOffsetEdit.getValue()), Double.valueOf(this.zOffsetEdit.getValue())
         );
      } catch (NumberFormatException var4) {
         offsetVec = Vec3.ZERO;
      }

      SGPacketHandler.sendToServer(
         new UpdateDataHandlerPacket(
            flag,
            this.dataHandler.getBlockPos(),
            this.handlers,
            this.nameEdit.getValue(),
            this.useGravity,
            this.waterlogged,
            offsetVec,
            this.connectToBlocks,
            this.markPostProcessing
         )
      );
   }

   public void onClose() {
      this.onCancel();
   }

   protected void init() {
      int centerX = this.width / 2;
      int centerY = this.height / 2;
      int buttonHeight = 20;
      this.typeEdit = new SuggestionEditBox(
         this.font, centerX - 152, 25, 215, buttonHeight, SGText.TYPE_LABEL, 10, StructureGelRegistries.DATA_HANDLER_TYPE.keySet()
      );
      this.typeEdit.setMaxLength(128);
      this.typeEdit.setResponder(s -> this.updateWidgets());
      this.addWidget(this.typeEdit);
      this.weightEdit = new EditBox(
         this.font,
         this.typeEdit.getX() + this.typeEdit.getWidth() + 7,
         this.typeEdit.getY(),
         303 - this.typeEdit.getWidth() - 6,
         this.typeEdit.getHeight(),
         Component.empty()
      );
      this.weightEdit.setMaxLength(8);
      this.weightEdit.setValue("1");
      this.weightEdit.setResponder(s -> this.updateWidgets());
      this.addRenderableWidget(this.weightEdit);
      this.dataParamsList = new DataParamsList(this, this.minecraft, null);
      this.addWidget(this.dataParamsList);
      int totalRowWidth = 300;
      int bottomRowStart = centerX - totalRowWidth / 2;
      int bottomRowIndex = 0;
      int bottomRowWidth = totalRowWidth / 3;
      this.doneButton = (Button)this.addRenderableWidget(
         Button.builder(CommonComponents.GUI_DONE, button -> this.onDone((byte)0))
            .pos(bottomRowStart + bottomRowWidth * bottomRowIndex++, this.height - 30)
            .size(bottomRowWidth, buttonHeight)
            .build()
      );
      this.addRenderableWidget(
         Button.builder(CommonComponents.GUI_CANCEL, button -> this.onCancel())
            .pos(bottomRowStart + bottomRowWidth * bottomRowIndex++, this.doneButton.getY())
            .size(bottomRowWidth, buttonHeight)
            .build()
      );
      this.generateButton = (Button)this.addRenderableWidget(
         Button.builder(Component.translatable("jigsaw_block.generate"), button -> this.onDone((byte)1))
            .pos(bottomRowStart + bottomRowWidth * bottomRowIndex++, this.doneButton.getY())
            .size(bottomRowWidth, buttonHeight)
            .build()
      );
      int nextButtonWidth = 16;
      int nextButtonHeight = 32;
      int nextButtonXOffset = 170;
      this.prevButton = (Button)this.addRenderableWidget(
         new TooltipImageButton(
            centerX - (nextButtonXOffset + nextButtonWidth),
            centerY - nextButtonHeight / 2,
            nextButtonWidth,
            nextButtonHeight,
            0,
            0,
            WIDGETS,
            button -> this.previous()
         )
      );
      this.nextButton = (Button)this.addRenderableWidget(
         new TooltipImageButton(
            centerX + nextButtonXOffset, centerY - nextButtonHeight / 2, nextButtonWidth, nextButtonHeight, nextButtonWidth, 0, WIDGETS, button -> this.next()
         )
      );
      int imageButtonWidth = 20;
      TooltipImageButton propertiesButton = new TooltipImageButton(
         this.typeEdit.getX() - 35, this.typeEdit.getY() + 1, imageButtonWidth, imageButtonWidth, 32, 0, WIDGETS, button -> this.toggleProperties()
      );
      propertiesButton.setTooltip(Tooltip.create(SGText.PROPERTIES_LABEL));
      this.addRenderableWidget(propertiesButton);
      int toggleButtonSpacing = imageButtonWidth + 5;
      int toggleButtonX = this.typeEdit.getX();
      this.toggleGravityButton = new TooltipImageButton(
         toggleButtonX, propertiesButton.getY(), imageButtonWidth, imageButtonWidth, this.useGravity ? 52 : 72, 0, WIDGETS, button -> this.toggleGravity()
      );
      this.toggleGravityButton
         .setTooltip(Tooltip.create(Component.translatable("gui.structure_gel.data_handler.apply_at_heightmap", new Object[]{this.useGravity})));
      this.toggleWidget(this.toggleGravityButton, false);
      this.addRenderableWidget(this.toggleGravityButton);
      toggleButtonX += toggleButtonSpacing;
      this.toggleWaterloggedButton = new TooltipImageButton(
         toggleButtonX,
         propertiesButton.getY(),
         imageButtonWidth,
         imageButtonWidth,
         this.waterlogged ? 92 : 112,
         0,
         WIDGETS,
         button -> this.toggleWaterlogged()
      );
      this.toggleWaterloggedButton.setTooltip(Tooltip.create(Component.translatable("gui.structure_gel.waterlogged", new Object[]{this.waterlogged})));
      this.toggleWidget(this.toggleWaterloggedButton, false);
      this.addRenderableWidget(this.toggleWaterloggedButton);
      toggleButtonX += toggleButtonSpacing;
      this.toggleConnectToBlocksButton = new TooltipImageButton(
         toggleButtonX,
         propertiesButton.getY(),
         imageButtonWidth,
         imageButtonWidth,
         this.connectToBlocks ? 132 : 152,
         0,
         WIDGETS,
         button -> this.toggleConnectToBlocks()
      );
      this.toggleConnectToBlocksButton
         .setTooltip(Tooltip.create(Component.translatable("gui.structure_gel.connect_to_blocks", new Object[]{this.connectToBlocks})));
      this.toggleWidget(this.toggleConnectToBlocksButton, false);
      this.addRenderableWidget(this.toggleConnectToBlocksButton);
      toggleButtonX += toggleButtonSpacing;
      this.togglePostProcessingButton = new TooltipImageButton(
         toggleButtonX,
         propertiesButton.getY(),
         imageButtonWidth,
         imageButtonWidth,
         this.markPostProcessing ? 172 : 192,
         0,
         WIDGETS,
         button -> this.togglePostProcessing()
      );
      this.togglePostProcessingButton
         .setTooltip(Tooltip.create(Component.translatable("gui.structure_gel.mark_post_processing", new Object[]{this.markPostProcessing})));
      this.toggleWidget(this.togglePostProcessingButton, false);
      this.addRenderableWidget(this.togglePostProcessingButton);
      toggleButtonX += toggleButtonSpacing;
      this.nameEdit = new EditBox(
         this.font, centerX - this.typeEdit.getWidth() / 2, this.typeEdit.getY() + 50, this.typeEdit.getWidth(), this.typeEdit.getHeight(), Component.empty()
      );
      Component name = this.dataHandler.getCustomName();
      if (name != null) {
         this.nameEdit.setValue(name.getString());
      }

      this.toggleWidget(this.nameEdit, false);
      this.addWidget(this.nameEdit);
      Vec3 offset = this.dataHandler.getOffset();
      int offsetEditWidth = 30;
      int offsetEditSpacing = 20;
      this.xOffsetEdit = new EditBox(
         this.font, this.nameEdit.getX() + 10, this.nameEdit.getY() + 40, offsetEditWidth, this.typeEdit.getHeight(), Component.empty()
      );
      this.xOffsetEdit.setValue(Double.toString(offset.x()));
      this.toggleWidget(this.xOffsetEdit, false);
      this.xOffsetEdit.setTooltip(Tooltip.create(SGText.LEFT_LABEL));
      this.addWidget(this.xOffsetEdit);
      this.yOffsetEdit = new EditBox(
         this.font,
         this.xOffsetEdit.getX() + this.xOffsetEdit.getWidth() + offsetEditSpacing,
         this.xOffsetEdit.getY(),
         offsetEditWidth,
         this.typeEdit.getHeight(),
         Component.empty()
      );
      this.yOffsetEdit.setValue(Double.toString(offset.y()));
      this.toggleWidget(this.yOffsetEdit, false);
      this.yOffsetEdit.setTooltip(Tooltip.create(SGText.UP_LABEL));
      this.addWidget(this.yOffsetEdit);
      this.zOffsetEdit = new EditBox(
         this.font,
         this.yOffsetEdit.getX() + this.yOffsetEdit.getWidth() + offsetEditSpacing,
         this.yOffsetEdit.getY(),
         offsetEditWidth,
         this.typeEdit.getHeight(),
         Component.empty()
      );
      this.zOffsetEdit.setValue(Double.toString(offset.z()));
      this.toggleWidget(this.zOffsetEdit, false);
      this.zOffsetEdit.setTooltip(Tooltip.create(SGText.FORWARD_LABEL));
      this.addWidget(this.zOffsetEdit);
      this.updateWidgets();
      this.injectData();
   }

   private void toggleProperties() {
      boolean active = !this.nameEdit.visible;
      this.toggleWidget(this.typeEdit, !active);
      this.toggleWidget(this.weightEdit, !active);
      this.dataParamsList.showScrollBar = !active;

      for (DataParamsList.Entry entry : this.dataParamsList.children()) {
         this.toggleWidget(entry.widget, !active);
      }

      this.prevButton.visible = !active;
      this.nextButton.visible = !active;
      this.toggleWidget(this.nameEdit, active);
      this.toggleWidget(this.xOffsetEdit, active);
      this.toggleWidget(this.yOffsetEdit, active);
      this.toggleWidget(this.zOffsetEdit, active);
      this.toggleWidget(this.toggleGravityButton, active);
      this.toggleWidget(this.toggleWaterloggedButton, active);
      this.toggleWidget(this.toggleConnectToBlocksButton, active);
      this.toggleWidget(this.togglePostProcessingButton, active);
   }

   private void toggleWidget(AbstractWidget widget, boolean active) {
      widget.visible = active;
      widget.active = active;
      widget.setFocused(false);
      if (widget instanceof EditBox edit) {
         edit.setEditable(active);
      }
   }

   private void toggleGravity() {
      this.useGravity = !this.useGravity;
      this.toggleGravityButton
         .setTooltip(Tooltip.create(Component.translatable("gui.structure_gel.data_handler.apply_at_heightmap", new Object[]{this.useGravity})));
      this.toggleGravityButton.u = this.useGravity ? 52 : 72;
   }

   private void toggleWaterlogged() {
      this.waterlogged = !this.waterlogged;
      this.toggleWaterloggedButton.setTooltip(Tooltip.create(Component.translatable("gui.structure_gel.waterlogged", new Object[]{this.waterlogged})));
      this.toggleWaterloggedButton.u = this.waterlogged ? 92 : 112;
   }

   private void toggleConnectToBlocks() {
      this.connectToBlocks = !this.connectToBlocks;
      this.toggleConnectToBlocksButton
         .setTooltip(Tooltip.create(Component.translatable("gui.structure_gel.connect_to_blocks", new Object[]{this.connectToBlocks})));
      this.toggleConnectToBlocksButton.u = this.connectToBlocks ? 132 : 152;
   }

   private void togglePostProcessing() {
      this.markPostProcessing = !this.markPostProcessing;
      this.togglePostProcessingButton
         .setTooltip(Tooltip.create(Component.translatable("gui.structure_gel.mark_post_processing", new Object[]{this.markPostProcessing})));
      this.togglePostProcessingButton.u = this.markPostProcessing ? 172 : 192;
   }

   private void next() {
      int size = this.handlers.size();
      if (this.visibleIndex < size) {
         this.beforePageChange();
         this.visibleIndex++;
         if (this.visibleIndex == size) {
            this.handlers.add(DataHandlerBlockEntity.RawHandler.EMPTY);
            this.dataParamsCache.add(new HashMap<>());
         }

         this.updateWidgets();
         this.injectData();
      }
   }

   private void previous() {
      if (this.visibleIndex > 0) {
         this.beforePageChange();
         this.visibleIndex--;
         this.updateWidgets();
         this.injectData();
      }
   }

   public boolean allowDoneButton() {
      return ResourceLocation.isValidResourceLocation(this.typeEdit.getValue()) && this.dataParamsList.isValid() && this.isWeightValid();
   }

   public boolean allowGenerateButton() {
      return this.allowDoneButton()
         && StructureGelRegistries.DATA_HANDLER_TYPE.get(new ResourceLocation(this.typeEdit.getValue())) != null
         && this.dataParamsList.isValid()
         && this.isWeightValid()
         && !this.nameEdit.isVisible();
   }

   public boolean isWeightValid() {
      try {
         int weight = Integer.parseInt(this.weightEdit.getValue());
         return weight >= 1;
      } catch (Exception var2) {
         return false;
      }
   }

   private void beforePageChange() {
      Weight weight = Weight.of(Integer.parseInt(this.weightEdit.getValue()));
      ResourceLocation typeName = new ResourceLocation(this.typeEdit.getValue());
      LinkedHashMap<String, String> dataMap = new LinkedHashMap<>();

      for (DataParamsList.Entry entry : this.dataParamsList.children()) {
         dataMap.put(entry.parser.key, entry.getValue());
      }

      if (this.visibleIndex < this.handlers.size()) {
         this.handlers.set(this.visibleIndex, new DataHandlerBlockEntity.RawHandler(weight, typeName, dataMap));
      }
   }

   private void injectData() {
      DataHandlerBlockEntity.RawHandler handler = this.visibleIndex < this.handlers.size()
         ? this.handlers.get(this.visibleIndex)
         : DataHandlerBlockEntity.RawHandler.EMPTY;
      this.weightEdit.setValue(handler.getWeight().asInt() + "");
      ResourceLocation typeName = handler.typeName();
      this.typeEdit.setValue(typeName.getPath().isEmpty() ? "" : typeName.toString());
      LinkedHashMap<String, String> dataMap = handler.dataEntries();

      for (DataParamsList.Entry entry : this.dataParamsList.children()) {
         String val = dataMap.get(entry.parser.key);
         if (val != null) {
            entry.setValue(val);
         }
      }
   }

   private void updateWidgets() {
      this.weightEdit.setTextColor(this.isWeightValid() ? 14737632 : 14703708);
      String typeStr = this.typeEdit.getValue();
      boolean typeFlag = ResourceLocation.isValidResourceLocation(this.typeEdit.getValue());
      this.typeEdit.setTextColor(typeFlag ? 14737632 : 14703708);
      this.doneButton.active = this.allowDoneButton();
      this.generateButton.active = this.allowGenerateButton();
      if (!typeFlag) {
         this.dataParamsList.setEntries(null);
      } else {
         ResourceLocation typeName = new ResourceLocation(typeStr);
         DataHandlerType<?> type = (DataHandlerType<?>)StructureGelRegistries.DATA_HANDLER_TYPE.get(typeName);
         if (type != null) {
            Map<ResourceLocation, List<DataParamsList.Entry>> cache = this.dataParamsCache.get(this.visibleIndex);
            List<DataParamsList.Entry> cachedParams = cache.get(typeName);
            if (cachedParams == null) {
               this.dataParamsList.createEntries(type.getDataParser());
               cache.put(typeName, this.dataParamsList.childrenClone());
            } else {
               this.dataParamsList.setEntries(cachedParams);
            }
         } else {
            this.dataParamsList.setEntries(null);
         }

         boolean allValid = this.doneButton.active && this.generateButton.active;
         this.prevButton.active = this.visibleIndex > 0 && (this.typeEdit.getValue().isEmpty() || allValid);
         this.nextButton.active = allValid;
      }
   }

   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      super.render(graphics, mouseX, mouseY, partialTicks);
      this.dataParamsList.render(graphics, mouseX, mouseY, partialTicks);
      if (this.typeEdit.isVisible()) {
         graphics.drawString(this.font, SGText.TYPE_LABEL, this.typeEdit.getX(), this.typeEdit.getY() - 14, 10526880);
         graphics.drawString(
            this.font,
            Component.translatable("info.structure_gel.building_tool.property.weight"),
            this.weightEdit.getX(),
            this.weightEdit.getY() - 14,
            10526880
         );
         this.typeEdit.render(graphics, mouseX, mouseY, partialTicks);
      } else {
         graphics.drawString(this.font, SGText.PROPERTIES_LABEL, this.typeEdit.getX(), this.typeEdit.getY() - 14, 10526880);
      }

      if (this.nameEdit.isVisible()) {
         graphics.drawString(this.font, SGText.DISPLAY_NAME_LABEL, this.nameEdit.getX(), this.nameEdit.getY() - 14, 10526880);
         this.nameEdit.render(graphics, mouseX, mouseY, partialTicks);
      }

      int labelX = 11;
      int labelY = 6;
      if (this.xOffsetEdit.isVisible()) {
         graphics.drawString(this.font, SGText.OFFSET_LABEL, this.nameEdit.getX(), this.xOffsetEdit.getY() - 14, 10526880);
         graphics.drawString(this.font, Component.literal("←"), this.xOffsetEdit.getX() - labelX, this.xOffsetEdit.getY() + labelY, 10526880);
         this.xOffsetEdit.render(graphics, mouseX, mouseY, partialTicks);
      }

      if (this.yOffsetEdit.isVisible()) {
         graphics.drawString(this.font, Component.literal("↑"), this.yOffsetEdit.getX() - labelX, this.yOffsetEdit.getY() + labelY, 10526880);
         this.yOffsetEdit.render(graphics, mouseX, mouseY, partialTicks);
      }

      if (this.zOffsetEdit.isVisible()) {
         graphics.drawString(this.font, Component.literal("↙"), this.zOffsetEdit.getX() - labelX, this.zOffsetEdit.getY() + labelY, 10526880);
         this.zOffsetEdit.render(graphics, mouseX, mouseY, partialTicks);
      }
   }

   public void renderTransparentBackground(GuiGraphics graphics) {
      PoseStack pose = graphics.pose();
      pose.pushPose();
      pose.translate(0.0F, 0.0F, -2.0F);
      super.renderTransparentBackground(graphics);
      pose.popPose();
   }

   public void renderTooltip(GuiGraphics graphics, List<Component> lines, int mouseX, int mouseY) {
      graphics.renderTooltip(this.font, lines, Optional.empty(), mouseX, mouseY);
   }

   public void resize(Minecraft mc, int mouseX, int mouseY) {
      String type = this.typeEdit.getValue();
      this.init(mc, mouseX, mouseY);
      this.typeEdit.setValue(type);
   }

   public boolean mouseClicked(double mouseX, double mouseY, int clickType) {
      for (DataParamsList.Entry entry : this.dataParamsList.children()) {
         if (entry.widget.isFocused() && entry.widget.mouseClicked(mouseX, mouseY, clickType)) {
            return true;
         }

         entry.widget.setFocused(false);
      }

      return super.mouseClicked(mouseX, mouseY, clickType);
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
      return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
   }

   public boolean keyPressed(int keyCode, int keyB, int modifiers) {
      if (this.typeEdit.isFocused() && this.typeEdit.keyPressed(keyCode, keyB, modifiers)) {
         return true;
      } else {
         for (DataParamsList.Entry entry : this.dataParamsList.children()) {
            if (entry.widget.isFocused() && entry.widget.keyPressed(keyCode, keyB, modifiers)) {
               return true;
            }
         }

         if (super.keyPressed(keyCode, keyB, modifiers)) {
            return true;
         } else if (!this.doneButton.active || keyCode != 257 && keyCode != 335) {
            return false;
         } else {
            this.onDone((byte)0);
            return true;
         }
      }
   }
}
