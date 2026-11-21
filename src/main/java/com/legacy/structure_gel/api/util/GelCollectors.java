package com.legacy.structure_gel.api.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Contains methods to merge different types of collections, creating mutable
 * versions.
 *
 * @author Silver_David
 */
public class GelCollectors
{
	/**
	 * Creates a new mutable map with the values passed
	 * 
	 * @param <K>
	 *            The key type of the map
	 * @param <V>
	 *            The value type of the map
	 * @param key
	 *            The first key in the map
	 * @param value
	 *            The first value in the map
	 * @param keysAndValues
	 *            An array of keys and values
	 * @return A new mutable map with the values passed
	 * @throws ClassCastException
	 *             If an object in keysAndValues cannot be casted to the key or
	 *             value class
	 * @throws ArrayIndexOutOfBoundsException
	 *             If keysAndValues has an odd number of objects
	 */
	@SuppressWarnings("unchecked")
	public static <K, V> Map<K, V> mapOf(K key, V value, Object... keysAndValues) throws ClassCastException, ArrayIndexOutOfBoundsException
	{
		Map<K, V> map = new HashMap<>((keysAndValues.length / 2) + 1);
		map.put(key, value);
		for (int i = 0; i < keysAndValues.length; i += 2)
			map.put((K) keysAndValues[i], (V) keysAndValues[i + 1]);
		return map;
	}

	public static <T> List<T> merge(List<T> list, T obj)
	{
		List<T> newList = new ArrayList<>(list);
		newList.add(obj);
		return newList;
	}

	public static <T> List<T> merge(List<T> list1, Collection<T> list2)
	{
		List<T> newList = new ArrayList<>(list1);
		newList.addAll(list2);
		return newList;
	}

	public static <T> Set<T> merge(Set<T> set, T obj)
	{
		Set<T> newSet = new HashSet<>(set);
		newSet.add(obj);
		return newSet;
	}

	public static <T> Set<T> merge(Set<T> set1, Collection<T> set2)
	{
		Set<T> newSet = new HashSet<>(set1);
		newSet.addAll(set2);
		return newSet;
	}

	public static <K, V> Map<K, V> merge(Map<K, V> map, K key, V value)
	{
		var newMap = new HashMap<>(map);
		newMap.put(key, value);
		return newMap;
	}

	public static <K, V> Map<K, V> merge(Map<K, V> map1, Map<K, V> map2)
	{
		var newMap = new HashMap<>(map1);
		newMap.putAll(map2);
		return newMap;
	}
}
