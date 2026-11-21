package com.legacy.structure_gel.api.data_handler.parsing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.ImmutableList;
import com.legacy.structure_gel.api.data_handler.DataHandlerType;
import com.legacy.structure_gel.api.registry.StructureGelRegistries;
import com.legacy.structure_gel.core.client.ClientUtil;
import com.legacy.structure_gel.core.network.SGPacketHandler;
import com.legacy.structure_gel.core.network.c_to_s.RequestRegistryKeysPacket;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.commands.arguments.item.ItemParser;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Stores information on how to parse string data as other objects to create a
 * {@link DataMap}. Data added to the parser will attempt to return the result
 * of parsing a string, or the defaultVal if the result is null or threw an
 * exception
 * 
 * @author Silver_David
 *
 */
public class DataParser
{
	private final List<Parser<?>> parsers;
	public final ResourceLocation dataHandlerType;

	private DataParser(ResourceLocation dataHandlerType, List<Parser<?>> parsers)
	{
		this.parsers = ImmutableList.copyOf(parsers);
		this.dataHandlerType = dataHandlerType;
	}

	/**
	 * @return A {@link DataParser.Factory} used to make a data parser for data
	 *         handlers
	 */
	public static DataParser.Factory of(Consumer<DataParser.Builder> buildFunc)
	{
		return (t, r) ->
		{
			DataParser.Builder builder = new DataParser.Builder(t, r);
			buildFunc.accept(builder);
			return builder.build();
		};
	}

	public static interface Factory
	{
		DataParser build(DataHandlerType<?> type, HolderLookup.Provider registryAccess);
	}

	public static final class Builder
	{
		private final List<Parser<?>> parsers = new ArrayList<>();
		private final ResourceLocation dataHandlerType;
		private final HolderLookup.Provider registryAccess;

		public Builder(DataHandlerType<?> dataHandlerType, HolderLookup.Provider registryAccess)
		{
			ResourceLocation key = StructureGelRegistries.DATA_HANDLER_TYPE.getKey(dataHandlerType);
			if (key == null)
				throw new IllegalStateException("The data handler type passed was not registered " + dataHandlerType);

			this.dataHandlerType = key;
			this.registryAccess = registryAccess;
		}

		public DataParser build()
		{
			return new DataParser(this.dataHandlerType, this.parsers);
		}

		public NumberParser<Byte> addByte(String key, byte defaultVal, byte min, byte max)
		{
			return this.addNumber(key, Byte::valueOf, defaultVal, Byte.class, "byte", min, max, (val, range) -> val >= range.getLeft() && val <= range.getRight());
		}

		public NumberParser<Short> addShort(String key, short defaultVal, short min, short max)
		{
			return this.addNumber(key, Short::valueOf, defaultVal, Short.class, "short", min, max, (val, range) -> val >= range.getLeft() && val <= range.getRight());
		}

		public NumberParser<Integer> add(String key, int defaultVal, int min, int max)
		{
			return this.addNumber(key, Integer::valueOf, defaultVal, Integer.class, "int", min, max, (val, range) -> val >= range.getLeft() && val <= range.getRight());
		}

		public NumberParser<Float> add(String key, float defaultVal, float min, float max)
		{
			return this.addNumber(key, Float::valueOf, defaultVal, Float.class, "float", min, max, (val, range) -> val >= range.getLeft() && val <= range.getRight());
		}

		public NumberParser<Double> add(String key, double defaultVal, double min, double max)
		{
			return this.addNumber(key, Double::valueOf, defaultVal, Double.class, "double", min, max, (val, range) -> val >= range.getLeft() && val <= range.getRight());
		}

		public Parser<Boolean> add(String key, boolean defaultVal)
		{
			return this.add(key, (s, l) -> Boolean.valueOf(s), defaultVal, Boolean.class, "boolean");
		}

