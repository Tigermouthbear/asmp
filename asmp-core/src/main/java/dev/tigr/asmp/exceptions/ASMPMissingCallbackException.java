package dev.tigr.asmp.exceptions;

/**
 * @author Tigermouthbear 2/7/21
 */
public class ASMPMissingCallbackException extends RuntimeException {
    public ASMPMissingCallbackException(String clazz, String method) {
        super("Missing callback parameter on method " + method + " in " + clazz);
    }
}
