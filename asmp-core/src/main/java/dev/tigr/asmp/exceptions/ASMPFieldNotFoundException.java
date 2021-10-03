package dev.tigr.asmp.exceptions;

/**
 * @author Tigermouthbear 10/3/21
 */
public class ASMPFieldNotFoundException extends RuntimeException {
    public ASMPFieldNotFoundException(String clazz, String field) {
        super("Field target " + field + " not found in " + clazz);
    }
}
