package dev.tigr.asmp.util;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

public class ASMPClassLoader extends ClassLoader {
    public ASMPClassLoader(ClassLoader classLoader) {
        super(classLoader);
    }

    public Class<?> defineClass(String name, byte[] bytes) {
        return defineClass(name, bytes, 0, bytes.length);
    }

    public Class<?> defineClass(ClassNode classNode) {
        classNode.name = classNode.name + "Generated";
        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        classNode.accept(classWriter);
        return defineClass(classNode.name.replaceAll("/", "."), classWriter.toByteArray());
    }
}
