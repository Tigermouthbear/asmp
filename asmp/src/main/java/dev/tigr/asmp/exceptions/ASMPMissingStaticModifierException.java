package dev.tigr.asmp.exceptions;

/**
 * @author Tigermouthbear 2/7/21
 */
public class ASMPMissingStaticModifierException extends RuntimeException {
    public ASMPMissingStaticModifierException(String clazz, String method) {
        super("Missing static modifier on method " + method + " in " + clazz);
    }
}
