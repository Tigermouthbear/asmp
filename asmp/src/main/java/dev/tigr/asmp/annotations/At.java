package dev.tigr.asmp.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * represents a location in a method's bytecode instruction list
 * @author Tigermouthbear
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface At {
    enum Target { NONE, HEAD, RETURN, INVOKE }

    Target value();

    String target() default "";

    int ordinal() default -1;
}
