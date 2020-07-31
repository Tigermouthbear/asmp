package net.minecraftforge.fml.common.asm.transformers.deobf;

/**
 * Wrapper class for ASMPForgeLoader
 * @author Tigermouthbear 7/31/20
 */
public class FMLDeobfuscatingRemapper {
    public static final FMLDeobfuscatingRemapper INSTANCE = new FMLDeobfuscatingRemapper();

    public String unmap(String string) {
        return string;
    }
}
