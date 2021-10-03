package dev.tigr.asmp.impl.forge;

import dev.tigr.asmp.ASMP;
import dev.tigr.asmp.obfuscation.ObfuscationMapper;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import org.objectweb.asm.tree.ClassNode;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * Modified FML loading plugin used to transform minecraft classes
 * @author Tigermouthbear 2/15/21
 */
public class ASMPForgeLoader extends ASMP implements IFMLLoadingPlugin {
	private static final Map<String, ASMPForgeLoader> instances = new HashMap<>();
	private final Class<?> transformer;

	public ASMPForgeLoader(String identifier, Class<?> transformer) {
		super(identifier, new ObfuscationMapper());
		this.transformer = transformer;
		instances.put(identifier, this);
	}

	@Override
	public ClassNode transform(String name, ClassNode classNode) {
		return super.transform(name, classNode);
	}

	public static ASMPForgeLoader get(String identifier) {
		return instances.get(identifier);
	}

	@Override
	public String[] getASMTransformerClass() {
		return new String[] { transformer.getName() };
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
