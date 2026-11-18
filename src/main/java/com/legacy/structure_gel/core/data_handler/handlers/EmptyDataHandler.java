package com.legacy.structure_gel.core.data_handler.handlers;

import com.legacy.structure_gel.api.data_handler.handlers.DataHandler;
import com.legacy.structure_gel.api.data_handler.parsing.DataMap;
import com.legacy.structure_gel.api.data_handler.parsing.DataParser;
import com.legacy.structure_gel.core.registry.SGRegistry;

public class EmptyDataHandler extends DataHandler<EmptyDataHandler> {
   private static final DataParser PARSER = DataParser.of(SGRegistry.DataHandlerTypes.CHEST, parser -> {});

   public EmptyDataHandler(DataMap data) {
      super(data);
   }

   public static DataParser parser() {
      return PARSER;
   }

   @Override
   protected void handle(DataHandler.Context context) {
   }
}
