package com.legacy.structure_gel.core.mixin.common;

import com.legacy.structure_gel.api.block.base.IStructureGel;
import com.legacy.structure_gel.api.block_entity.IRotatable;
import com.legacy.structure_gel.api.structure.base.IModifyState;
import com.legacy.structure_gel.api.structure.processor.RemoveGelStructureProcessor;
import com.legacy.structure_gel.core.capability.misc.StructureSettingsData;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.commands.PlaceCommand;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

public class TemplateMixins {
   @Mixin({PlaceCommand.class})
   public static class PlaceCommandMixin {
      @ModifyExpressionValue(
         at = {@At(
            value = "NEW",
            target = "net/minecraft/world/level/levelgen/structure/templatesystem/StructurePlaceSettings"
         )},
         method = {"placeTemplate"}
      )
      private static StructurePlaceSettings modifySettings_placeTemplate(StructurePlaceSettings original) {
         ((StructureSettingsData)original).structure_gel$setNatural(false);
         return original;
      }
   }

   @Mixin({StructureBlockEntity.class})
   public static class StructureBlockEntityMixin {
      @ModifyExpressionValue(
         at = {@At(
            value = "NEW",
            target = "net/minecraft/world/level/levelgen/structure/templatesystem/StructurePlaceSettings"
         )},
         method = {"placeStructure(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructureTemplate;)V"}
      )
      private StructurePlaceSettings modifySettings_placeStructure(StructurePlaceSettings original) {
         ((StructureSettingsData)original).structure_gel$setNatural(false);
         return original;
      }
   }

   @Mixin({StructurePlaceSettings.class})
   public static class StructurePlaceSettingsMixin implements StructureSettingsData {
      private boolean structure_gel$setNatural = true;
      private IModifyState structure_gel$stateModifier = IModifyState.NOOP;

      @Override
      public void structure_gel$setNatural(boolean natural) {
         this.structure_gel$setNatural = natural;
      }

      @Override
      public boolean structure_gel$isNatural() {
         return this.structure_gel$setNatural;
      }

      @Override
      public void structure_gel$setStateModifier(IModifyState modifier) {
         this.structure_gel$stateModifier = modifier;
      }

      @Override
      public IModifyState structure_gel$getStateModifier() {
         return this.structure_gel$stateModifier;
      }
   }

   @Mixin({StructureTemplate.class})
   public static class StructureTemplateMixin {
      @Inject(
         at = {@At("HEAD")},
         remap = false,
         method = {"processBlockInfos(Lnet/minecraft/world/level/ServerLevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructurePlaceSettings;Ljava/util/List;Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructureTemplate;)Ljava/util/List;"}
      )
      private static void addGelProcessor_processBlockInfos(
         ServerLevelAccessor pServerLevel,
         BlockPos pOffset,
         BlockPos p_74520_,
         StructurePlaceSettings settings,
         List<StructureBlockInfo> blockInfos,
         @Nullable StructureTemplate template,
         CallbackInfoReturnable<List<StructureBlockInfo>> callback
      ) {
         StructureSettingsData settingsData = (StructureSettingsData)settings;
         if (settingsData.structure_gel$isNatural()
            && !settings.getProcessors().contains(RemoveGelStructureProcessor.INSTANCE)
            && blockInfos.stream().anyMatch(info -> info.state().getBlock() instanceof IStructureGel)) {
            settings.getProcessors().add(0, RemoveGelStructureProcessor.INSTANCE);
         }
      }

      @Inject(
         at = {@At("TAIL")},
         remap = false,
         method = {"processBlockInfos(Lnet/minecraft/world/level/ServerLevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructurePlaceSettings;Ljava/util/List;Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructureTemplate;)Ljava/util/List;"}
      )
      private static void modifyStates_processBlockInfos(
         ServerLevelAccessor level,
         BlockPos pOffset,
         BlockPos p_74520_,
         StructurePlaceSettings settings,
         List<StructureBlockInfo> blockInfos,
         @Nullable StructureTemplate template,
         CallbackInfoReturnable<List<StructureBlockInfo>> callback
      ) {
         StructureSettingsData settingsData = (StructureSettingsData)settings;
         if (settingsData.structure_gel$isNatural()) {
            IModifyState stateModifier = settingsData.structure_gel$getStateModifier();
            if (stateModifier != IModifyState.NOOP) {
               RandomSource rand = level.getRandom();
               List<StructureBlockInfo> processedBlocks = (List<StructureBlockInfo>)callback.getReturnValue();
               List<Integer> toRemove = new ArrayList<>();
               int size = processedBlocks.size();

               for (int i = 0; i < size; i++) {
                  StructureBlockInfo info = processedBlocks.get(i);
                  BlockState state = info.state();
                  BlockPos infoPos = info.pos();
                  BlockState modifiedState = stateModifier.modifyState(level, rand, infoPos, state);
                  if (modifiedState == null) {
                     toRemove.add(i);
                  } else if (state != modifiedState) {
                     processedBlocks.set(i, new StructureBlockInfo(infoPos, modifiedState, info.nbt()));
                  }
               }

               int removeCount = toRemove.size();
               if (removeCount > 0) {
                  for (int ix = removeCount - 1; ix > -1; ix--) {
                     processedBlocks.remove(toRemove.get(ix).intValue());
                  }
               }
            }
         }
      }

      @Inject(
         at = {@At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/entity/BlockEntity;load(Lnet/minecraft/nbt/CompoundTag;)V",
            shift = Shift.AFTER
         )},
         method = {"placeInWorld"}
      )
      private void placeInWorld_loadBlockEntity(
         ServerLevelAccessor p_230329_,
         BlockPos p_230330_,
         BlockPos p_230331_,
         StructurePlaceSettings settings,
         RandomSource p_230333_,
         int p_230334_,
         CallbackInfoReturnable<Boolean> callback,
         @Local(ordinal = 0) BlockEntity blockentity1
      ) {
         if (blockentity1 instanceof IRotatable r) {
            r.transform(settings.getMirror(), settings.getRotation());
         }
      }
   }
}
