package dev.tigr.asmp.exceptions;

/**
 * @author Tigermouthbear 2/8/21
 */
public class ASMPMethodNotFoundException extends RuntimeException {
    public ASMPMethodNotFoundException(String clazz, String method) {
        super("Method target not found on method " + method + " in " + clazz);
    }
}
