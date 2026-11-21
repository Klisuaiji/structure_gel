package com.legacy.structure_gel.core.mixin.client;

import java.util.function.Consumer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.legacy.structure_gel.core.SGConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.StructureBlockEditScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

@Mixin(StructureBlockEditScreen.class)
public abstract class StructureBlockEditScreenMixin extends Screen
{
	private StructureBlockEditScreenMixin(Component pTitle)
	{
		super(pTitle);
	}

	@Shadow
	private CycleButton<Boolean> toggleAirButton;

	private Checkbox structure_gel$renderAir, structure_gel$renderVoid, structure_gel$renderBarrier,
			structure_gel$renderLight;

	@Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/StructureBlockEditScreen;updateMode(Lnet/minecraft/world/level/block/state/properties/StructureMode;)V"), method = "init")
	private void initHook(CallbackInfo callback)
	{		
		int x = this.toggleAirButton.getX() + this.toggleAirButton.getWidth() + 8;
		int y = this.toggleAirButton.getY();
		int spacing = 20;
		int i = 0;
		this.structure_gel$renderAir = this.addRenderableWidget(structure_gel$createCheckBox(Blocks.AIR, x, y + spacing * i++, value -> SGConfig.CLIENT.showAir.set(value), SGConfig.CLIENT.showAir.get()));
		this.structure_gel$renderVoid = this.addRenderableWidget(structure_gel$createCheckBox(Blocks.STRUCTURE_VOID, x, y + spacing * i++, value -> SGConfig.CLIENT.showVoid.set(value), SGConfig.CLIENT.showVoid.get()));
		this.structure_gel$renderBarrier = this.addRenderableWidget(structure_gel$createCheckBox(Blocks.BARRIER, x, y + spacing * i++, value -> SGConfig.CLIENT.showBarrier.set(value), SGConfig.CLIENT.showBarrier.get()));
		this.structure_gel$renderLight = this.addRenderableWidget(structure_gel$createCheckBox(Blocks.LIGHT, x, y + spacing * i++, value -> SGConfig.CLIENT.showLight.set(value), SGConfig.CLIENT.showLight.get()));
	}
	
	private Checkbox structure_gel$createCheckBox(Block block, int x, int y, Consumer<Boolean> onPress, boolean isSelected)
	{
		return Checkbox.builder(Component.empty(), Minecraft.getInstance().font).pos(x, y).onValueChange((c, value) -> onPress.accept(value)).selected(isSelected).tooltip(Tooltip.create(Component.translatable("gui.structure_gel.structure_block.show_block", block.getName()))).build();
	}

	@Inject(at = @At("TAIL"), method = "render")
	private void onUpdateMode(CallbackInfo callback)
	{
		boolean showInvis = this.toggleAirButton.visible && this.toggleAirButton.getValue();
		this.structure_gel$renderAir.visible = showInvis;
		this.structure_gel$renderVoid.visible = showInvis;
		this.structure_gel$renderBarrier.visible = showInvis;
		this.structure_gel$renderLight.visible = showInvis;
	}
}
