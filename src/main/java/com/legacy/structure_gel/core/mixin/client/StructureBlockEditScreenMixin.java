package com.legacy.structure_gel.core.mixin.client;

import com.legacy.structure_gel.core.SGConfig;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.StructureBlockEditScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({StructureBlockEditScreen.class})
public abstract class StructureBlockEditScreenMixin extends Screen {
   @Shadow
   private CycleButton<Boolean> toggleAirButton;
   private Checkbox structure_gel$renderAir;
   private Checkbox structure_gel$renderVoid;
   private Checkbox structure_gel$renderBarrier;
   private Checkbox structure_gel$renderLight;

   private StructureBlockEditScreenMixin(Component pTitle) {
      super(pTitle);
   }

   @Inject(
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/screens/inventory/StructureBlockEditScreen;updateMode(Lnet/minecraft/world/level/block/state/properties/StructureMode;)V"
      )},
      method = {"init"}
   )
   private void initHook(CallbackInfo callback) {
      int x = this.toggleAirButton.getX() + this.toggleAirButton.getWidth() + 8;
      int y = this.toggleAirButton.getY();
      int spacing = 20;
      int i = 0;
      this.structure_gel$renderAir = (Checkbox)this.addRenderableWidget(
         this.structure_gel$createCheckBox(
            Blocks.AIR, x, y + spacing * i++, value -> SGConfig.CLIENT.showAir.set(value), (Boolean)SGConfig.CLIENT.showAir.get()
         )
      );
      this.structure_gel$renderVoid = (Checkbox)this.addRenderableWidget(
         this.structure_gel$createCheckBox(
            Blocks.STRUCTURE_VOID, x, y + spacing * i++, value -> SGConfig.CLIENT.showVoid.set(value), (Boolean)SGConfig.CLIENT.showVoid.get()
         )
      );
      this.structure_gel$renderBarrier = (Checkbox)this.addRenderableWidget(
         this.structure_gel$createCheckBox(
            Blocks.BARRIER, x, y + spacing * i++, value -> SGConfig.CLIENT.showBarrier.set(value), (Boolean)SGConfig.CLIENT.showBarrier.get()
         )
      );
      this.structure_gel$renderLight = (Checkbox)this.addRenderableWidget(
         this.structure_gel$createCheckBox(
            Blocks.LIGHT, x, y + spacing * i++, value -> SGConfig.CLIENT.showLight.set(value), (Boolean)SGConfig.CLIENT.showLight.get()
         )
      );
   }

   private Checkbox structure_gel$createCheckBox(Block block, int x, int y, Consumer<Boolean> onPress, boolean isSelected) {
      return Checkbox.builder(Component.empty(), Minecraft.getInstance().font)
         .pos(x, y)
         .onValueChange((c, value) -> onPress.accept(value))
         .selected(isSelected)
         .tooltip(Tooltip.create(Component.translatable("gui.structure_gel.structure_block.show_block", new Object[]{block.getName()})))
         .build();
   }

   @Inject(
      at = {@At("TAIL")},
      method = {"render"}
   )
   private void onUpdateMode(CallbackInfo callback) {
      boolean showInvis = this.toggleAirButton.visible && (Boolean)this.toggleAirButton.getValue();
      this.structure_gel$renderAir.visible = showInvis;
      this.structure_gel$renderVoid.visible = showInvis;
      this.structure_gel$renderBarrier.visible = showInvis;
      this.structure_gel$renderLight.visible = showInvis;
   }
}
