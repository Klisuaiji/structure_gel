package com.legacy.structure_gel.core.mixin.common;

import java.util.Optional;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.legacy.structure_gel.api.events.LoadStructureTemplateEvent;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

@Mixin(StructureTemplateManager.class)
public class StructureTemplateManagerMixin
{
	
	@Inject(at = @At(value = "RETURN"), method = "tryLoad")
	private void onLoad(ResourceLocation id, CallbackInfoReturnable<Optional<StructureTemplate>> callback)
	{
		if (callback.getReturnValue().isPresent() && ServerLifecycleHooks.getCurrentServer() != null)
		{
			StructureTemplateAccessor template = (StructureTemplateAccessor) (Object) callback.getReturnValue().get();
			NeoForge.EVENT_BUS.post(new LoadStructureTemplateEvent(id, template.getPalettes(), template.getEntityInfoList(), template.getSize(), ServerLifecycleHooks.getCurrentServer()));
		}
	}
}
