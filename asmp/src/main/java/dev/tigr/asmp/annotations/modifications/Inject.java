package dev.tigr.asmp.annotations.modifications;

import dev.tigr.asmp.annotations.At;
import dev.tigr.asmp.modification.Modificate;
import dev.tigr.asmp.modification.modifications.InjectModification;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Tigermouthbear 8/1/20
 */
@Modificate(InjectModification.class)
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Inject {
    String method();

    String desc() default "";

    At at();
}
