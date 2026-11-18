package com.legacy.structure_gel.api.data.tags;

import com.legacy.structure_gel.api.registry.registrar.Registrar;
import com.legacy.structure_gel.core.registry.SGRegistry;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.HolderSet.Named;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.HolderSetCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.neoforge.registries.holdersets.CompositeHolderSet;
import net.neoforged.neoforge.registries.holdersets.HolderSetType;
import net.neoforged.neoforge.registries.holdersets.ICustomHolderSet;
import net.neoforged.neoforge.registries.holdersets.OrHolderSet;

public class FilterHolderSet<T> implements ICustomHolderSet<T> {
   private final HolderSet<T> whitelist;
   private final HolderSet<T> blacklist;
   @Nullable
   private Set<Holder<T>> set;
   @Nullable
   private List<Holder<T>> list;

   public FilterHolderSet(HolderSet<T> whitelist, HolderSet<T> blacklist) {
      this.whitelist = whitelist;
      this.blacklist = blacklist;
      this.whitelist.addInvalidationListener(this::invalidate);
      this.blacklist.addInvalidationListener(this::invalidate);
   }

   public static <T> FilterHolderSet.Builder<T> builder(ResourceKey<Registry<T>> registry) {
      return new FilterHolderSet.Builder<>();
   }

   public static FilterHolderSet.Builder<Biome> biomes() {
      return builder(Registries.BIOME);
   }

   public static <T> Codec<? extends ICustomHolderSet<T>> codec(ResourceKey<? extends Registry<T>> registryKey, Codec<Holder<T>> holderCodec, boolean forceList) {
      return RecordCodecBuilder.create(
         instance -> instance.group(
               HolderSetCodec.create(registryKey, holderCodec, forceList).fieldOf("whitelist").forGetter(f -> f.whitelist),
               HolderSetCodec.create(registryKey, holderCodec, forceList).fieldOf("blacklist").forGetter(f -> f.blacklist)
            )
            .apply(instance, FilterHolderSet::new)
      );
   }

   public Stream<Holder<T>> stream() {
      return this.whitelist.stream().filter(holder -> !this.blacklist.contains(holder));
   }

   public int size() {
      return this.getList().size();
   }

   public Either<TagKey<T>, List<Holder<T>>> unwrap() {
      return Either.right(this.getList());
   }

   public Optional<Holder<T>> getRandomElement(RandomSource rand) {
      List<Holder<T>> l = this.getList();
      int size = l.size();
      return size > 0 ? Optional.of(l.get(rand.nextInt(size))) : Optional.empty();
   }

   public Holder<T> get(int index) {
      return this.getList().get(index);
   }

   public boolean contains(Holder<T> holder) {
      return this.getSet().contains(holder);
   }

   public Iterator<Holder<T>> iterator() {
      return this.getList().iterator();
   }

   public HolderSetType type() {
      return SGRegistry.HolderSetTypes.FILTER.get();
   }

   public boolean canSerializeIn(HolderOwner<T> holderOwner) {
      return true;
   }

   public Optional<TagKey<T>> unwrapKey() {
      return Optional.empty();
   }

   private void invalidate() {
      this.set = null;
      this.list = null;
   }

   private List<Holder<T>> getList() {
      if (this.list == null) {
         this.list = List.copyOf(this.getSet());
      }

      return this.list;
   }

   private Set<Holder<T>> getSet() {
      if (this.set == null) {
         this.set = this.stream().collect(Collectors.toSet());
      }

      return this.set;
   }

   public List<String> toFilterStrings() {
      List<String> vals = this.valuesToStrings(this.whitelist);
      this.valuesToStrings(this.blacklist).stream().map(s -> "!" + s).forEach(vals::add);
      return vals;
   }

   private List<String> valuesToStrings(HolderSet<T> holderSet) {
      List<String> values = new ArrayList<>();
      if (holderSet instanceof Named<T> named) {
         values.add("#" + named.key().location().toString());
      } else if (holderSet instanceof CompositeHolderSet<T> composite) {
         composite.getComponents().forEach(hSet -> values.addAll(this.valuesToStrings(hSet)));
      } else {
         holderSet.stream()
            .<Optional>map(Holder::unwrapKey)
            .filter(Optional::isPresent)
            .map(o -> ((ResourceKey)o.get()).location().toString())
            .forEach(values::add);
      }

      return values;
   }

   @Override
   public String toString() {
      return this.getClass().getSimpleName() + " = " + this.toFilterStrings();
   }

