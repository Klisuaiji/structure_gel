package com.legacy.structure_gel.api.util;

import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import net.neoforged.neoforge.common.util.Lazy;

/**
 * Stores a {@link Lazy} and keeps track of if a value has been obtained from
 * it.
 * 
 * @author Silver_David
 *
 * @param <T>
 */
public class LazyOptional<T> implements Supplier<T>
{
	protected final Lazy<T> lazy;
	protected boolean isPresent = false;

	public static <T> LazyOptional<T> of(Supplier<T> supplier)
	{
		return new LazyOptional<T>(supplier);
	}

	public static <T> LazyOptional<T> concurrent(Supplier<T> supplier)
	{
		return new LazyOptional.Concurrent<>(supplier);
	}

	protected LazyOptional(Supplier<T> supplier)
	{
		this.lazy = Lazy.of(supplier);
	}

	@Override
	@Nullable
	public T get()
	{
		if (!this.isPresent)
			this.isPresent = true;
		return this.lazy.get();
	}

	public <E extends T> T orElseGet(Supplier<E> orElse)
	{
		return this.isPresent ? this.get() : orElse.get();
	}
	
	public T getOrThrow(String message, Object... args) throws IllegalStateException
	{
		if (this.isPresent)
			return this.get();
		throw new IllegalStateException(String.format(message, args));
	}

	public boolean isPresent()
	{
		return this.isPresent;
	}

	public static class Concurrent<T> extends LazyOptional<T>
	{
		private volatile Object lock = new Object();

		protected Concurrent(Supplier<T> supplier)
		{
			super(supplier);
		}

		@Override
		@Nullable
		public final T get()
		{
			if (!this.isPresent)
			{
				Object localLock = this.lock;
				synchronized (localLock)
				{
					this.lock = null;
					return super.get();
				}
			}
			return super.get();
		}
	}
}