		public Parser<String> add(String key, @Nullable String defaultVal)
		{
			return this.add(key, (s, l) -> s, defaultVal, String.class, "String");
		}

		public Parser<ResourceLocation> add(String key, @Nullable ResourceLocation defaultVal)
		{
			return this.add(key, (s, l) -> DataParser.parseResourceLocation(s), defaultVal, ResourceLocation.class, "ResourceLocation");
		}

		public Parser<Direction> add(String key, @Nullable Direction defaultVal)
		{
			return this.addEnum(key, Direction.class, defaultVal);
		}

		public Parser<Direction.Axis> add(String key, @Nullable Direction.Axis defaultVal)
		{
			return this.addEnum(key, Direction.Axis.class, defaultVal);
		}

		public <T extends Enum<T> & StringRepresentable> Parser<T> addEnum(String key, Class<T> clazz, @Nullable T defaultVal)
		{
			return this.add(key, (s, l) ->
			{
				for (T v : clazz.getEnumConstants())
					if (s.equals(v.getSerializedName()))
						return v;
				return null;
			}, defaultVal, clazz, clazz.getSimpleName()).setDefaultValName(defaultVal != null ? defaultVal.getSerializedName() : null);
		}

		public Parser<BlockState> add(String key, @Nullable BlockState defaultVal)
		{
			String defaultValName = null;
			if (defaultVal != null)
				defaultValName = BlockStateParser.serialize(defaultVal);
			return this.add(key, DataParser::parseBlockState, defaultVal, BlockState.class, "BlockState").setDefaultValName(defaultValName).setExample("minecraft:chest[facing=east]");
		}

		public Parser<ItemStack> add(String key, @Nullable ItemStack defaultVal)
		{
			ItemStack defaultClone = null;
			String defaultValName = null;
			if (defaultVal != null)
			{
				defaultClone = defaultVal.copy();
				ResourceLocation id = defaultClone.getItemHolder().unwrapKey().map(ResourceKey::location).orElse(null);
				if (id != null)
				{
					// ID
					defaultValName = id.toString();

					// Components
					// Unused because this makes the tooltip string REALLY long and likely doesn't provide much info
					/*DataComponentMap comps = defaultClone.getComponents();
					List<String> compStrings = new ArrayList<>();
					for (DataComponentType comp : comps.keySet())
					{
						Optional<?> result = comp.codec().encodeStart(NbtOps.INSTANCE, comps.get(comp)).result();
						if (result.isPresent())
						{
							compStrings.add(BuiltInRegistries.DATA_COMPONENT_TYPE.getResourceKey(comp).get().location() + "=" + result.get());
						}
					}
					
					if (!compStrings.isEmpty())
					{
						StringBuilder compBuilder = new StringBuilder("[");
						Iterator<String> compIter = compStrings.iterator();
						while (compIter.hasNext())
						{
							String compStr = compIter.next();
							compBuilder.append(compStr);
							if (compIter.hasNext())
								compBuilder.append(",");
						}
						compBuilder.append("]");
					
						//defaultValName = defaultValName + compBuilder.toString();
					}*/
					
					// Count
					int count = defaultClone.getCount();
					count = 2;
					if (count > 1)
					{
						defaultValName = "x" + count + " " + defaultValName;
					}
				}
			}
			return this.add(key, DataParser::parseItemStack, defaultClone, ItemStack.class, "ItemStack").setDefaultValName(defaultValName, (a, b) -> a == b || a.getCount() == b.getCount() && ItemStack.isSameItem(a, b)).setExample("x64 minecraft:diamond[components]");
		}

		public Parser<CompoundTag> add(String key, @Nullable CompoundTag defaultVal)
		{
			return this.add(key, (s, l) -> DataParser.parseTag(s), defaultVal, CompoundTag.class, "CompoundTag").setDefaultValName(defaultVal != null ? defaultVal.getAsString() : null).setExample("{key:\"value\", id:0, list:[0, 1, 2]}");
		}

