package com.legacy.structure_gel.core.mixin.common;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.legacy.structure_gel.api.block_entity.IRotatable;
import com.legacy.structure_gel.api.data.tags.SGTags;
import com.legacy.structure_gel.api.structure.base.IModifyState;
import com.legacy.structure_gel.api.structure.processor.RemoveGelStructureProcessor;
import com.legacy.structure_gel.core.capability.misc.StructureSettingsData;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.commands.PlaceCommand;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;

public class TemplateMixins
{
	@Mixin(StructurePlaceSettings.class)
	public static class StructurePlaceSettingsMixin implements StructureSettingsData
	{
		private boolean structure_gel$setNatural = true;
		private IModifyState structure_gel$stateModifier = IModifyState.NOOP;

		@Override
		public void structure_gel$setNatural(boolean natural)
		{
			//System.out.println("setNatural: " + natural);
			structure_gel$setNatural = natural;
		}

		@Override
		public boolean structure_gel$isNatural()
		{
			return structure_gel$setNatural;
		}

		@Override
		public void structure_gel$setStateModifier(IModifyState modifier)
		{
			structure_gel$stateModifier = modifier;
		}

		@Override
		public IModifyState structure_gel$getStateModifier()
		{
			return structure_gel$stateModifier;
		}
	}

	@Mixin(StructureTemplate.class)
	public static class StructureTemplateMixin
	{
		//@formatter:off
		@Inject(
				at = @At("HEAD"), 
				remap = false, 
				method = "processBlockInfos(Lnet/minecraft/world/level/ServerLevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructurePlaceSettings;Ljava/util/List;Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructureTemplate;)Ljava/util/List;")
		//@formatter:on
		private static void addGelProcessor_processBlockInfos(ServerLevelAccessor pServerLevel, BlockPos pOffset, BlockPos p_74520_, StructurePlaceSettings settings, List<StructureTemplate.StructureBlockInfo> blockInfos, @Nullable StructureTemplate template, CallbackInfoReturnable<List<StructureTemplate.StructureBlockInfo>> callback)
		{
			StructureSettingsData settingsData = (StructureSettingsData) settings;
			if (settingsData.structure_gel$isNatural() && !settings.getProcessors().contains(RemoveGelStructureProcessor.INSTANCE) && blockInfos.stream().anyMatch(info -> info.state().is(SGTags.BlockTags.GEL)))
			{
				// Add the processor as the first one that operates. If we do it last, jigsaws that place air end up getting their air voided
				settings.getProcessors().add(0, RemoveGelStructureProcessor.INSTANCE);
				//System.out.println("Added processor");
			}
		}

		//@formatter:off
		@Inject(
				at = @At("TAIL"), 
				remap = false, 
				method = "processBlockInfos(Lnet/minecraft/world/level/ServerLevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructurePlaceSettings;Ljava/util/List;Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructureTemplate;)Ljava/util/List;")
		//@formatter:on
		private static void modifyStates_processBlockInfos(ServerLevelAccessor level, BlockPos pOffset, BlockPos p_74520_, StructurePlaceSettings settings, List<StructureTemplate.StructureBlockInfo> blockInfos, @Nullable StructureTemplate template, CallbackInfoReturnable<List<StructureTemplate.StructureBlockInfo>> callback)
		{
			StructureSettingsData settingsData = (StructureSettingsData) settings;
			if (settingsData.structure_gel$isNatural())
			{
				IModifyState stateModifier = settingsData.structure_gel$getStateModifier();
				if (stateModifier != IModifyState.NOOP)
				{
					RandomSource rand = level.getRandom();
					List<StructureTemplate.StructureBlockInfo> processedBlocks = callback.getReturnValue();
					List<Integer> toRemove = new ArrayList<>();
					int size = processedBlocks.size();
					for (int i = 0; i < size; i++)
					{
						StructureBlockInfo info = processedBlocks.get(i);
						BlockState state = info.state();
						BlockPos infoPos = info.pos();
						BlockState modifiedState = stateModifier.modifyState(level, rand, infoPos, state);
						if (modifiedState == null)
						{
							toRemove.add(i);
						}
						else if (state != modifiedState)
						{
							processedBlocks.set(i, new StructureBlockInfo(infoPos, modifiedState, info.nbt()));
						}
					}

					// Remove in inverse order. If we remove the lower index first, all others shift. Removing the top elements first prevents that.
					int removeCount = toRemove.size();
					if (removeCount > 0)
					{
						for (int i = removeCount - 1; i > -1; i--)
						{
							processedBlocks.remove(toRemove.get(i).intValue());
						}
					}
				}
			}
		}

		// After BlockEntity.load, run the rotate stuff. This needs to have access to the blockentity1 being loaded, so local capture is used. Thank you MixinExtras
		//@formatter:off
		//@formatter:off
		@WrapOperation(at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/BlockEntity;loadWithComponents(Lnet/minecraft/nbt/CompoundTag;Lnet/minecraft/core/HolderLookup$Provider;)V"), method = "placeInWorld")
		private void loadBlockEntity(
				BlockEntity target, 
				CompoundTag tag, HolderLookup.Provider lookup, 
				Operation<Void> original,
				ServerLevelAccessor p_230329_, BlockPos p_230330_, BlockPos p_230331_, StructurePlaceSettings settings)
		//@formatter:on
		{
			original.call(target, tag, lookup);
			
			if (target instanceof IRotatable r)
				r.transform(settings.getMirror(), settings.getRotation());
		}
	}

	@Mixin(PlaceCommand.class)
	public static class PlaceCommandMixin
	{
		//@formatter:off
		@ModifyExpressionValue(
				at = @At(
						value = "NEW", 
						target = "net/minecraft/world/level/levelgen/structure/templatesystem/StructurePlaceSettings"), 
				method = "placeTemplate")
		//@formatter:on
		private static StructurePlaceSettings modifySettings_placeTemplate(StructurePlaceSettings original)
		{
			((StructureSettingsData) original).structure_gel$setNatural(false);
			return original;
		}
	}

	@Mixin(StructureBlockEntity.class)
	public static class StructureBlockEntityMixin
	{
		//@formatter:off
		@ModifyExpressionValue(
				at = @At(
						value = "NEW",
						target = "net/minecraft/world/level/levelgen/structure/templatesystem/StructurePlaceSettings"
						),
				method = "placeStructure(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructureTemplate;)V")
		//@formatter:on
		private StructurePlaceSettings modifySettings_placeStructure(StructurePlaceSettings original)
		{
			((StructureSettingsData) original).structure_gel$setNatural(false);
			return original;
		}
	}
}
