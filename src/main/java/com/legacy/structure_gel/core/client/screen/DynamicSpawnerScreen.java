package com.legacy.structure_gel.core.client.screen;

import com.legacy.structure_gel.api.registry.StructureGelRegistries;
import com.legacy.structure_gel.core.block_entity.DynamicSpawnerBlockEntity;
import com.legacy.structure_gel.core.client.widget.RenderWidget;
import com.legacy.structure_gel.core.client.widget.SuggestionEditBox;
import com.legacy.structure_gel.core.network.SGPacketHandler;
import com.legacy.structure_gel.core.network.c_to_s.UpdateDynamicSpawnerPacket;
import com.legacy.structure_gel.core.util.SGText;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class DynamicSpawnerScreen extends Screen {
   private static final Component EXP_RANGE_LABEL = Component.translatable("gui.structure_gel.dynamic_spawner.exp_range").withStyle(ChatFormatting.UNDERLINE);
   private final DynamicSpawnerBlockEntity spawner;
   private SuggestionEditBox typeEdit;
   private Button doneButton;
   private EditBox minEdit;
   private EditBox maxEdit;

   public DynamicSpawnerScreen(DynamicSpawnerBlockEntity spawner) {
      super(Component.empty());
      this.spawner = spawner;
   }

   private void onDone(byte flag) {
      this.sendToServer();
      this.minecraft.setScreen((Screen)null);
   }

   private void onCancel() {
      this.minecraft.setScreen((Screen)null);
   }

   private void sendToServer() {
      DynamicSpawnerBlockEntity.ExpRange range = new DynamicSpawnerBlockEntity.ExpRange(
         Integer.parseInt(this.minEdit.getValue()), Integer.parseInt(this.maxEdit.getValue())
      );
      SGPacketHandler.sendToServer(new UpdateDynamicSpawnerPacket(this.spawner.getBlockPos(), new ResourceLocation(this.typeEdit.getValue()), range));
   }

   public void onClose() {
      this.onCancel();
   }

   protected void init() {
      int centerX = this.width / 2;
      int centerY = this.height / 2;
      int editHeight = 20;
      int editSpacing = editHeight + 20;
      int typeWidth = 300;
      this.typeEdit = new SuggestionEditBox(
         this.font, centerX - typeWidth / 2, centerY - 60, typeWidth, editHeight, SGText.TYPE_LABEL, 10, StructureGelRegistries.DYNAMIC_SPAWNER_TYPE.keySet()
      );
      this.typeEdit.setMaxLength(128);
      ResourceLocation type = this.spawner.getSpawnerID();
      if (type != null) {
         this.typeEdit.setValue(type.toString());
      }

      this.typeEdit.setResponder(s -> this.updateWidgets());
      this.addWidget(this.typeEdit);
      DynamicSpawnerBlockEntity.ExpRange expRange = this.spawner.getExpRange();
      int minMaxWidth = 60;
      this.minEdit = new EditBox(this.font, this.typeEdit.getX(), this.typeEdit.getY() + editSpacing + 20, minMaxWidth, editHeight, SGText.MIN_LABEL);
      this.minEdit.setMaxLength(8);
      this.minEdit.setValue(expRange.min + "");
      this.minEdit.setResponder(s -> this.updateWidgets());
      this.addWidget(this.minEdit);
      this.maxEdit = new EditBox(this.font, this.minEdit.getX() + this.minEdit.getWidth() + 10, this.minEdit.getY(), minMaxWidth, editHeight, SGText.MAX_LABEL);
      this.maxEdit.setMaxLength(8);
      this.maxEdit.setValue(expRange.max + "");
      this.maxEdit.setResponder(s -> this.updateWidgets());
      this.addWidget(this.maxEdit);
      this.doneButton = (Button)this.addRenderableWidget(
         Button.builder(CommonComponents.GUI_DONE, button -> this.onDone((byte)0)).pos(centerX - 3 - 150, this.height - 30).size(150, 20).build()
      );
      this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> this.onCancel()).pos(centerX + 4, this.height - 30).size(150, 20).build());
      this.addRenderableOnly(
         RenderWidget.builder(this.font, this.typeEdit.getX(), this.typeEdit.getY() - 14, 80, 10)
            .setTooltip("gui.structure_gel.dynamic_spawner.type.tooltip", ChatFormatting.GRAY)
            .setLabel(SGText.TYPE_LABEL, 10526880)
            .build()
      );
      this.addRenderableOnly(
         RenderWidget.builder(this.font, this.minEdit.getX(), this.minEdit.getY() - 26, 80, 10)
            .setTooltip("gui.structure_gel.dynamic_spawner.exp_range.tooltip", ChatFormatting.GRAY)
            .setLabel(EXP_RANGE_LABEL, 10526880)
            .build()
      );
      this.updateWidgets();
   }

   private boolean isValid() {
      boolean isValid = true;
      this.typeEdit.setTextColor(14737632);
      this.minEdit.setTextColor(14737632);
      this.maxEdit.setTextColor(14737632);
      if (!ResourceLocation.isValidResourceLocation(this.typeEdit.getValue())) {
         this.typeEdit.setTextColor(14703708);
         isValid = false;
      }

      String minStr = this.minEdit.getValue();
      String maxStr = this.maxEdit.getValue();
      if (minStr.isBlank() || maxStr.isBlank()) {
         isValid = false;
      }

      try {
         int min = Integer.parseInt(minStr);
         int max = Integer.parseInt(maxStr);
         if (max < min || min < 0 || max < 0) {
            this.minEdit.setTextColor(14703708);
            this.maxEdit.setTextColor(14703708);
            isValid = false;
         }
      } catch (Exception var8) {
         try {
            Integer.parseInt(minStr);
         } catch (Exception var7) {
            this.minEdit.setTextColor(14703708);
         }

         try {
            Integer.parseInt(maxStr);
         } catch (Exception var6) {
            this.maxEdit.setTextColor(14703708);
         }

         isValid = false;
      }

      return isValid;
   }

   private void updateWidgets() {
      this.doneButton.active = this.isValid();
   }

   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      super.render(graphics, mouseX, mouseY, partialTicks);
      graphics.drawString(this.font, SGText.MIN_LABEL, this.minEdit.getX(), this.minEdit.getY() - 14, 10526880);
      graphics.drawString(this.font, SGText.MAX_LABEL, this.maxEdit.getX(), this.maxEdit.getY() - 14, 10526880);
      this.minEdit.render(graphics, mouseX, mouseY, partialTicks);
      this.maxEdit.render(graphics, mouseX, mouseY, partialTicks);
      this.typeEdit.render(graphics, mouseX, mouseY, partialTicks);
   }

   public void resize(Minecraft mc, int mouseX, int mouseY) {
      String type = this.typeEdit.getValue();
      String min = this.minEdit.getValue();
      String max = this.maxEdit.getValue();
      this.init(mc, mouseX, mouseY);
      this.typeEdit.setValue(type);
      this.minEdit.setValue(min);
      this.maxEdit.setValue(max);
   }

   public boolean keyPressed(int keyCode, int keyB, int modifiers) {
      if (this.typeEdit.isFocused() && this.typeEdit.keyPressed(keyCode, keyB, modifiers)) {
         return true;
      } else if (super.keyPressed(keyCode, keyB, modifiers)) {
         return true;
      } else if (!this.doneButton.active || keyCode != 257 && keyCode != 335) {
         return false;
      } else {
         this.onDone((byte)0);
         return true;
      }
   }
}
