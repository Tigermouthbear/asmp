package dev.tigr.asmp.exceptions;

/**
 * @author Tigermouthbear 2/12/21
 */
public class ASMPMissingApSetting extends RuntimeException {
    public ASMPMissingApSetting(String name) {
        super("Missing annotation processor setting: " + name + "!");
    }
}
