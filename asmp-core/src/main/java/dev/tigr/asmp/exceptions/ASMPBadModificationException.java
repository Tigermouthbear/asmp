package dev.tigr.asmp.exceptions;

/**
 * @author Tigermouthbear 10/1/21
 */
public class ASMPBadModificationException extends RuntimeException {
    public ASMPBadModificationException(String clazz, String method) {
        super("Bad modification on method " + method + " in " + clazz);
    }
}
