package dev.tigr.asmp.api.annotations;

import dev.tigr.asmp.api.modification.Modificate;
import dev.tigr.asmp.api.modification.modifications.ModifyModification;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Tigermouthbear 7/30/20
 */
@Modificate(ModifyModification.class)
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Modify {
	String value();

	String desc() default "";
}
