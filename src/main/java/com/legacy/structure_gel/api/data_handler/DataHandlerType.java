package com.legacy.structure_gel.api.data_handler;

import java.util.function.Function;

import com.legacy.structure_gel.api.data_handler.handlers.DataHandler;
import com.legacy.structure_gel.api.data_handler.parsing.DataMap;
import com.legacy.structure_gel.api.data_handler.parsing.DataParser;

import net.minecraft.core.HolderLookup;

/**
 * Internally registered object that creates a {@link DataHandler}
 * 
 * @author Silver_David
 *
 * @param <T>
 */
public class DataHandlerType<T extends DataHandler<T>>
{
	private final Function<DataMap, T> factory;
	private final DataParser.Factory dataParserFactory;

	public DataHandlerType(Function<DataMap, T> factory, DataParser.Factory dataParserFactory)
	{
		this.factory = factory;
		this.dataParserFactory = dataParserFactory;
	}

	public T create(DataMap dataMap)
	{
		return this.factory.apply(dataMap);
	}

	public DataParser getDataParser(HolderLookup.Provider registryAccess)
	{
		return this.dataParserFactory.build(this, registryAccess);
	}
}