		@SuppressWarnings("unchecked")
		public <T> ResourceKeyParser<ResourceKey<T>> add(String key, @Nonnull ResourceKey<T> defaultVal) throws NullPointerException
		{
			if (defaultVal == null)
				throw new NullPointerException("defaultVal cannot be null for " + this.dataHandlerType + "." + key + " use add(String, ResourceKey<T>, ResourceKey<Registry<T>>)");
			ResourceKey<Registry<T>> registry = ResourceKey.createRegistryKey(defaultVal.registry());
			ResourceKeyParser<T> parser = new ResourceKeyParser<>(this.dataHandlerType, key, (s, l) -> parseResourceKey(registry, s), defaultVal, registry);
			parser.setDefaultValName(defaultVal != null ? defaultVal.location().toString() : null);
			this.add(parser);
			return (ResourceKeyParser<ResourceKey<T>>) parser;
		}

		@SuppressWarnings({ "unchecked", "rawtypes" })
		public <T> ResourceKeyParser<ResourceKey<T>> add(String key, @Nonnull ResourceKey<Registry<T>> registryKey, @Nullable T defaultVal) throws NullPointerException
		{
			Registry registry = BuiltInRegistries.REGISTRY.getValue((ResourceKey) registryKey);
			if (registry != null)
			{
				Optional<ResourceKey> resourceKey = registry.getResourceKey(defaultVal);
				if (resourceKey.isPresent())
				{
					return (ResourceKeyParser<ResourceKey<T>>) this.add(key, resourceKey.get());
				}
				throw new NullPointerException("Could not find the defaultVal in the registry: Registry = " + registryKey.location());
			}
			throw new NullPointerException("Could not find the registry from the registryKey passed: registryKey = " + registryKey);
		}

		/**
		 * Creates and adds a {@link Parser} to this instance
		 * 
		 * @param <T>
		 * @param key
		 *            The name to store values under
		 * @param parser
		 *            A function to convert a String to T
		 * @param defaultVal
		 *            The default value if the parser fails or returns null
		 * @param type
		 *            The class being parsed
		 * @param typeName
		 *            The name of the class being parsed
		 * @param validator
		 *            Validates the value being parsed
		 * @return The created {@link Parser}
		 */
		public <T> Parser<T> add(String key, ParseFunc<T> parser, @Nullable T defaultVal, Class<T> type, String typeName, ParseValidator validator)
		{
			Parser<T> dataParser = new Parser<>(this.dataHandlerType, key, parser, defaultVal, type, typeName, validator);
			this.add(dataParser);
			return dataParser;
		}

		/**
		 * Creates and adds a {@link Parser} to this instance
		 * 
		 * @param <T>
		 * @param key
		 *            The name to store values under
		 * @param parser
		 *            A function to convert a String to T
		 * @param defaultVal
		 *            The default value if the parser fails or returns null
		 * @param type
		 *            The class being parsed
		 * @param typeName
		 *            The name of the class being parsed
		 * @return The created {@link Parser}
		 */
		public <T> Parser<T> add(String key, ParseFunc<T> parser, @Nullable T defaultVal, Class<T> type, String typeName)
		{
			return this.add(key, parser, defaultVal, type, typeName, (s, l) -> Objects.nonNull(parser.parse(s, l)));
		}

