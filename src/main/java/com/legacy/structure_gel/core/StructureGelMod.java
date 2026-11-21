package com.legacy.structure_gel.core;

import com.legacy.structure_gel.core.util.Internal;
import com.legacy.structure_gel.core.util.LoggerWrapper;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;

/**
 * This is an API with the purpose of giving access and shortcuts to various
 * aspects of worldgen, such as structures, biomes, and dimensions. Methods and
 * classes are documented with details on how they work and where to use them.
 * Anywhere that you see the {@link Internal} annotation means that you
 * shouldn't need to call the thing annotated.<br>
 * <br>
 * If you encounter issues or find any bugs, please report them to the issue
 * tracker. https://gitlab.com/modding-legacy/structure-gel-api/-/issues
 *
 * @author Silver_David
 */
@Mod(StructureGelMod.MODID)
@Internal
public class StructureGelMod
{
	public static final String MODID = "structure_gel";
	public static final LoggerWrapper LOGGER = new LoggerWrapper(MODID);
	public static SGProxy proxy = new SGProxy();

	public StructureGelMod(IEventBus modBus, ModContainer mod)
	{
		mod.registerConfig(ModConfig.Type.COMMON, SGConfig.COMMON_SPEC);

		if (FMLEnvironment.dist == Dist.CLIENT)
		{
			proxy = new com.legacy.structure_gel.core.client.ClientProxy();
			mod.registerConfig(ModConfig.Type.CLIENT, SGConfig.CLIENT_SPEC);
		}

		com.legacy.structure_gel.core.SGAccessor.init();
	}

	public static ResourceLocation locate(String key)
	{
		return ResourceLocation.fromNamespaceAndPath(MODID, key);
	}

	public static final boolean IS_IDE = isRunningFromIDE();

	private static boolean isRunningFromIDE()
	{
		String p = System.getProperty(String.format("%s.iside", MODID));
		return Boolean.parseBoolean(p);
	}
}
