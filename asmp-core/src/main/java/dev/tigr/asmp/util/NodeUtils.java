package dev.tigr.asmp.util;

import dev.tigr.asmp.ASMP;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.util.Printer;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceMethodVisitor;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
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

    public static MethodNode readMethodNode(Object object, Method method) {
        return readMethodNode(object.getClass(), method);
    }

    public static MethodNode readMethodNode(Class<?> clazz, Method method) {
        ClassNode classNode = readClassNode(clazz);
        return getMethod(classNode, new Reference(classNode.name, method.getName(), Type.getMethodDescriptor(method)));
    }

    public static MethodNode getMethod(ClassNode classNode, Reference reference) {
        for(MethodNode methodNode: classNode.methods) {
            if(methodNode.name.equals(reference.getName()) && methodNode.desc.equals(reference.getDesc())) return methodNode;
        }
        return null;
    }

    public static boolean isReturn(AbstractInsnNode abstractInsnNode) {
        return abstractInsnNode.getOpcode() >= Opcodes.IRETURN && abstractInsnNode.getOpcode() <= Opcodes.RETURN;
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
                return null;
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

    public static AbstractInsnNode castToNonPrimitive(Type type) {
        String nonPrimitive = getNonPrimitiveClass(type);
        if(nonPrimitive.equals("java/lang/Object")) return new TypeInsnNode(Opcodes.CHECKCAST, type.getClassName());
        else return new TypeInsnNode(Opcodes.CHECKCAST, nonPrimitive);
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
                return null;
        }

        return new MethodInsnNode(Opcodes.INVOKEVIRTUAL, owner, name, descriptor, false);
    }

    public static void printInsns(InsnList insnNodes) {
        for(AbstractInsnNode insnNode: insnNodes) {
            ASMP.LOGGER.info(insnToString(insnNode));
        }
    }

    public static String insnToString(AbstractInsnNode insn){
        insn.accept(mp);
        StringWriter sw = new StringWriter();
        printer.print(new PrintWriter(sw));
        printer.getText().clear();
        return sw.toString();
    }

    private static Printer printer = new Textifier();
    private static TraceMethodVisitor mp = new TraceMethodVisitor(printer);
}
