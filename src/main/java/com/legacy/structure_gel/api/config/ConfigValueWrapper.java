package com.legacy.structure_gel.api.config;

import java.util.function.Function;
import java.util.function.Supplier;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;

/**
 * Stores a config value and a function to convert it into useful data when
 * needed, while also caching said data for future use.
 * <p>
 * Useful in cases where a list of strings may need to be converted into a list
 * of blocks, and you don't want to do a registry lookup every time you need the
 * config value.
 * 
 * @author Silver_David
 *
 * @param <O>
 *            The original value in the config
 * @param <P>
 *            The value parsed through the {@link #parseFunction}
 */
public class ConfigValueWrapper<O, P> implements Supplier<P>
{
	public static <O, P> ConfigValueWrapper<O, P> create(ConfigValue<O> configValue, Function<O, P> parseFunction, IEventBus modEventBus, String modID)
	{
		return new ConfigValueWrapper<>(configValue, parseFunction, modEventBus, modID);
	}

	public static <O> ConfigValueWrapper<O, O> create(ConfigValue<O> configValue, IEventBus modEventBus, String modID)
	{
		return create(configValue, Function.identity(), modEventBus, modID);
	}

	private static final Multimap<String, ConfigValueWrapper<?, ?>> LISTENERS = HashMultimap.create();
	private final ConfigValue<O> configValue;
	private final Function<O, P> parseFunction;
	private P cached = null;
	private boolean resolved = false;

	private ConfigValueWrapper(ConfigValue<O> configValue, Function<O, P> parseFunction, IEventBus modEventBus, String modID)
	{
		this.configValue = configValue;
		this.parseFunction = parseFunction;
		LISTENERS.put(modID, this);
		if (!LISTENERS.containsKey(modID))
			modEventBus.register(ConfigValueWrapper.class);
	}

	@Override
	public P get()
	{
		if (!this.resolved)
		{
			this.cached = this.parseFunction.apply(this.configValue.get());
			this.resolved = true;
		}
		return this.cached;
	}

	public P getDefault()
	{
		return this.parseFunction.apply(this.configValue.getDefault());
	}

	public ConfigValue<O> getConfigValue()
	{
		return this.configValue;
	}

	public boolean isResolved()
	{
		return this.resolved;
	}
	
	protected void clearCache()
	{
		this.cached = null;
		this.resolved = false;
	}

	@SubscribeEvent
	protected static void onConfigLoad(final ModConfigEvent.Loading event)
	{
		LISTENERS.get(event.getConfig().getModId()).forEach(ConfigValueWrapper::clearCache);
	}

	@SubscribeEvent
	protected static void onConfigReload(final ModConfigEvent.Reloading event)
	{
		LISTENERS.get(event.getConfig().getModId()).forEach(ConfigValueWrapper::clearCache);
	}
}
