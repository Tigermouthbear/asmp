package dev.tigr.asmp.modification;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to specify the Modification class for the corresponding annotation
 * @author Tigermouthbear 7/31/20
 */
@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Modificate {
    Class<? extends Modification<?>> value();
}
