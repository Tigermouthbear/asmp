package net.minecraft.launchwrapper;

/**
 * Wrapper class for ASMPForgeLoader
 * @author Tigermouthbear 8/2/20
 */
public interface IClassTransformer {
    byte[] transform(String name, String transformedName, byte[] basicClass);
}
