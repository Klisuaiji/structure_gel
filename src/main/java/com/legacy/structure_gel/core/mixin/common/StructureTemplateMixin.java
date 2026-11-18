package com.legacy.structure_gel.core.mixin.common;

import com.legacy.structure_gel.api.block_entity.IRotatable;
import java.util.Iterator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin({StructureTemplate.class})
public abstract class StructureTemplateMixin {
   @Inject(
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/level/block/entity/BlockEntity;load(Lnet/minecraft/nbt/CompoundTag;)V",
         shift = Shift.AFTER
      )},
      method = {"placeInWorld"},
      locals = LocalCapture.CAPTURE_FAILEXCEPTION
   )
   private void loadBlockEntity(
      ServerLevelAccessor p_230329_,
      BlockPos p_230330_,
      BlockPos p_230331_,
      StructurePlaceSettings settings,
      RandomSource p_230333_,
      int p_230334_,
      CallbackInfoReturnable<Boolean> callback,
      List list,
      BoundingBox boundingbox,
      List list1,
      List list2,
      List list3,
      int i,
      int j,
      int k,
      int l,
      int i1,
      int j1,
      Iterator iterator,
      StructureBlockInfo info,
      BlockPos pos,
      FluidState fluidState,
      BlockState blockState,
      BlockEntity blockentity1
   ) {
      if (blockentity1 instanceof IRotatable r) {
         r.transform(settings.getMirror(), settings.getRotation());
      }
   }
}
