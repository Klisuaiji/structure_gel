package com.legacy.structure_gel.api.data.providers;

import java.util.concurrent.CompletableFuture;

import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;

/**
 * Used to store a data provider with a different name. All methods are passed
 * through to the provider contained. Good for cases like pack.mcmeta where we
 * have 2 of the same generator.
 * 
 * @author Silver_David
 *
 */
public record NestedDataProvider<D extends DataProvider>(D provider, String namePrefix) implements DataProvider
{

	public static <D extends DataProvider> NestedDataProvider<D> of(D provider, String namePrefix)
	{
		return new NestedDataProvider<>(provider, namePrefix);
	}
	
	@Override
	public CompletableFuture<?> run(CachedOutput cachedOutput)
	{
		return this.provider.run(cachedOutput);
	}

	@Override
	public String getName()
	{
		return this.namePrefix + "/" + this.provider.getName();
	}
}
