package com.legacy.structure_gel.api.data.providers;

import java.util.concurrent.CompletableFuture;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;

public record NestedDataProvider<D extends DataProvider>(D provider, String namePrefix) implements DataProvider {
   public static <D extends DataProvider> NestedDataProvider<D> of(D provider, String namePrefix) {
      return new NestedDataProvider<>(provider, namePrefix);
   }

   public CompletableFuture<?> run(CachedOutput cachedOutput) {
      return this.provider.run(cachedOutput);
   }

   public String getName() {
      return this.namePrefix + "/" + this.provider.getName();
   }
}
