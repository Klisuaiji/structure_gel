package com.legacy.structure_gel.api.data_handler;

import com.legacy.structure_gel.api.data_handler.handlers.DataHandler;
import com.legacy.structure_gel.api.data_handler.parsing.DataMap;
import com.legacy.structure_gel.api.data_handler.parsing.DataParser;
import java.util.function.Function;
import java.util.function.Supplier;

public class DataHandlerType<T extends DataHandler<T>> {
   private final Function<DataMap, T> factory;
   private final Supplier<DataParser> dataParser;

   public DataHandlerType(Function<DataMap, T> factory, Supplier<DataParser> dataParser) {
      this.factory = factory;
      this.dataParser = dataParser;
   }

   public T create(DataMap dataMap) {
      return this.factory.apply(dataMap);
   }

   public DataParser getDataParser() {
      return this.dataParser.get();
   }
}
