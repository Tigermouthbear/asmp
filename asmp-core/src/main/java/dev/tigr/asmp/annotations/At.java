package dev.tigr.asmp.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * represents a location in a method's bytecode instruction list
 * @author Tigermouthbear
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface At {
    /**
     * Values: NONE, HEAD, RETURN, INVOKE
     */
    String value();

    String target() default "";

    int ordinal() default -1;
}
