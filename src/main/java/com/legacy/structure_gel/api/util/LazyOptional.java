package com.legacy.structure_gel.api.util;

import java.util.function.Supplier;
import net.neoforged.neoforge.common.util.Lazy;
import org.jetbrains.annotations.Nullable;

public class LazyOptional<T> implements Lazy<T> {
   protected final Lazy<T> lazy;
   protected boolean isPresent = false;

   public static <T> LazyOptional<T> of(Supplier<T> supplier) {
      return new LazyOptional<>(supplier);
   }

   public static <T> LazyOptional<T> concurrent(Supplier<T> supplier) {
      return new LazyOptional.Concurrent<>(supplier);
   }

   protected LazyOptional(Supplier<T> supplier) {
      this.lazy = Lazy.of(supplier);
   }

   @Nullable
   public T get() {
      if (!this.isPresent) {
         this.isPresent = true;
      }

      return (T)this.lazy.get();
   }

   public <E extends T> T orElseGet(Supplier<E> orElse) {
      return (T)(this.isPresent ? this.get() : orElse.get());
   }

   public T getOrThrow(String message, Object... args) throws IllegalStateException {
      if (this.isPresent) {
         return this.get();
      } else {
         throw new IllegalStateException(String.format(message, args));
      }
   }

   public boolean isPresent() {
      return this.isPresent;
   }

   public static class Concurrent<T> extends LazyOptional<T> {
      private volatile Object lock = new Object();

      protected Concurrent(Supplier<T> supplier) {
         super(supplier);
      }

      @Nullable
      @Override
      public final T get() {
         if (!this.isPresent) {
            Object localLock = this.lock;
            synchronized (localLock) {
               this.lock = null;
               return super.get();
            }
         } else {
            return super.get();
         }
      }
   }
}
