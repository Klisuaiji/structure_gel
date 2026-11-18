package com.legacy.structure_gel.core.mixin.common;

import com.legacy.structure_gel.api.events.LoadStructureTemplateEvent;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.neoforged.neoforge.common.NeoForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({StructureTemplateManager.class})
public class StructureTemplateManagerMixin {
   @Inject(
      at = {@At("RETURN")},
      method = {"tryLoad"}
   )
   private void onLoad(ResourceLocation id, CallbackInfoReturnable<Optional<StructureTemplate>> callback) {
      if (((Optional)callback.getReturnValue()).isPresent()) {
         StructureTemplateAccessor template = (StructureTemplateAccessor)((Optional)callback.getReturnValue()).get();
         NeoForge.EVENT_BUS.post(new LoadStructureTemplateEvent(id, template.getPalettes(), template.getEntityInfoList(), template.getSize()));
      }
   }
}
