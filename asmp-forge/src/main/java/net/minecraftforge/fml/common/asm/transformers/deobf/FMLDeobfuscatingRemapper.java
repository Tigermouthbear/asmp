package net.minecraftforge.fml.common.asm.transformers.deobf;

import java.util.Map;

/**
 * Wrapper class for ASMPForgeLoader
 * @author Tigermouthbear 7/31/20
 */
public class FMLDeobfuscatingRemapper {
    public static final FMLDeobfuscatingRemapper INSTANCE = new FMLDeobfuscatingRemapper();

    public String unmap(String string) {
        return null;
    }

    private Map<String,String> getFieldMap(String className, boolean raw) {
        return null;
    }

    private Map<String,String> getMethodMap(String className) {
        return null;
    }
}
