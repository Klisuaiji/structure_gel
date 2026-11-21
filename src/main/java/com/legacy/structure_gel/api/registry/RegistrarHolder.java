package com.legacy.structure_gel.api.registry;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.legacy.structure_gel.api.registry.registrar.RegistrarHandler;

import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * Put this annotation on a class that has {@link RegistrarHandler} fields it to
 * register them automatically. This will initialize any class annotated during
 * {@link RegisterEvent}.
 * 
 * {@link RegistrarHandler} fields must be public and static.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface RegistrarHolder
{
}
