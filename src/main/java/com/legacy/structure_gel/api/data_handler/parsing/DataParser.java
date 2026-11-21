package com.legacy.structure_gel.api.data_handler.parsing;

import com.legacy.blue_skies.compat.*;
import com.legacy.structure_gel.api.data_handler.DataHandlerType;
import com.legacy.structure_gel.api.registry.StructureGelRegistries;
import com.legacy.structure_gel.core.client.ClientUtil;
import com.legacy.structure_gel.core.network.SGPacketHandler;
import com.legacy.structure_gel.core.network.c_to_s.RequestRegistryKeysPacket;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
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
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.commands.arguments.item.ItemParser;
import net.minecraft.commands.arguments.item.ItemParser.ItemResult;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.tuple.Pair;

public class DataParser {
   private boolean finalized = false;
   private final List<DataParser.Parser<?>> parsers = new ArrayList<>();
   public final ResourceLocation dataHandlerType;

   private DataParser(ResourceLocation dataHandlerType) {
      this.dataHandlerType = dataHandlerType;
   }

   public static DataParser of(DataHandlerType<?> dataHandlerType, Consumer<DataParser> parserHandler) {
      DataParser dataParser = new DataParser(StructureGelRegistries.DATA_HANDLER_TYPE.getKey(dataHandlerType));
      parserHandler.accept(dataParser);

      for (DataParser.Parser<?> parser : dataParser.parsers) {
         if (parser.defaultValName == null) {
            parser.setDefaultValName(parser.defaultVal != null ? parser.defaultVal.toString() : null);
         }
      }

      dataParser.finalized = true;
      return dataParser;
   }

   public static DataParser of(Supplier<? extends DataHandlerType<?>> dataHandlerType, Consumer<DataParser> parserHandler) {
      return of((DataHandlerType<?>)dataHandlerType.get(), parserHandler);
   }

   public DataParser.NumberParser<Byte> addByte(String key, byte defaultVal, byte min, byte max) {
      return this.addNumber(
         key, Byte::valueOf, defaultVal, Byte.class, "byte", min, max, (val, range) -> val >= (Byte)range.getLeft() && val <= (Byte)range.getRight()
      );
   }

   public DataParser.NumberParser<Short> addShort(String key, short defaultVal, short min, short max) {
      return this.addNumber(
         key, Short::valueOf, defaultVal, Short.class, "short", min, max, (val, range) -> val >= (Short)range.getLeft() && val <= (Short)range.getRight()
      );
   }

   public DataParser.NumberParser<Integer> add(String key, int defaultVal, int min, int max) {
      return this.addNumber(
         key, Integer::valueOf, defaultVal, Integer.class, "int", min, max, (val, range) -> val >= (Integer)range.getLeft() && val <= (Integer)range.getRight()
      );
   }

   public DataParser.NumberParser<Float> add(String key, float defaultVal, float min, float max) {
      return this.addNumber(
         key, Float::valueOf, defaultVal, Float.class, "float", min, max, (val, range) -> val >= (Float)range.getLeft() && val <= (Float)range.getRight()
      );
   }

   public DataParser.NumberParser<Double> add(String key, double defaultVal, double min, double max) {
      return this.addNumber(
         key, Double::valueOf, defaultVal, Double.class, "double", min, max, (val, range) -> val >= (Double)range.getLeft() && val <= (Double)range.getRight()
      );
   }

   public DataParser.Parser<Boolean> add(String key, boolean defaultVal) {
      return this.add(key, (s, l) -> Boolean.valueOf(s), defaultVal, Boolean.class, "boolean");
   }

   public DataParser.Parser<String> add(String key, String defaultVal) {
      return this.add(key, (s, l) -> s, defaultVal, String.class, "String");
   }

   public DataParser.Parser<ResourceLocation> add(String key, ResourceLocation defaultVal) {
      return this.add(key, (s, l) -> parseResourceLocation(s), defaultVal, ResourceLocation.class, "ResourceLocation");
   }

   public DataParser.Parser<Direction> add(String key, Direction defaultVal) {
      return this.addEnum(key, Direction.class, defaultVal);
   }

   public DataParser.Parser<Axis> add(String key, Axis defaultVal) {
      return this.addEnum(key, Axis.class, defaultVal);
   }

   public <T extends Enum<T> & StringRepresentable> DataParser.Parser<T> addEnum(String key, Class<T> clazz, T defaultVal) {
      return this.<T>add(key, (s, l) -> {
         for (T v : (Enum[])clazz.getEnumConstants()) {
            if (s.equals(v.getSerializedName())) {
               return v;
            }
         }

         return null;
      }, defaultVal, clazz, clazz.getSimpleName()).setDefaultValName(defaultVal != null ? defaultVal.getSerializedName() : null);
   }

