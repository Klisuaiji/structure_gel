package com.legacy.structure_gel.api.data_handler.handlers;

import com.legacy.structure_gel.api.data_handler.parsing.DataMap;

public abstract class ProbabilityDataHandler<T extends ProbabilityDataHandler<T>> extends DataHandler<T> {
   private final float probability;

   public ProbabilityDataHandler(DataMap data, String probabilityFieldName) {
      super(data);
      this.probability = data.getFloat(probabilityFieldName);
   }

   @Override
   protected float getProbability() {
      return this.probability;
   }
}
