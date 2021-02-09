package dev.tigr.asmp.exceptions;

/**
 * @author Tigermouthbear 2/8/21
 */
public class ASMPBadTargetException extends RuntimeException {
    public ASMPBadTargetException(String target) {
        super("Bad target " + target);
    }
}
