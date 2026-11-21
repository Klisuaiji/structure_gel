package com.legacy.structure_gel.core.data_handler.handlers;

import com.legacy.structure_gel.api.data_handler.handlers.DataHandler;
import com.legacy.structure_gel.api.data_handler.parsing.DataMap;
import com.legacy.structure_gel.api.data_handler.parsing.DataParser;

/**
 * This handler does nothing and is used by default if no handler was set. It's
 * main purpose is to allow for data handlers to simply mark a block for post
 * processing and do nothing else.
 */
public class EmptyDataHandler extends DataHandler<EmptyDataHandler>
{
	public static final DataParser.Factory PARSER = DataParser.of(builder ->
	{
	});

	public EmptyDataHandler(DataMap data)
	{
		super(data);
	}

	@Override
	protected void handle(Context context)
	{
	}
}
