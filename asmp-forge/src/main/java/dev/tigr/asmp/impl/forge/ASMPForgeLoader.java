package dev.tigr.asmp.impl.forge;

import dev.tigr.asmp.ASMP;
import dev.tigr.asmp.obfuscation.SrgObfuscationMapper;
import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraftforge.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;

import javax.annotation.Nullable;
import java.io.InputStreamReader;
import java.util.Map;

/**
 * Modified FML loading plugin used to transform minecraft classes
 * @author Tigermouthbear 7/30/20
 */
public class ASMPForgeLoader extends ASMP implements IClassTransformer, IFMLLoadingPlugin {
	private static final boolean OBFUSCATED = !FMLDeobfuscatingRemapper.INSTANCE.unmap("net/minecraft/client/Minecraft").equals("net/minecraft/client/Minecraft");

	public ASMPForgeLoader(String identifier) {
		super(identifier);
		SrgObfuscationMapper srgObfuscationMapper = new SrgObfuscationMapper();
		srgObfuscationMapper.read(new InputStreamReader(ASMPForgeLoader.class.getResourceAsStream("asmp." + identifier + "." + (OBFUSCATED ? "notch" : "searge") + ".srg")));
		obfuscationMapper = srgObfuscationMapper;
	}

	@Override
	public byte[] transform(String name, String transformedName, byte[] basicClass) {
		return transform(transformedName, basicClass);
	}

	@Override
	public String[] getASMTransformerClass() {
		return new String[] { getClass().getName() };
	}

	@Override
	public String getModContainerClass() {
		return null;
	}

	@Nullable
	@Override
	public String getSetupClass() {
		return null;
	}

	@Override
	public void injectData(Map<String, Object> data) {
	}

	@Override
	public String getAccessTransformerClass() {
		return null;
	}
}