   public DataParser.Parser<BlockState> add(String key, BlockState defaultVal) {
      String defaultValName = null;
      if (defaultVal != null) {
         defaultValName = BlockStateParser.serialize(defaultVal);
      }

      return this.add(key, DataParser::parseBlockState, defaultVal, BlockState.class, "BlockState")
         .setDefaultValName(defaultValName)
         .setExample("minecraft:chest[facing=east]");
   }

   public DataParser.Parser<ItemStack> add(String key, ItemStack defaultVal) {
      ItemStack defaultClone = null;
      String defaultValName = null;
      if (defaultVal != null) {
         defaultClone = defaultVal.isEmpty() ? new ItemStack((Item)null) : defaultVal.copy();
         ResourceLocation registryName = BuiltInRegistries.ITEM.getKey(defaultClone.getItem());
         if (registryName != null) {
            CompoundTag tag = defaultClone.save(new CompoundTag());
            tag.remove("id");
            defaultValName = registryName.toString() + (tag.isEmpty() ? "" : tag.getAsString());
         }

         LegacyNbt.getOrCreate(defaultClone).putByte("Count", (byte)defaultClone.getCount());
      }

      return this.add(key, DataParser::parseItemStack, defaultClone, ItemStack.class, "ItemStack")
         .setDefaultValName(defaultValName, ItemStack::matches)
         .setExample("minecraft:diamond{Count:1b}");
   }

   public DataParser.Parser<CompoundTag> add(String key, CompoundTag defaultVal) {
      return this.<CompoundTag>add(key, (s, l) -> parseTag(s), defaultVal, CompoundTag.class, "CompoundTag")
         .setDefaultValName(defaultVal != null ? defaultVal.getAsString() : null)
         .setExample("{key:\"value\", id:0, list:[0, 1, 2]}");
   }

   public <T> DataParser.ResourceKeyParser<ResourceKey<T>> add(String key, @Nonnull ResourceKey<T> defaultVal) throws NullPointerException {
      if (defaultVal == null) {
         throw new NullPointerException(
            "defaultVal cannot be null for " + this.dataHandlerType + "." + key + " use add(String, ResourceKey<T>, ResourceKey<Registry<T>>)"
         );
      }

      ResourceKey<Registry<T>> registry = ResourceKey.createRegistryKey(defaultVal.registry());
      DataParser.ResourceKeyParser<T> parser = new DataParser.ResourceKeyParser<>(
         this.dataHandlerType, key, (s, l) -> parseResourceKey(registry, s), defaultVal, registry
      );
      parser.setDefaultValName(defaultVal != null ? defaultVal.location().toString() : null);
      this.add(parser);
      return parser;
   }

   public <T> DataParser.ResourceKeyParser<ResourceKey<T>> add(String key, @Nonnull ResourceKey<Registry<T>> registryKey, T defaultVal) throws NullPointerException {
      Registry registry = (Registry)BuiltInRegistries.REGISTRY.get(registryKey);
      if (registry != null) {
         Optional<ResourceKey> resourceKey = registry.getResourceKey(defaultVal);
         if (resourceKey.isPresent()) {
            return this.add(key, resourceKey.get());
         } else {
            throw new NullPointerException("Could not find the defaultVal in the registry: Registry = " + registryKey.location());
         }
      } else {
         throw new NullPointerException("Could not find the registry from the registryKey passed: registryKey = " + registryKey);
      }
   }

   public <T> DataParser.Parser<T> add(
      String key, DataParser.ParseFunc<T> parser, T defaultVal, Class<T> type, String typeName, DataParser.ParseValidator validator
   ) {
      DataParser.Parser<T> dataParser = new DataParser.Parser<>(this.dataHandlerType, key, parser, defaultVal, type, typeName, validator);
      this.add(dataParser);
      return dataParser;
   }

   public <T> DataParser.Parser<T> add(String key, DataParser.ParseFunc<T> parser, T defaultVal, Class<T> type, String typeName) {
      return this.add(key, parser, defaultVal, type, typeName, (s, l) -> Objects.nonNull(parser.parse(s, l)));
   }

   public <T extends Number> DataParser.NumberParser<T> addNumber(
      String key, Function<String, T> parser, T defaultVal, Class<T> type, String typeName, T minVal, T maxVal, BiPredicate<T, Pair<T, T>> rangeTest
   ) {
      DataParser.NumberParser<T> numParser = new DataParser.NumberParser<>(
         this.dataHandlerType, key, parser, defaultVal, type, typeName, minVal, maxVal, rangeTest
      );
      this.add(numParser);
      return numParser;
   }

