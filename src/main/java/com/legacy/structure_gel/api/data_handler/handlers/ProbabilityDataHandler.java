package com.legacy.structure_gel.api.data_handler.handlers;

import com.legacy.structure_gel.api.data_handler.parsing.DataMap;

/**
 * A version of {@link DataHandler} with builtin probability handling.
 * 
 * @author Silver_David
 *
 * @param <T>
 */
public abstract class ProbabilityDataHandler<T extends ProbabilityDataHandler<T>> extends DataHandler<T>
{
	private final float probability;

	/**
	 * @param data
	 * @param probabilityFieldName
	 *            The name of the probability field in data
	 */
	public ProbabilityDataHandler(DataMap data, String probabilityFieldName)
	{
		super(data);
		this.probability = data.getFloat(probabilityFieldName);
	}

	@Override
	protected float getProbability()
	{
		return this.probability;
	}
}
