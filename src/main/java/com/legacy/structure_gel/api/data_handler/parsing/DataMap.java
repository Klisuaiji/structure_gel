package com.legacy.structure_gel.api.data_handler.parsing;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

/**
 * Stores data parsed from {@link DataParser}
 * 
 * @author Silver_David
 *
 */
public class DataMap
{
	public Map<String, Object> data = new HashMap<>();

	public DataMap(Map<String, Object> data)
	{
		this.data = data;
	}

	/**
	 * Attempts to get the data stored in this instance, casted to the class passed
	 * 
	 * @param <T>
	 * @param key
	 * @param castTo
	 * @return The casted value stored in the map. May be null if the defaultVal for
	 *         the {@link DataParser} was null
	 * @throws ClassCastException
	 *             When the data stored under "key" doesn't match the class asked
	 *             for
	 */
	public <T> T get(String key, Class<T> castTo) throws ClassCastException
	{
		return castTo.cast(this.data.get(key));
	}

	public byte getByte(String key) throws ClassCastException
	{
		return this.get(key, Byte.class).byteValue();
	}

	public short getShort(String key) throws ClassCastException
	{
		return this.get(key, Short.class).shortValue();
	}

	public int getInt(String key) throws ClassCastException
	{
		return this.get(key, Integer.class).intValue();
	}

	public float getFloat(String key) throws ClassCastException
	{
		return this.get(key, Float.class).floatValue();
	}

	public double getDouble(String key) throws ClassCastException
	{
		return this.get(key, Double.class).doubleValue();
	}

	public boolean getBoolean(String key) throws ClassCastException
	{
		return this.get(key, Boolean.class).booleanValue();
	}

	@SuppressWarnings("unchecked")
	public <T> ResourceKey<T> get(String key, ResourceKey<Registry<T>> castTo) throws ClassCastException
	{
		return (ResourceKey<T>) this.data.get(key);
	}

	/**
	 * @return An unmodifiable view of the data map
	 */
	public Map<String, Object> getData()
	{
		return Collections.unmodifiableMap(this.data);
	}
}
