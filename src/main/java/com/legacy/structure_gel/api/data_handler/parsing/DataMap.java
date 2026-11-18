package com.legacy.structure_gel.api.data_handler.parsing;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

public class DataMap {
   public Map<String, Object> data = new HashMap<>();

   public DataMap(Map<String, Object> data) {
      this.data = data;
   }

   public <T> T get(String key, Class<T> castTo) throws ClassCastException {
      return castTo.cast(this.data.get(key));
   }

   public byte getByte(String key) throws ClassCastException {
      return this.get(key, Byte.class);
   }

   public short getShort(String key) throws ClassCastException {
      return this.get(key, Short.class);
   }

   public int getInt(String key) throws ClassCastException {
      return this.get(key, Integer.class);
   }

   public float getFloat(String key) throws ClassCastException {
      return this.get(key, Float.class);
   }

   public double getDouble(String key) throws ClassCastException {
      return this.get(key, Double.class);
   }

   public boolean getBoolean(String key) throws ClassCastException {
      return this.get(key, Boolean.class);
   }

   public <T> ResourceKey<T> get(String key, ResourceKey<Registry<T>> castTo) throws ClassCastException {
      return (ResourceKey<T>)this.data.get(key);
   }

   public Map<String, Object> getData() {
      return Collections.unmodifiableMap(this.data);
   }
}