   public static final class Builder<T> {
      private final LinkedHashSet<ResourceKey<T>> wVals = new LinkedHashSet<>();
      private final LinkedHashSet<TagKey<T>> wTags = new LinkedHashSet<>();
      private final LinkedHashSet<HolderSet<T>> wHolderSets = new LinkedHashSet<>();
      private final LinkedHashSet<ResourceKey<T>> bVals = new LinkedHashSet<>();
      private final LinkedHashSet<TagKey<T>> bTags = new LinkedHashSet<>();
      private final LinkedHashSet<HolderSet<T>> bHolderSets = new LinkedHashSet<>();

      private Builder() {
      }

      public final FilterHolderSet.Builder<T> values(boolean whitelist, Collection<ResourceKey<T>> names) {
         if (whitelist) {
            this.wVals.addAll(names);
         } else {
            this.bVals.addAll(names);
         }

         return this;
      }

      @SafeVarargs
      public final FilterHolderSet.Builder<T> values(boolean whitelist, ResourceKey<T>... keys) {
         return this.values(whitelist, List.of(keys));
      }

      @SafeVarargs
      public final FilterHolderSet.Builder<T> values(boolean whitelist, Registrar<T>... objects) {
         return this.values(whitelist, Arrays.stream(objects).map(Registrar::getKey).toList());
      }

      public final FilterHolderSet.Builder<T> tags(boolean whitelist, Collection<TagKey<T>> tags) {
         if (whitelist) {
            this.wTags.addAll(tags);
         } else {
            this.bTags.addAll(tags);
         }

         return this;
      }

      @SafeVarargs
      public final FilterHolderSet.Builder<T> tags(boolean whitelist, TagKey<T>... tags) {
         return this.tags(whitelist, List.of(tags));
      }

      public final FilterHolderSet.Builder<T> holderSets(boolean whitelist, Collection<HolderSet<T>> holderSets) {
         if (whitelist) {
            this.wHolderSets.addAll(holderSets);
         } else {
            this.bHolderSets.addAll(holderSets);
         }

         return this;
      }

      @SafeVarargs
      public final FilterHolderSet.Builder<T> holderSets(boolean whitelist, HolderSet<T>... holderSets) {
         return this.holderSets(whitelist, List.of(holderSets));
      }

      public FilterHolderSet<T> build(HolderGetter<T> registry) {
         return new FilterHolderSet<>(
            this.compileList(registry, this.wVals, this.wTags, this.wHolderSets), this.compileList(registry, this.bVals, this.bTags, this.bHolderSets)
         );
      }

      private HolderSet<T> compileList(
         HolderGetter<T> registry, Collection<ResourceKey<T>> keys, Collection<TagKey<T>> tags, Collection<HolderSet<T>> holderSets
      ) {
         List<HolderSet<T>> compiledSets = new ArrayList<>();
         if (!keys.isEmpty()) {
            compiledSets.add(HolderSet.direct(keys.stream().<Optional>map(registry::get).filter(Optional::isPresent).map(Optional::get).toList()));
         }

         if (!tags.isEmpty()) {
            compiledSets.addAll(tags.stream().<Optional>map(registry::get).filter(Optional::isPresent).map(n -> (HolderSet<T>)n.get()).toList());
         }

         if (!holderSets.isEmpty()) {
            compiledSets.addAll(holderSets);
         }

         return (HolderSet<T>)(compiledSets.isEmpty() ? HolderSet.direct(new Holder[0]) : new OrHolderSet(compiledSets));
      }

      public List<String> toFilterStrings() {
         List<String> vals = this.valuesToStrings(this.wVals, this.wTags, this.wHolderSets);
         this.valuesToStrings(this.bVals, this.bTags, this.bHolderSets).stream().map(s -> "!" + s).forEach(vals::add);
         return vals;
      }

      private List<String> valuesToStrings(Collection<ResourceKey<T>> keys, Collection<TagKey<T>> tags, Collection<HolderSet<T>> holderSets) {
         List<String> values = new ArrayList<>();

         for (ResourceKey<T> key : keys) {
            values.add(key.location().toString());
         }

         for (TagKey<T> tag : tags) {
            values.add("#" + tag.location().toString());
         }

         for (HolderSet<T> set : holderSets) {
            set.stream()
               .<Optional>map(Holder::unwrapKey)
               .filter(Optional::isPresent)
               .map(o -> ((ResourceKey)o.get()).location().toString())
               .forEach(values::add);
         }

         return values;
      }

      @Override
      public String toString() {
         return this.getClass().getSimpleName() + " = " + this.toFilterStrings();
      }
   }
}
