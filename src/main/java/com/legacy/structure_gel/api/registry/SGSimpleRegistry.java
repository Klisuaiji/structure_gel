package com.legacy.structure_gel.api.registry;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;
import com.legacy.structure_gel.core.StructureGelMod;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.ModLoader;
import net.neoforged.fml.event.IModBusEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * A basic registry class with event handling on {@link #init()}. All data is
 * stored in a {@link BiMap}.
 * 
 * @author Silver_David
 *
 * @param <K>
 *            The key to register objects as
 * @param <V>
 *            The value to register
 */
public class SGSimpleRegistry<K, V> implements Iterable<V>
{
	private final BiMap<K, V> registry = HashBiMap.create();
	private final ResourceLocation name;
	private final Supplier<V> defaultValue;
	@Nullable
	private final Supplier<Event> event;
	private boolean frozen = false;

	/**
	 * @param name
	 *            The name of this registry
	 * @param defaultValue
	 *            A supplier for the default value
	 * @param event
	 *            An event that fires during {@link #init()} to flag mods to
	 *            register data. Pass null to not use an event.
	 */
	public SGSimpleRegistry(ResourceLocation name, Supplier<V> defaultValue, @Nullable Supplier<Event> event)
	{
		this.name = name;
		this.defaultValue = defaultValue;
		this.event = event;
	}

	/**
	 * Call this when data is expected to be registered. Fires the registry
	 * {@link #event} after {@link #preInit()}.
	 */
	public final void init()
	{
		if (!this.frozen)
		{
			if (this.event != null)
			{
				StructureGelMod.LOGGER.info("Registering data for " + this.getRegistryName());
				this.preInit();
				Event event = this.event.get();
				if (event instanceof IModBusEvent)
				{
					ModLoader.postEvent((Event & IModBusEvent) event);
				}
				else
				{
					NeoForge.EVENT_BUS.post(event);
				}
				this.frozen = true;
			}
		}
	}

	/**
	 * Fires before the registry {@link #event}. Implement this method for special
	 * registry handling.
	 */
	protected void preInit()
	{

	}

	@Nullable
	public K getKey(V value)
	{
		return this.registry.inverse().get(value);
	}
	
	@Nullable
	public K getKey(V value, @Nullable K defaultKey)
	{
		return this.registry.inverse().getOrDefault(value, defaultKey);
	}

	@Nullable
	public V get(K key)
	{
		return this.get(key, this.getDefaultValue());
	}

	@Nullable
	public V get(K key, @Nullable V defaultValue)
	{
		return this.registry.getOrDefault(key, defaultValue);
	}

	public boolean containsKey(K key)
	{
		return this.registry.containsKey(key);
	}

	public boolean containsValue(V value)
	{
		return this.registry.containsValue(value);
	}

	/**
	 * @return An unmodifiable view of the keys in this registry
	 */
	public Set<K> getKeys()
	{
		return Collections.unmodifiableSet(this.registry.keySet());
	}

	/**
	 * @return An unmodifiable view of the values in this registry
	 */
	public Set<V> getValues()
	{
		return Collections.unmodifiableSet(this.registry.values());
	}

	/**
	 * @return An unmodifiable view of this registry
	 */
	public BiMap<K, V> getRegistry()
	{
		return Maps.unmodifiableBiMap(this.registry);
	}

	/**
	 * Attempts to register the passed object under the given key
	 * 
	 * @param <O>
	 * @param key
	 * @param value
	 * @return value
	 * @throws IllegalStateException
	 *             If the registry has already been frozen
	 * @throws IllegalArgumentException
	 *             If the key or value passed has already been registered
	 */
	public <O extends V> O register(K key, O value) throws IllegalStateException, IllegalArgumentException
	{
		if (this.frozen)
			throw new IllegalStateException("Attempted to register an object after registry data has been frozen. Registry = " + this.name + ", Entry = [" + key + " = " + value + "]");
		if (this.registry.putIfAbsent(key, value) != null)
			throw new IllegalArgumentException("Attempted to register an object under an existing name. Registry = " + this.name + ", Entry = [" + key + " = " + value + "]");
		return value;
	}

	/**
	 * Forces the passed object to be registered, bypassing logic in the standard
	 * register method
	 * 
	 * @param <O>
	 * @param key
	 * @param value
	 * @return value
	 * @throws IllegalStateException
	 */
	protected <O extends V> O forceRegister(K key, O value) throws IllegalStateException
	{
		if (this.frozen)
			throw new IllegalStateException("Attempted to register an object after registry data has been frozen. Registry = " + this.name + ", Entry = [" + key + " = " + value + "]");
		this.registry.forcePut(key, value);
		return value;
	}

	public ResourceLocation getRegistryName()
	{
		return this.name;
	}

	public V getDefaultValue()
	{
		return this.defaultValue.get();
	}

	public Stream<Map.Entry<K, V>> stream()
	{
		return this.registry.entrySet().stream();
	}

	public void forEach(BiConsumer<K, V> action)
	{
		this.registry.forEach(action);
	}
	
	@Override
	public Iterator<V> iterator()
	{
		return this.registry.values().iterator();
	}

	@Override
	public String toString()
	{
		return "SGRegistry: " + this.name;
	}
}
