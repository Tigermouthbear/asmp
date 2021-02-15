package dev.tigr.asmp.impl.forge;

import dev.tigr.asmp.obfuscation.ObfuscationMapper;
import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraftforge.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Minecraft class transformer used to modify classes
 * @author Tigermouthbear 2/15/21
 */
public class ASMPForgeTransformer implements IClassTransformer {
    private static final boolean OBFUSCATED = !FMLDeobfuscatingRemapper.INSTANCE.unmap("net/minecraft/client/Minecraft").equals("net/minecraft/client/Minecraft");
    private final ASMPForgeLoader asmpForgeLoader;

    public ASMPForgeTransformer(ASMPForgeLoader asmpForgeLoader) {
        this.asmpForgeLoader = asmpForgeLoader;

        // load mappings
        InputStream mappings = Thread.currentThread().getContextClassLoader().getResourceAsStream("asmp." + asmpForgeLoader.getIdentifier() + "." + (OBFUSCATED ? "notch" : "searge") + ".srg");
        if(mappings == null) throw new RuntimeException("ASMP failed to load mappings!");
        try {
            ((ObfuscationMapper) asmpForgeLoader.getObfuscationMapper()).read(new InputStreamReader(mappings), ObfuscationMapper.Format.SRG);
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        return asmpForgeLoader.transform(transformedName, basicClass);
    }
}