		/**
		 * Creates and adds a {@link NumberParser} to this instance
		 * 
		 * @param <T>
		 * @param key
		 *            The name to store values under
		 * @param parser
		 *            A function to convert a String to T
		 * @param defaultVal
		 *            The default value if the parser fails or returns null
		 * @param type
		 *            The class being parsed
		 * @param typeName
		 *            The name of the class being parsed
		 * @param minVal
		 *            The minimum value allowed (inclusive)
		 * @param maxVal
		 *            The maximum value allowed (inclusive)
		 * @param rangeTest
		 *            A function to check if the parsed value is within the min and max
		 *            value
		 * @return the created {@link NumberParser}
		 */
		public <T extends Number> NumberParser<T> addNumber(String key, Function<String, T> parser, @Nullable T defaultVal, Class<T> type, String typeName, T minVal, T maxVal, BiPredicate<T, Pair<T, T>> rangeTest)
		{
			NumberParser<T> numParser = new NumberParser<>(this.dataHandlerType, key, parser, defaultVal, type, typeName, minVal, maxVal, rangeTest);
			this.add(numParser);
			return numParser;
		}

		/**
		 * Adds the parser to this instance
		 * 
		 * @param parser
		 *            The parser to add
		 * @throws IllegalArgumentException
		 *             If a parser already exists under the passed name
		 * @throws IllegalStateException
		 *             If this instance has already been finalized
		 */
		public void add(Parser<?> parser) throws IllegalStateException, IllegalArgumentException
		{
			for (var p : this.parsers)
				if (p.name.equals(parser.name))
					throw new IllegalArgumentException("Attempted to add a parser under an existing name for " + this.dataHandlerType + ". " + parser);
			this.parsers.add(parser);
		}
	}

	/**
	 * Converts the passed data into a {@link DataMap} using the stored parsers
	 * 
	 * @param input
	 * @return The {@link DataMap}
	 */
	public DataMap parse(Map<String, String> input, LevelReader level)
	{
		Map<String, Object> data = new HashMap<>();
		for (var parser : this.parsers)
		{
			String key = parser.key;
			String rawVal = input.get(key);
			if (rawVal != null)
			{
				Object parsedVal = parser.parse(rawVal, level);
				data.put(key, parsedVal);
			}
			else
			{
				data.put(key, parser.defaultVal);
			}
		}

		return new DataMap(data);
	}

	/**
	 * @return An unmodifiable view of the parsers
	 */
	public List<Parser<?>> getParsers()
	{
		return Collections.unmodifiableList(this.parsers);
	}

	@Nullable
	private static ResourceLocation parseResourceLocation(String key)
	{
		return ResourceLocation.tryParse(key);
	}

