package dev.tigr.asmp.impl.forge;

import dev.tigr.asmp.ASMP;
import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * Modified FML loading plugin used to transform minecraft classes
 * @author Tigermouthbear 7/30/20
 */
public class ASMPForgeLoader extends ASMP implements IClassTransformer, IFMLLoadingPlugin {
	public ASMPForgeLoader(String identifier) {
		super(identifier, ForgeObfuscationMapper.INSTANCE);
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