   public void add(DataParser.Parser<?> parser) throws IllegalStateException, IllegalArgumentException {
      if (!this.finalized) {
         for (DataParser.Parser<?> p : this.parsers) {
            if (p.name.equals(parser.name)) {
               throw new IllegalArgumentException("Attempted to add a parser under an existing name for " + this.dataHandlerType + ". " + parser);
            }
         }

         this.parsers.add(parser);
      } else {
         throw new IllegalStateException("Attempted to add a parser after the DataParser for " + this.dataHandlerType + " has already been created. " + parser);
      }
   }

   public DataMap parse(Map<String, String> input, LevelReader level) {
      Map<String, Object> data = new HashMap<>();

      for (DataParser.Parser<?> parser : this.parsers) {
         String key = parser.key;
         String rawVal = input.get(key);
         if (rawVal != null) {
            Object parsedVal = parser.parse(rawVal, level);
            data.put(key, parsedVal);
         } else {
            data.put(key, parser.defaultVal);
         }
      }

      return new DataMap(data);
   }

   public List<DataParser.Parser<?>> getParsers() {
      return Collections.unmodifiableList(this.parsers);
   }

   @Nullable
   private static ResourceLocation parseResourceLocation(String key) {
      return LegacyRL.isValid(key) ? ResourceLocation.parse(key) : null;
   }

