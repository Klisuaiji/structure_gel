package com.legacy.structure_gel.api.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GelCollectors {
   public static <K, V> Map<K, V> mapOf(K key, V value, Object... keysAndValues) throws ClassCastException, ArrayIndexOutOfBoundsException {
      Map<K, V> map = new HashMap<>(keysAndValues.length / 2 + 1);
      map.put(key, value);

      for (int i = 0; i < keysAndValues.length; i += 2) {
         map.put((K)keysAndValues[i], (V)keysAndValues[i + 1]);
      }

      return map;
   }

   public static <T> List<T> merge(List<T> list, T obj) {
      List<T> newList = new ArrayList<>(list);
      newList.add(obj);
      return newList;
   }

   public static <T> List<T> merge(List<T> list1, Collection<T> list2) {
      List<T> newList = new ArrayList<>(list1);
      newList.addAll(list2);
      return newList;
   }

   public static <T> Set<T> merge(Set<T> set, T obj) {
      Set<T> newSet = new HashSet<>(set);
      newSet.add(obj);
      return newSet;
   }

   public static <T> Set<T> merge(Set<T> set1, Collection<T> set2) {
      Set<T> newSet = new HashSet<>(set1);
      newSet.addAll(set2);
      return newSet;
   }

   public static <K, V> Map<K, V> merge(Map<K, V> map, K key, V value) {
      HashMap<K, V> newMap = new HashMap<>(map);
      newMap.put(key, value);
      return newMap;
   }

   public static <K, V> Map<K, V> merge(Map<K, V> map1, Map<K, V> map2) {
      HashMap<K, V> newMap = new HashMap<>(map1);
      newMap.putAll(map2);
      return newMap;
   }
}
