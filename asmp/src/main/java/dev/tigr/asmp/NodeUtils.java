package dev.tigr.asmp;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;

public class NodeUtils {
    public static ClassNode readClassNode(Object object) {
        // read class
        ClassReader classReader = null;
        try {
            classReader = new ClassReader(object.getClass().getResourceAsStream("/" + object.getClass().getName().replaceAll("\\.", "/") + ".class"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        assert classReader != null;

        // accept and return class node
        ClassNode classNode = new ClassNode();
        classReader.accept(classNode, 0);
        return classNode;
    }

    public static MethodNode getMethod(ClassNode classNode, String name) {
        for(MethodNode methodNode: classNode.methods) {
            if(methodNode.name.equals(name)) return methodNode;
        }
        return null;
    }

    public static MethodNode getMethod(ClassNode classNode, String name, String desc) {
        for(MethodNode methodNode: classNode.methods) {
            if(methodNode.name.equals(name) && methodNode.desc.equals(desc)) return methodNode;
        }
        return null;
    }
}
