package com.legacy.structure_gel.api.config;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.Builder;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;
import net.neoforged.neoforge.common.ModConfigSpec.DoubleValue;
import net.neoforged.neoforge.common.ModConfigSpec.EnumValue;
import net.neoforged.neoforge.common.ModConfigSpec.IntValue;
import net.neoforged.neoforge.common.ModConfigSpec.LongValue;

public final class ConfigBuilder {
   public static <T> ConfigValue<T> makeConfig(Builder builder, String name, String comment, T defaultVal) {
      return builder.comment(comment(comment, defaultVal)).define(name, defaultVal);
   }

   public static <T> ConfigValue<T> makeConfig(Builder builder, String name, String comment, Object example, T defaultVal) {
      return builder.comment(comment(comment, defaultVal, example)).define(name, defaultVal);
   }

   public static BooleanValue makeBoolean(Builder builder, String name, String comment, boolean defaultVal) {
      return builder.comment(comment(comment, defaultVal)).define(name, defaultVal);
   }

   public static IntValue makeInt(Builder builder, String name, String comment, int defaultVal, int min, int max) {
      return builder.comment(comment(comment, defaultVal)).defineInRange(name, defaultVal, min, max);
   }

   public static LongValue makeLong(Builder builder, String name, String comment, long defaultVal, long min, long max) {
      return builder.comment(comment(comment, defaultVal)).defineInRange(name, defaultVal, min, max);
   }

   public static DoubleValue makeDouble(Builder builder, String name, String comment, double defaultVal, double min, double max) {
      return builder.comment(comment(comment, defaultVal)).defineInRange(name, defaultVal, min, max);
   }

   public static <T extends Enum<T>> EnumValue<T> makeEnum(Builder builder, String name, String comment, T defaultVal) {
      return builder.comment(comment(comment, defaultVal)).defineEnum(name, defaultVal);
   }

   public static <T> ConfigValue<List<? extends T>> makeList(Builder builder, String name, String comment, List<T> defaultVal, Predicate<Object> validator) {
      return builder.comment(comment(comment, defaultVal)).defineList(name, defaultVal, validator);
   }

   public static <T> ConfigValue<List<? extends T>> makeEmptyList(Builder builder, String name, String comment, List<T> defaultVal, Predicate<Object> validator) {
      return builder.comment(comment(comment, defaultVal)).defineListAllowEmpty(List.of(name), () -> defaultVal, validator);
   }

   public static <T> ConfigValue<List<? extends T>> makeList(
      Builder builder, String name, String comment, Object example, List<T> defaultVal, Predicate<Object> validator
   ) {
      return builder.comment(comment(comment, defaultVal, example)).defineList(name, defaultVal, validator);
   }

   public static <T> ConfigValue<List<? extends T>> makeEmptyList(
      Builder builder, String name, String comment, Object example, List<T> defaultVal, Predicate<Object> validator
   ) {
      return builder.comment(comment(comment, defaultVal, example)).defineListAllowEmpty(List.of(name), () -> defaultVal, validator);
   }

   public static String comment(String description, Object defaultVal, Object example) {
      return String.format((description.isEmpty() ? "%s" : "\n %s") + "\n Example: %s\n Default: %s", description, valString(example), valString(defaultVal));
   }

   public static String comment(String description, Object defaultVal) {
      return String.format((description.isEmpty() ? "%s" : "\n %s") + "\n Default: %s", description, valString(defaultVal));
   }

   private static String valString(Object val) {
      if (val instanceof List<?> list) {
         return list.isEmpty() ? "[]" : "[\"" + String.join("\", \"", list.stream().map(Object::toString).collect(Collectors.toList())) + "\"]";
      } else {
         return val.toString();
      }
   }
}
