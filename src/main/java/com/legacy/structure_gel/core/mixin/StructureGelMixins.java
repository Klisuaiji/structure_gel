package com.legacy.structure_gel.core.mixin;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.MixinEnvironment.Side;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public class StructureGelMixins implements IMixinConfigPlugin {
   public void onLoad(String mixinPackage) {
   }

   public String getRefMapperConfig() {
      return null;
   }

   public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
      return false;
   }

   public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
   }

   public List<String> getMixins() {
      List<String> mixins = new ArrayList<>();
      addMixin(
         mixins,
         "BlockMixin",
         "ChunkGeneratorMixin",
         "StructureMixin",
         "EntityMixin",
         "LakeFeatureMixin",
         "PoolElementStructurePieceMixin",
         "SinglePoolElementMixin",
         "StructureBlockEntityMixin",
         "StructureTemplateManagerMixin",
         "TemplateStructurePieceMixin",
         "LevelTicksMixin"
      );
      addMixin(mixins, subclasses("TemplateMixins", "StructurePlaceSettingsMixin", "StructureTemplateMixin", "PlaceCommandMixin", "StructureBlockEntityMixin"));
      addMixin(mixins, subclasses("JigsawMixins", "JigsawStructureMixin", "JigsawPlacementMixin", "JigsawPlacerMixin"));
      addMixin(mixins, "GenerationContextMixin");
      addMixin(mixins, "ChunkAccessMixin$Access", "DimensionDataStorageAccessor", "StructureTemplateAccessor");
      if (MixinEnvironment.getCurrentEnvironment().getSide() == Side.CLIENT) {
         addClientMixin(mixins, "EndPortalBlockMixin", "GuiMixin", "ItemMixin", "StructureBlockRendererMixin", "StructureBlockEditScreenMixin");
         addClientMixin(mixins, subclasses("ArmorTrimMixins", "SpriteSourceListMixin", "ArmorTrimMixin"));
         if (!doesClassExist("net.optifine.Lang")) {
            addClientMixin(mixins, subclasses("ArmorTrimMixins", "HumanoidArmorLayerMixin"));
         }
      }

      return mixins;
   }

   private static String[] subclasses(String mainClass, String... subclasses) {
      int l = subclasses.length;
      String[] ret = new String[l];

      for (int i = 0; i < l; i++) {
         ret[i] = mainClass + "$" + subclasses[i];
      }

      return ret;
   }

   private static void addMixin(List<String> mixins, String... toAdd) {
      addMixin("common", mixins, toAdd);
   }

   private static void addClientMixin(List<String> mixins, String... toAdd) {
      addMixin("client", mixins, toAdd);
   }

   private static void addServerMixin(List<String> mixins, String... toAdd) {
      addMixin("server", mixins, toAdd);
   }

   private static void addMixin(String sidePackage, List<String> mixins, String... toAdd) {
      String prefix = sidePackage + ".";

      for (String s : toAdd) {
         mixins.add(prefix + s);
      }
   }

   private static boolean doesClassExist(String classPath) {
      try {
         Class.forName(classPath);
         return true;
      } catch (Throwable var2) {
         return false;
      }
   }

   public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
   }

   public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
   }
}
