package dev.tigr.asmp.annotations.modifications;

import dev.tigr.asmp.modification.Modificate;
import dev.tigr.asmp.modification.modifications.OverwriteModification;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Tigermouthbear 3/17/21
 */
@Modificate(OverwriteModification.class)
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Overwrite {
    String value();
}
