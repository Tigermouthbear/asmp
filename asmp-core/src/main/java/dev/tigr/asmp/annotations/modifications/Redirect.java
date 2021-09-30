package dev.tigr.asmp.annotations.modifications;

import dev.tigr.asmp.annotations.At;
import dev.tigr.asmp.modification.Modificate;
import dev.tigr.asmp.modification.modifications.RedirectModification;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Tigermouthbear 3/17/21
 */
@Modificate(RedirectModification.class)
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Redirect {
    String method();

    At at();
}
