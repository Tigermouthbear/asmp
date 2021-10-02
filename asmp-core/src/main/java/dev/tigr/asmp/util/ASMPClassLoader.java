package dev.tigr.asmp.util;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.util.HashMap;

public class ASMPClassLoader extends ClassLoader {
    private final HashMap<String, Class<?>> classHashMap = new HashMap<>();

    public ASMPClassLoader(ClassLoader classLoader) {
        super(classLoader);
    }

    private Class<?> defineClass(String name, byte[] bytes) {
        return defineClass(name, bytes, 0, bytes.length);
    }

    public Class<?> defineClass(ClassNode classNode) {
        Class<?> clazz = classHashMap.get(classNode.name);
        if(clazz == null) {
            String originalName = classNode.name;
            classNode.name = classNode.name + "Generated";
            ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
            classNode.accept(classWriter);
            clazz = defineClass(classNode.name.replaceAll("/", "."), classWriter.toByteArray());
            classHashMap.put(originalName, clazz);
        }
        return clazz;
    }
}
