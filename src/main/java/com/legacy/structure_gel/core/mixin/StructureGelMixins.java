package com.legacy.structure_gel.core.mixin;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public class StructureGelMixins implements IMixinConfigPlugin
{

	@Override
	public void onLoad(String mixinPackage)
	{
	}

	@Override
	public String getRefMapperConfig()
	{
		return null;
	}

	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName)
	{
		return false;
	}

	@Override
	public void acceptTargets(Set<String> myTargets, Set<String> otherTargets)
	{
	}

	@Override
	public List<String> getMixins()
	{
		List<String> mixins = new ArrayList<>();

		// Common mixins
		addMixin(mixins, "BlockMixin", "ChunkGeneratorMixin", "StructureMixin", "LakeFeatureMixin", "PoolElementStructurePieceMixin", "SinglePoolElementMixin", "StructureBlockEntityMixin", "StructureTemplateManagerMixin", "TemplateStructurePieceMixin", "LevelTicksMixin");
		
		// Make gel work automatically, handles IModifyState, and IRotatable
		addMixin(mixins, subclasses("TemplateMixins", "StructurePlaceSettingsMixin", "StructureTemplateMixin", "PlaceCommandMixin", "StructureBlockEntityMixin"));
		// Makes ExtendedJigsawStructure data work as intended, with jigsaw capability
		addMixin(mixins, subclasses("JigsawMixins", "JigsawStructureMixin", "JigsawPlacementMixin", "JigsawPlacerMixin"));
		addMixin(mixins, "GenerationContextMixin");
		// Misc Accessors
		addMixin(mixins, "ChunkAccessMixin$Access", "StructureTemplateAccessor");

		// Client mixins
		if (MixinEnvironment.getCurrentEnvironment().getSide() == MixinEnvironment.Side.CLIENT)
		{
			addClientMixin(mixins, "EntityMixin", "GuiMixin", "ItemMixin", "StructureBlockRendererMixin", "StructureBlockEditScreenMixin");
			addClientMixin(mixins, subclasses("ArmorTrimMixins", "SpriteSourceListMixin"));
			
			// Things that that crash with Optifine that we can live without
			if (!doesClassExist("net.optifine.Lang"))
			{
				addClientMixin(mixins, subclasses("ArmorTrimMixins", "EquipmentLayerRendererMixin"));
			}

		}
		return mixins;
	}

	private static String[] subclasses(String mainClass, String... subclasses)
	{
		int l = subclasses.length;
		String[] ret = new String[l];
		for (int i = 0; i < l; i++)
		{
			ret[i] = mainClass + "$" + subclasses[i];
		}
		return ret;
	}

	private static void addMixin(List<String> mixins, String... toAdd)
	{
		addMixin("common", mixins, toAdd);
	}

	private static void addClientMixin(List<String> mixins, String... toAdd)
	{
		addMixin("client", mixins, toAdd);
	}

	private static void addServerMixin(List<String> mixins, String... toAdd)
	{
		addMixin("server", mixins, toAdd);
	}

	private static void addMixin(String sidePackage, List<String> mixins, String... toAdd)
	{
		String prefix = sidePackage + ".";
		for (var s : toAdd)
			mixins.add(prefix + s);
	}

	private static boolean doesClassExist(String classPath)
	{
		try
		{
			Class.forName(classPath);
			return true;
		}
		catch (Throwable t)
		{
			return false;
		}
	}

	@Override
	public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo)
	{
	}

	@Override
	public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo)
	{
	}

}
