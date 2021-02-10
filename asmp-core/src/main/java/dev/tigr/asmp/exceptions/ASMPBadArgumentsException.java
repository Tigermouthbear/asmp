package dev.tigr.asmp.exceptions;

/**
 * @author Tigermouthbear 2/8/21
 */
public class ASMPBadArgumentsException extends RuntimeException {
    public ASMPBadArgumentsException(String clazz, String method) {
        super("Bad parameters on method " + method + " in " + clazz);
    }
}
