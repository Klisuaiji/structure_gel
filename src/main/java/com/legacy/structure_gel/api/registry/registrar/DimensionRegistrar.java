package com.legacy.structure_gel.api.registry.registrar;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.legacy.structure_gel.core.util.Internal;

import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

/**
 * Handles registering dimensions. Create a new instance of this during the
 * {@link FMLCommonSetupEvent} to have it automatically register a dimension
 * with the values passed in.
 * <p>
 * Call
 * {@linkplain RegistrarHandler#registerHandlers(String, net.neoforged.bus.api.IEventBus, RegistrarHandler...)
 * RegistrarHandler.registerHandlers} during your mod's initialization, after
 * classes containing dimension registrars have been initialized.
 *
 * @author Silver_David
 */
public class DimensionRegistrar
{
	@Internal
	public static final List<DimensionRegistrar> REGISTRARS = new ArrayList<>();

	private final Registrar.Pointer<DimensionType> dimensionType;
	private final Registrar.Pointer<NoiseGeneratorSettings> noiseSettings;
	private final Registrar.Pointer<LevelStem> levelStem;
	private final ResourceKey<Level> levelKey;

	/**
	 * @param registryName
	 *            The name of your dimension, dimension type, and noise settings.
	 * @param dimensionType
	 * @param noiseSettings
	 * @param chunkGeneratorFactory
	 *            A function that will create your chunk generator using the
	 *            bootstrap context.
	 */
	public DimensionRegistrar(ResourceLocation registryName, Function<BootstrapContext<?>, DimensionType> dimensionType, Function<BootstrapContext<?>, NoiseGeneratorSettings> noiseSettings, Function<BootstrapContext<?>, ChunkGenerator> chunkGeneratorFactory)
	{
		String modID = registryName.getNamespace();
		var dimTypeHandler = RegistrarHandler.getOrCreate(Registries.DIMENSION_TYPE, modID);
		var noiseSettingsHandler = RegistrarHandler.getOrCreate(Registries.NOISE_SETTINGS, modID);
		var levelStemHandler = RegistrarHandler.getOrCreate(Registries.LEVEL_STEM, modID);
		this.dimensionType = dimTypeHandler.createPointer(ResourceKey.create(Registries.DIMENSION_TYPE, registryName), dimensionType);
		this.noiseSettings = noiseSettingsHandler.createPointer(ResourceKey.create(Registries.NOISE_SETTINGS, registryName), noiseSettings);
		this.levelStem = levelStemHandler.createPointer(ResourceKey.create(Registries.LEVEL_STEM, registryName), c -> new LevelStem(this.dimensionType.getHolder(c).get(), chunkGeneratorFactory.apply(c)));
		this.levelKey = ResourceKey.create(Registries.DIMENSION, registryName);

		REGISTRARS.add(this);
	}

	public Registrar.Pointer<DimensionType> getType()
	{
		return this.dimensionType;
	}

	public Registrar.Pointer<NoiseGeneratorSettings> getNoiseSettings()
	{
		return this.noiseSettings;
	}

	public Registrar.Pointer<LevelStem> getLevelStem()
	{
		return this.levelStem;
	}

	public ResourceKey<Level> getLevelKey()
	{
		return this.levelKey;
	}
}
