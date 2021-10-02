package dev.tigr.asmp.modification.modifications;

import dev.tigr.asmp.ASMP;
import dev.tigr.asmp.annotations.Annotations;
import dev.tigr.asmp.modification.Modification;
import dev.tigr.asmp.util.Reference;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.lang.reflect.Modifier;

/**
 * @author Tigermouthbear 10/1/21
 */
public class GetterModification extends Modification<Annotations.Getter> {
    public GetterModification(ASMP asmp, Annotations.Getter annotation) {
        super(asmp, annotation);
    }

    @Override
    public void invoke(String patchClassName, ClassNode classNode, MethodNode methodNode) {
        // first add patch to interfaces
        if(!classNode.interfaces.contains(patchClassName)) classNode.interfaces.add(patchClassName);

        // then create invoker method in classNode
        methodNode.access = Opcodes.ACC_PUBLIC;
        InsnList instructions = new InsnList();

        // make instructions
        Reference field = unmapFieldReference(annotation.getValue());
        instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        instructions.add(new FieldInsnNode(Modifier.isStatic(methodNode.access) ? Opcodes.GETSTATIC : Opcodes.GETFIELD, field.getOwner(), field.getName(), Type.getReturnType(methodNode.desc).getDescriptor()));
        instructions.add(new InsnNode(Type.getReturnType(methodNode.desc).getOpcode(Opcodes.IRETURN)));

        // add to class
        methodNode.instructions = instructions;
        classNode.methods.add(methodNode);
    }
}
