package com.legacy.structure_gel.core.registry;

import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.legacy.structure_gel.api.registry.RegistrarHolder;
import com.legacy.structure_gel.api.registry.registrar.RegistrarHandler;
import com.legacy.structure_gel.core.StructureGelMod;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.ModFileScanData;
import net.neoforged.neoforgespi.language.ModFileScanData.AnnotationData;

/**
 * Automatically loads {@link RegistrarHandler} fields in a
 * {@link RegistrarHolder} annotated class.
 */
public class RegistrarLoader
{
	private static boolean loaded = false;

	public static void loadRegistrars()
	{
		if (loaded)
			return;
		// Set this first in case findHandlers() throws an exception.
		loaded = true;

		for (var entry : findHandlers(RegistrarHolder.class).entrySet())
		{
			List<RegistrarHandler<?>> handlers = entry.getValue();
			String modID = entry.getKey();
			IEventBus modBus = ModList.get().getModContainerById(modID).get().getEventBus();
			StructureGelMod.LOGGER.debug("Found and registering {} annotated {} for {}", handlers.size(), handlerStr(), modID);
			RegistrarHandler.registerHandlers(modID, modBus, handlers);
		}
	}

	private static Map<String, List<RegistrarHandler<?>>> findHandlers(Class<?> annotation)
	{
		org.objectweb.asm.Type registrarHolder = org.objectweb.asm.Type.getType(annotation);

		// Locate static registrar handlers
		Map<String, List<RegistrarHandler<?>>> handlers = new HashMap<>();
		for (AnnotationData annotationAata : ModList.get().getAllScanData().stream().map(ModFileScanData::getAnnotations).flatMap(Collection::stream).filter(a -> registrarHolder.equals(a.annotationType())).toList())
		{
			String className = annotationAata.memberName();
			try
			{
				Class<?> clazz = Class.forName(className);
	
				java.lang.reflect.Field[] fields = clazz.getDeclaredFields();
				for (var field : fields)
				{
					// Only run for registrar handler field
					if (RegistrarHandler.class.isAssignableFrom(field.getType()))
					{
						// If public and static, evaluate the field
						int modifiers = field.getModifiers();
						if (Modifier.isStatic(modifiers) && Modifier.isPublic(modifiers))
						{
							try
							{
								RegistrarHandler<?> handler = RegistrarHandler.class.cast(field.get(null));
								String modID = handler.getModID();
								handlers.computeIfAbsent(modID, m -> new LinkedList<>()).add(handler);
							}
							catch (IllegalArgumentException | IllegalAccessException e)
							{
								throw new IllegalStateException("Failed to get " + handlerStr() + " field " + field + " from class " + className);
							}
						}
						else
						{
							throw new IllegalStateException("Failed to load " + handlerStr() + " " + field.toString() + " as it must be public and static.");
						}
					}
				}
			}
			catch (ClassNotFoundException e)
			{
				throw new IllegalStateException("Failed to locate " + annStr(annotation) + " class " + className);
			}
		}

		return handlers;

	}

	private static String handlerStr()
	{
		return RegistrarHandler.class.getSimpleName();
	}
	
	private static String annStr(Class<?> annotation)
	{
		return "@" + annotation.getSimpleName();
	}
}
