package dev.tigr.asmp;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.io.IOException;
import java.lang.reflect.Method;

public class NodeUtils {
    public static ClassNode readClassNode(Object object) {
        return readClassNode(object.getClass());
    }

    public static ClassNode readClassNode(Class<?> clazz) {
        // read class
        ClassReader classReader = null;
        try {
            classReader = new ClassReader(clazz.getResourceAsStream("/" + clazz.getName().replaceAll("\\.", "/") + ".class"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        assert classReader != null;

        // accept and return class node
        ClassNode classNode = new ClassNode();
        classReader.accept(classNode, 0);
        return classNode;
    }

    public static MethodNode getMethod(ClassNode classNode, String value) {
        boolean hasDescriptor = value.contains("(");
        int index = hasDescriptor ? value.indexOf("(") : 0;
        String name = hasDescriptor ? value.substring(0, index) : value;
        String desc = hasDescriptor ? value.substring(index) : null;
        for(MethodNode methodNode: classNode.methods) {
            if(methodNode.name.equals(name) && (!hasDescriptor || methodNode.desc.equals(desc))) return methodNode;
        }
        return null;
    }

    public static boolean isReturn(AbstractInsnNode abstractInsnNode) {
        return abstractInsnNode.getOpcode() >= Opcodes.IRETURN && abstractInsnNode.getOpcode() <= Opcodes.RETURN;
    }

    public static String getDescriptor(Method method) {
        Type[] arguments = Type.getArgumentTypes(method);
        StringBuilder descriptor = new StringBuilder("(");
        for(Type type: arguments) {
            descriptor.append(type.getDescriptor());
        }
        descriptor.append(")");
        descriptor.append(Type.getDescriptor(method.getReturnType()));
        return descriptor.toString();
    }

    public static AbstractInsnNode valueOfInsnNode(Type type) {
        String owner;
        String descriptor;

        switch(type.getSort()) {
            case Type.BOOLEAN:
                owner = "java/lang/Boolean";
                descriptor = "(Z)Ljava/lang/Boolean;";
                break;
            case Type.CHAR:
                owner = "java/lang/Character";
                descriptor = "(C)Ljava/lang/Character";
                break;
            case Type.BYTE:
                owner = "java/lang/Byte";
                descriptor = "(B)Ljava/lang/Byte";
                break;
            case Type.SHORT:
                owner = "java/lang/Short";
                descriptor = "(S)Ljava/lang/Short";
                break;
            case Type.INT:
                owner = "java/lang/Integer";
                descriptor = "(I)Ljava/lang/Integer";
                break;
            case Type.FLOAT:
                owner = "java/lang/Float";
                descriptor = "(F)Ljava/lang/Float";
                break;
            case Type.LONG:
                owner = "java/lang/Long";
                descriptor = "(J)Ljava/lang/Long";
                break;
            case Type.DOUBLE:
                owner = "java/lang/Double";
                descriptor = "(D)Ljava/lang/Double";
                break;
            default:
                return new InsnNode(Opcodes.NOP);
        }

        return new MethodInsnNode(Opcodes.INVOKESTATIC, owner, "valueOf", descriptor, false);
    }

    public static String getNonPrimitiveClass(Type type) {
        switch(type.getSort()) {
            case Type.BOOLEAN:
                return "java/lang/Boolean";
            case Type.CHAR:
                return "java/lang/Character";
            case Type.BYTE:
                return "java/lang/Byte";
            case Type.SHORT:
                return "java/lang/Short";
            case Type.INT:
                return "java/lang/Integer";
            case Type.FLOAT:
                return "java/lang/Float";
            case Type.LONG:
                return "java/lang/Long";
            case Type.DOUBLE:
                return "java/lang/Double";
            default:
                return "java/lang/Object";
        }
    }

    public static AbstractInsnNode primitiveValueInsnNode(Type type) {
        String owner;
        String descriptor;
        String name;

        switch(type.getSort()) {
            case Type.BOOLEAN:
                owner = "java/lang/Boolean";
                descriptor = "()Z";
                name = "booleanValue";
                break;
            case Type.CHAR:
                owner = "java/lang/Character";
                descriptor = "()C";
                name = "charValue";
                break;
            case Type.BYTE:
                owner = "java/lang/Byte";
                descriptor = "()B";
                name = "byteValue";
                break;
            case Type.SHORT:
                owner = "java/lang/Short";
                descriptor = "()S";
                name = "shortValue";
                break;
            case Type.INT:
                owner = "java/lang/Integer";
                descriptor = "()I";
                name = "intValue";
                break;
            case Type.FLOAT:
                owner = "java/lang/Float";
                descriptor = "()F";
                name = "floatValue";
                break;
            case Type.LONG:
                owner = "java/lang/Long";
                descriptor = "()J";
                name = "longValue";
                break;
            case Type.DOUBLE:
                owner = "java/lang/Double";
                descriptor = "()D";
                name = "doubleValue";
                break;
            default:
                return new InsnNode(Opcodes.NOP);
        }

        return new MethodInsnNode(Opcodes.INVOKEVIRTUAL, owner, name, descriptor, false);
    }
}
