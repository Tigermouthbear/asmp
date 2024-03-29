package dev.tigr.asmp.exceptions;

/**
 * @author Tigermouthbear 10/1/21
 */
public class ASMPBadOrdinalException extends RuntimeException {
    public ASMPBadOrdinalException(String clazz, String method, int size) {
        super("Bad target ordinal on method " + method + " in " + clazz + ". There are only " + size + " nodes");
    }
}
