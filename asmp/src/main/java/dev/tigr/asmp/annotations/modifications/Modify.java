package dev.tigr.asmp.annotations.modifications;

import dev.tigr.asmp.modification.Modificate;
import dev.tigr.asmp.modification.modifications.ModifyModification;

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
