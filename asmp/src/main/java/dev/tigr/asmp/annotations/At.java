package dev.tigr.asmp.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author Tigermouthbear
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface At {
    enum Target { HEAD, RETURN, INVOKE }

    Target value();

    String target() default "";

    int ordinal() default -1;
}
