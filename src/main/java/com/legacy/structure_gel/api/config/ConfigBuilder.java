package com.legacy.structure_gel.api.config;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Stores methods to create "pretty" configs.
 * 
 * @author Silver_David
 *
 */
public final class ConfigBuilder
{
	public static <T> ModConfigSpec.ConfigValue<T> makeConfig(ModConfigSpec.Builder builder, String name, String comment, T defaultVal)
	{
		return builder.comment(comment(comment, defaultVal)).define(name, defaultVal);
	}

	public static <T> ModConfigSpec.ConfigValue<T> makeConfig(ModConfigSpec.Builder builder, String name, String comment, Object example, T defaultVal)
	{
		return builder.comment(comment(comment, defaultVal, example)).define(name, defaultVal);
	}

	public static ModConfigSpec.BooleanValue makeBoolean(ModConfigSpec.Builder builder, String name, String comment, boolean defaultVal)
	{
		return builder.comment(comment(comment, defaultVal)).define(name, defaultVal);
	}

	public static ModConfigSpec.IntValue makeInt(ModConfigSpec.Builder builder, String name, String comment, int defaultVal, int min, int max)
	{
		return builder.comment(comment(comment, defaultVal)).defineInRange(name, defaultVal, min, max);
	}

	public static ModConfigSpec.LongValue makeLong(ModConfigSpec.Builder builder, String name, String comment, long defaultVal, long min, long max)
	{
		return builder.comment(comment(comment, defaultVal)).defineInRange(name, defaultVal, min, max);
	}

	public static ModConfigSpec.DoubleValue makeDouble(ModConfigSpec.Builder builder, String name, String comment, double defaultVal, double min, double max)
	{
		return builder.comment(comment(comment, defaultVal)).defineInRange(name, defaultVal, min, max);
	}

	public static <T extends Enum<T>> ModConfigSpec.EnumValue<T> makeEnum(ModConfigSpec.Builder builder, String name, String comment, T defaultVal)
	{
		return builder.comment(comment(comment, defaultVal)).defineEnum(name, defaultVal);
	}

	public static <T> ModConfigSpec.ConfigValue<List<? extends T>> makeList(ModConfigSpec.Builder builder, String name, String comment, List<T> defaultVal, Predicate<Object> validator)
	{
		return builder.comment(comment(comment, defaultVal)).defineList(name, defaultVal, validator);
	}

	public static <T> ModConfigSpec.ConfigValue<List<? extends T>> makeEmptyList(ModConfigSpec.Builder builder, String name, String comment, List<T> defaultVal, Predicate<Object> validator)
	{
		return builder.comment(comment(comment, defaultVal)).defineListAllowEmpty(List.of(name), () -> defaultVal, validator);
	}

	public static <T> ModConfigSpec.ConfigValue<List<? extends T>> makeList(ModConfigSpec.Builder builder, String name, String comment, Object example, List<T> defaultVal, Predicate<Object> validator)
	{
		return builder.comment(comment(comment, defaultVal, example)).defineList(name, defaultVal, validator);
	}

	public static <T> ModConfigSpec.ConfigValue<List<? extends T>> makeEmptyList(ModConfigSpec.Builder builder, String name, String comment, Object example, List<T> defaultVal, Predicate<Object> validator)
	{
		return builder.comment(comment(comment, defaultVal, example)).defineListAllowEmpty(List.of(name), () -> defaultVal, validator);
	}

	public static String comment(String description, Object defaultVal, Object example)
	{
		return String.format((description.isEmpty() ? "%s" : "\n %s") + "\n Example: %s\n Default: %s", description, valString(example), valString(defaultVal));
	}

	public static String comment(String description, Object defaultVal)
	{
		return String.format((description.isEmpty() ? "%s" : "\n %s") + "\n Default: %s", description, valString(defaultVal));
	}

	private static String valString(Object val)
	{
		if (val instanceof List<?> list)
		{
			if (list.isEmpty())
				return "[]";
			return "[\"" + String.join("\", \"", list.stream().map(Object::toString).collect(Collectors.toList())) + "\"]";
		}
		return val.toString();
	}

}
