package dev.tigr.asmp;

import dev.tigr.asmp.annotations.modifications.Modify;
import dev.tigr.asmp.annotations.Patch;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

// test patch for ASMP Forge Loader
@Patch("net.minecraft.client.Minecraft")
public class TestPatch {
	public TestPatch(ClassNode classNode) {
		System.out.println(classNode.name + ":" + classNode.superName);
	}

	@Modify("<init>")
	public void patch(MethodNode methodNode) {
		System.out.println("Test Patch working!");
		System.out.println(methodNode.name + ":" + methodNode.desc + ":" + methodNode.signature);
	}
}