	@Nullable
	private static BlockState parseBlockState(String key, LevelReader level)
	{
		try
		{
			BlockState state = BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK, new StringReader(key), false).blockState();
			return state.getBlock().isEnabled(level.enabledFeatures()) ? state : null;
		}
		catch (CommandSyntaxException e)
		{
			// Silently fail
			return null;
		}
	}

	@Nullable
	private static ItemStack parseItemStack(String key, LevelReader level)
	{
		try
		{
			int count = 1;
			String itemString;
			int firstSpace = key.indexOf(' ');
			if (firstSpace > 0 && key.startsWith("x"))
			{
				String countText = key.substring(1, firstSpace).trim();
				try
				{
					count = Integer.parseInt(countText);
					itemString = key.substring(firstSpace).trim();
				}
				catch (NumberFormatException e)
				{
					itemString = key.trim();
				}
			}
			else
			{
				itemString = key.trim();
			}
			ItemParser.ItemResult result = new ItemParser(level.registryAccess()).parse(new StringReader(itemString));
			ItemInput item = new ItemInput(result.item(), result.components());
			ItemStack itemStack = item.createItemStack(count, false);
			return itemStack.isItemEnabled(level.enabledFeatures()) ? itemStack : null;
		}
		catch (CommandSyntaxException e)
		{
			// Silently fail
			return null;
		}
	}

	@Nullable
	private static CompoundTag parseTag(String key)
	{
		try
		{
			return new TagParser(new StringReader(key)).readStruct();
		}
		catch (CommandSyntaxException e)
		{
			// Silently fail
			return null;
		}
	}

	@Nullable
	public static <T> ResourceKey<T> parseResourceKey(ResourceKey<Registry<T>> registry, String key)
	{
		ResourceLocation location = ResourceLocation.tryParse(key);
		if (location != null)
		{
			return ResourceKey.create(registry, location);
		}
		return null;
	}

	/**
	 * Handles converting string data to an object
	 * 
	 * @author Silver_David
	 *
	 * @param <T>
	 */
	public static class Parser<T>
	{
		public final String name;
		public final String key;
		public final ParseFunc<T> parseFunc;
		@Nullable
		public final T defaultVal;
		public final Class<T> type;
		public final String typeName;
		public final ParseValidator validator;
		@Nullable
		private String example;
		@Nullable
		private DefaultValue defaultValName;
		private Supplier<Collection<T>> suggestions = () -> Collections.emptyList();
		private Function<T, String> toStringFunc = Object::toString;

		public Parser(ResourceLocation dataHandlerType, String key, ParseFunc<T> parseFunc, @Nullable T defaultVal, Class<T> type, String typeName, ParseValidator validator)
		{
			this.name = dataHandlerType.toString() + "." + key;
			this.key = key;
			this.parseFunc = parseFunc;
			this.defaultVal = defaultVal;
			this.type = type;
			this.typeName = typeName;
			this.validator = validator;
		}

		public <S> Parser<T> setSuggestions(Supplier<Collection<T>> suggestions, Function<T, String> toStringFunc)
		{
			this.suggestions = suggestions;
			this.toStringFunc = toStringFunc;
			return this;
		}

		public Collection<T> getSuggestions()
		{
			return this.suggestions.get();
		}

		public Function<T, String> getSuggestionToString()
		{
			return this.toStringFunc;
		}

		/**
		 * Sets the example to display if one is needed
		 * 
		 * @param example
		 * @return This instance
		 */
		public Parser<T> setExample(@Nullable String example)
		{
			this.example = example;
			return this;
		}

		/**
		 * Sets the default value name that displays in the DataHandlerBlockEntity gui
		 * 
		 * @param defaultValName
		 *            The name to display as the default value. Must be able to be
		 *            parsed
		 * @return This instance
		 * @throws IllegalArgumentException
		 *             If defaultValName does not pass the validator or is not equal to
		 *             the defaultVal
		 */
		public Parser<T> setDefaultValName(@Nullable String defaultValName) throws IllegalArgumentException
		{
			return this.setDefaultValName(defaultValName, Objects::equals);
		}

		/**
		 * Sets the default value name that displays in the DataHandlerBlockEntity gui
		 * 
		 * @param defaultValName
		 *            The name to display as the default value. Must be able to be
		 *            parsed
		 * @param equalsFunc
		 *            Compares the parsed defaultValName and the stored defaultVal
		 * @return This instance
		 * @throws IllegalArgumentException
		 *             If defaultValName does not pass the validator or is not equal to
		 *             the defaultVal
		 */
		public Parser<T> setDefaultValName(@Nullable String defaultValName, BiPredicate<T, T> equalsFunc) throws IllegalArgumentException
		{
			if (defaultValName == null && this.defaultVal == null)
			{
				this.defaultValName = (l) -> "null";
			}
			else
			{
				this.defaultValName = (l) ->
				{
					// Check if passes validator
					if (!this.validator.test(defaultValName, l))
						throw new IllegalArgumentException("The defaultValName for " + this.name + " was not valid: " + defaultValName);

					// Check if equal to defaultVal
					T parsedVal = this.parseFunc.parse(defaultValName, l);
					if ((this.defaultVal == null && this.defaultVal != parsedVal) || !equalsFunc.test(this.defaultVal, parsedVal))
						throw new IllegalArgumentException("The defaultValName for " + this.name + " did not equal the defaultVal. defaultVal: " + this.defaultVal + ", parsedVal: " + parsedVal + " from: " + defaultValName);

					return defaultValName;
				};
			}
			return this;
		}

		@Nullable
		public String getExample()
		{
			return this.example;
		}

		public String getDefaultValName(LevelReader level)
		{
			if (this.defaultValName == null)
			{
				this.defaultValName = (l) -> this.defaultVal == null ? "empty" : this.toStringFunc.apply(this.defaultVal);
			}
			return this.defaultValName.get(level);
		}

		/**
		 * @param value
		 *            The string to parse
		 * @return The result of {@link #parseFunc} or {@link #defaultVal} if parseFunc
		 *         returned null or threw an exception
		 */
		@Nullable
		public T parse(String value, LevelReader level)
		{
			try
			{
				T ret = this.parseFunc.parse(value, level);
				return ret != null ? ret : this.defaultVal;
			}
			catch (Exception e)
			{
				return this.defaultVal;
			}
		}

		public String getTranlsationKey(String labelType)
		{
			return "gui.structure_gel.data_handler." + this.name + "." + labelType;
		}

		@Override
		public String toString()
		{
			return "DataParser[key=" + this.key + ", type=" + this.typeName + "]";
		}
	}

	/**
	 * A version of {@link Parser} specifically for handling numbers
	 * 
	 * @author Silver_David
	 *
	 * @param <T>
	 */
	public static class NumberParser<T extends Number> extends Parser<T>
	{
		private T minVal;
		private T maxVal;

		public NumberParser(ResourceLocation dataHandlerType, String key, Function<String, T> parseFunc, T defaultVal, Class<T> type, String typeName, T minVal, T maxVal, BiPredicate<T, Pair<T, T>> rangeTest)
		{
			super(dataHandlerType, key, (s, l) -> parseFunc.apply(s), defaultVal, type, typeName, validator((s, l) -> parseFunc.apply(s), minVal, maxVal, rangeTest));
			this.minVal = minVal;
			this.maxVal = maxVal;
		}

		public T getMin()
		{
			return this.minVal;
		}

		public T getMax()
		{
			return this.maxVal;
		}

		private static <T extends Number> ParseValidator validator(ParseFunc<T> func, T minVal, T maxVal, BiPredicate<T, Pair<T, T>> rangeTest)
		{
			return (s, l) ->
			{
				try
				{
					T val = func.parse(s, l);
					return rangeTest.test(val, Pair.of(minVal, maxVal));
				}
				catch (Exception e)
				{
					return false;
				}
			};
		}
	}

	public static class ResourceKeyParser<T> extends Parser<ResourceKey<T>>
	{
		private ResourceKey<Registry<T>> registry;

		@SuppressWarnings("unchecked")
		public ResourceKeyParser(ResourceLocation dataHandlerType, String key, ParseFunc<ResourceKey<T>> parseFunc, @Nullable ResourceKey<T> defaultVal, ResourceKey<Registry<T>> registry)
		{
			super(dataHandlerType, key, parseFunc, defaultVal, ((Class<ResourceKey<T>>) (Object) ResourceKey.class), "Registry: " + registry.location(), (s, l) -> Objects.nonNull(parseFunc.parse(s, l)));
			this.registry = registry;
		}

		public ResourceKey<Registry<T>> getRegistry()
		{
			return this.registry;
		}

		@SuppressWarnings({ "unchecked", "rawtypes" })
		@Override
		public Collection<ResourceKey<T>> getSuggestions()
		{
			var suggestions = ClientUtil.REGISTRY_KEYS.get(this.registry);
			if (suggestions == null)
			{
				SGPacketHandler.sendToServer(new RequestRegistryKeysPacket(this.registry));
			}

			return suggestions == null ? super.getSuggestions() : (Collection) suggestions;
		}
	}

	public static interface ParseFunc<T>
	{
		@Nullable
		T parse(String string, LevelReader level);
	}

	public static interface ParseValidator
	{
		boolean test(String string, LevelReader level);
	}

	public static interface DefaultValue
	{
		String get(LevelReader level);
	}
}
