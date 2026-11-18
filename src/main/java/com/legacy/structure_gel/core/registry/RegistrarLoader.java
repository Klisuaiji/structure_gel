package com.legacy.structure_gel.core.registry;

import com.legacy.structure_gel.api.registry.RegistrarHolder;
import com.legacy.structure_gel.api.registry.registrar.RegistrarHandler;
import com.legacy.structure_gel.core.StructureGelMod;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.ModFileScanData;
import net.neoforged.neoforgespi.language.ModFileScanData.AnnotationData;
import org.objectweb.asm.Type;

public class RegistrarLoader {
   private static boolean loaded = false;

   public static void loadRegistrars() {
      if (!loaded) {
         loaded = true;

         for (Entry<String, List<RegistrarHandler<?>>> entry : findHandlers(RegistrarHolder.class).entrySet()) {
            List<RegistrarHandler<?>> handlers = entry.getValue();
            String modID = entry.getKey();
            IEventBus modBus = ((ModContainer)ModList.get().getModContainerById(modID).get()).getEventBus();
            StructureGelMod.LOGGER.debug("Found and registering {} annotated {} for {}", handlers.size(), handlerStr(), modID);
            RegistrarHandler.registerHandlers(modID, modBus, handlers);
         }
      }
   }

   private static Map<String, List<RegistrarHandler<?>>> findHandlers(Class<?> annotation) {
      Type registrarHolder = Type.getType(annotation);
      Map<String, List<RegistrarHandler<?>>> handlers = new HashMap<>();

      for (AnnotationData annotationAata : ModList.get()
         .getAllScanData()
         .stream()
         .map(ModFileScanData::getAnnotations)
         .flatMap(Collection::stream)
         .filter(a -> registrarHolder.equals(a.annotationType()))
         .toList()) {
         String className = annotationAata.memberName();

         try {
            Class<?> clazz = Class.forName(className);
            Field[] fields = clazz.getDeclaredFields();

            for (Field field : fields) {
               if (RegistrarHandler.class.isAssignableFrom(field.getType())) {
                  int modifiers = field.getModifiers();
                  if (!Modifier.isStatic(modifiers) || !Modifier.isPublic(modifiers)) {
                     throw new IllegalStateException("Failed to load " + handlerStr() + " " + field.toString() + " as it must be public and static.");
                  }

                  try {
                     RegistrarHandler<?> handler = RegistrarHandler.class.cast(field.get(null));
                     String modID = handler.getModID();
                     handlers.computeIfAbsent(modID, m -> new LinkedList<>()).add(handler);
                  } catch (IllegalAccessException | IllegalArgumentException var15) {
                     throw new IllegalStateException("Failed to get " + handlerStr() + " field " + field + " from class " + className);
                  }
               }
            }
         } catch (ClassNotFoundException var16) {
            throw new IllegalStateException("Failed to locate " + annStr(annotation) + " class " + className);
         }
      }

      return handlers;
   }

   private static String handlerStr() {
      return RegistrarHandler.class.getSimpleName();
   }

   private static String annStr(Class<?> annotation) {
      return "@" + annotation.getSimpleName();
   }
}
