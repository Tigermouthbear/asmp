package dev.tigr.asmp.modification.modifications;

import dev.tigr.asmp.ASMP;
import dev.tigr.asmp.annotations.Annotations;
import dev.tigr.asmp.modification.Modification;
import dev.tigr.asmp.util.Reference;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

/**
 * @author Tigermouthbear 10/1/21
 */
public class InvokerModification extends Modification<Annotations.Invoker> {
    public InvokerModification(ASMP asmp, Annotations.Invoker annotation) {
        super(asmp, annotation);
    }

    @Override
    public void invoke(ClassNode patchNode, ClassNode classNode, MethodNode methodNode) {
        // first add patch to interfaces
        if(!classNode.interfaces.contains(patchNode.name)) classNode.interfaces.add(patchNode.name);

        // then create invoker method in classNode
        methodNode.access = Opcodes.ACC_PUBLIC;
        InsnList instructions = new InsnList();

        // make instructions
        int i = 1;
        for(Type type: Type.getArgumentTypes(methodNode.desc)) {
            instructions.add(new VarInsnNode(type.getOpcode(Opcodes.ILOAD), i++));
        }
        Reference reference = unmapMethodReference(annotation.getValue());
        instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, reference.getOwner(), reference.getName(), reference.getDesc()));
        instructions.add(new InsnNode(Type.getReturnType(methodNode.desc).getOpcode(Opcodes.IRETURN)));

        // add to class
        methodNode.instructions = instructions;
        classNode.methods.add(methodNode);
    }
}
