package dev.tigr.asmp.annotations.modifications;

import dev.tigr.asmp.annotations.At;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Tigermouthbear 2/10/21
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Modify {
	String value() default "";

	At at() default @At("NONE");
}