   @Nullable
   private static BlockState parseBlockState(String key, LevelReader level) {
      try {
         BlockState state = BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK.asLookup(), new StringReader(key), false).blockState();
         return state.getBlock().isEnabled(level.enabledFeatures()) ? state : null;
      } catch (CommandSyntaxException e) {
         return null;
      }
   }

   @Nullable
   private static ItemStack parseItemStack(String key, LevelReader level) {
      try {
         ItemResult result = ItemParser.parseForItem(BuiltInRegistries.ITEM.asLookup(), new StringReader(key));
         ItemStack itemStack = new ItemStack((ItemLike)result.item().value());
         CompoundTag nbt = result.nbt();
         if (nbt != null) {
            itemStack.setTag(nbt);
            if (nbt.contains("Count", 1)) {
               itemStack.setCount(nbt.getByte("Count"));
            }
         }

         return itemStack.isItemEnabled(level.enabledFeatures()) ? itemStack : null;
      } catch (CommandSyntaxException e) {
         return null;
      }
   }

   @Nullable
   private static CompoundTag parseTag(String key) {
      try {
         return new TagParser(new StringReader(key)).readStruct();
      } catch (CommandSyntaxException e) {
         return null;
      }
   }

   @Nullable
   public static <T> ResourceKey<T> parseResourceKey(ResourceKey<Registry<T>> registry, String key) {
      return LegacyRL.isValid(key) ? ResourceKey.create(registry, ResourceLocation.parse(key)) : null;
   }

   public interface DefaultValue {
      String get(LevelReader var1);
   }

   public static class NumberParser<T extends Number> extends DataParser.Parser<T> {
      private T minVal;
      private T maxVal;

      public NumberParser(
         ResourceLocation dataHandlerType,
         String key,
         Function<String, T> parseFunc,
         T defaultVal,
         Class<T> type,
         String typeName,
         T minVal,
         T maxVal,
         BiPredicate<T, Pair<T, T>> rangeTest
      ) {
         super(
            dataHandlerType, key, (s, l) -> parseFunc.apply(s), defaultVal, type, typeName, validator((s, l) -> parseFunc.apply(s), minVal, maxVal, rangeTest)
         );
         this.minVal = minVal;
         this.maxVal = maxVal;
      }

      public T getMin() {
         return this.minVal;
      }

      public T getMax() {
         return this.maxVal;
      }

      private static <T extends Number> DataParser.ParseValidator validator(
         DataParser.ParseFunc<T> func, T minVal, T maxVal, BiPredicate<T, Pair<T, T>> rangeTest
      ) {
         return (s, l) -> {
            try {
               T val = func.parse(s, l);
               return rangeTest.test(val, Pair.of(minVal, maxVal));
            } catch (Exception e) {
               return false;
            }
         };
      }
   }

   public interface ParseFunc<T> {
      @Nullable
      T parse(String var1, LevelReader var2);
   }

   public interface ParseValidator {
      boolean test(String var1, LevelReader var2);
   }

   public static class Parser<T> {
      public final String name;
      public final String key;
      public final DataParser.ParseFunc<T> parseFunc;
      @Nullable
      public final T defaultVal;
      public final Class<T> type;
      public final String typeName;
      public final DataParser.ParseValidator validator;
      @Nullable
      private String example;
      @Nullable
      private DataParser.DefaultValue defaultValName;
      private Supplier<Collection<T>> suggestions = () -> Collections.emptyList();
      private Function<T, String> toStringFunc = Object::toString;

      public Parser(
         ResourceLocation dataHandlerType,
         String key,
         DataParser.ParseFunc<T> parseFunc,
         T defaultVal,
         Class<T> type,
         String typeName,
         DataParser.ParseValidator validator
      ) {
         this.name = dataHandlerType.toString() + "." + key;
         this.key = key;
         this.parseFunc = parseFunc;
         this.defaultVal = defaultVal;
         this.type = type;
         this.typeName = typeName;
         this.validator = validator;
      }

      public <S> DataParser.Parser<T> setSuggestions(Supplier<Collection<T>> suggestions, Function<T, String> toStringFunc) {
         this.suggestions = suggestions;
         this.toStringFunc = toStringFunc;
         return this;
      }

      public Collection<T> getSuggestions() {
         return this.suggestions.get();
      }

      public Function<T, String> getSuggestionToString() {
         return this.toStringFunc;
      }

      public DataParser.Parser<T> setExample(String example) {
         this.example = example;
         return this;
      }

      public DataParser.Parser<T> setDefaultValName(String defaultValName) throws IllegalArgumentException {
         return this.setDefaultValName(defaultValName, (a, b) -> a.equals(b));
      }

      public DataParser.Parser<T> setDefaultValName(String defaultValName, BiPredicate<T, T> equalsFunc) throws IllegalArgumentException {
         if (defaultValName == null && this.defaultVal == null) {
            this.defaultValName = l -> "null";
         } else {
            this.defaultValName = l -> {
               if (!this.validator.test(defaultValName, l)) {
                  throw new IllegalArgumentException("The defaultValName for " + this.name + " was not valid: " + defaultValName);
               } else {
                  T parsedVal = this.parseFunc.parse(defaultValName, l);
                  if ((this.defaultVal != null || this.defaultVal == parsedVal) && equalsFunc.test(this.defaultVal, parsedVal)) {
                     return defaultValName;
                  } else {
                     throw new IllegalArgumentException(
                        "The defaultValName for "
                           + this.name
                           + " did not equal the defaultVal. defaultVal: "
                           + this.defaultVal
                           + ", parsedVal: "
                           + parsedVal
                           + " from: "
                           + defaultValName
                     );
                  }
               }
            };
         }

         return this;
      }

      @Nullable
      public String getExample() {
         return this.example;
      }

      public String getDefaultValName(LevelReader level) {
         if (this.defaultValName == null) {
            throw new NullPointerException("The defaultValName for " + this.name + " was still null");
         } else {
            return this.defaultValName.get(level);
         }
      }

      @Nullable
      public T parse(String value, LevelReader level) {
         try {
            T ret = this.parseFunc.parse(value, level);
            return ret != null ? ret : this.defaultVal;
         } catch (Exception e) {
            return this.defaultVal;
         }
      }

      public String getTranlsationKey(String labelType) {
         return "gui.structure_gel.data_handler." + this.name + "." + labelType;
      }

      @Override
      public String toString() {
         return "DataParser[key=" + this.key + ", type=" + this.typeName + "]";
      }
   }

   public static class ResourceKeyParser<T> extends DataParser.Parser<ResourceKey<T>> {
      private ResourceKey<Registry<T>> registry;

      public ResourceKeyParser(
         ResourceLocation dataHandlerType,
         String key,
         DataParser.ParseFunc<ResourceKey<T>> parseFunc,
         ResourceKey<T> defaultVal,
         ResourceKey<Registry<T>> registry
      ) {
         super(
            dataHandlerType,
            key,
            parseFunc,
            defaultVal,
            ResourceKey.class,
            "Registry: " + registry.location(),
            (s, l) -> Objects.nonNull(parseFunc.parse(s, l))
         );
         this.registry = registry;
      }

      public ResourceKey<Registry<T>> getRegistry() {
         return this.registry;
      }

      @Override
      public Collection<ResourceKey<T>> getSuggestions() {
         List<ResourceKey<?>> suggestions = ClientUtil.REGISTRY_KEYS.get(this.registry);
         if (suggestions == null) {
            SGPacketHandler.sendToServer(new RequestRegistryKeysPacket(this.registry));
         }

         return suggestions == null ? super.getSuggestions() : suggestions;
      }
   }
}
