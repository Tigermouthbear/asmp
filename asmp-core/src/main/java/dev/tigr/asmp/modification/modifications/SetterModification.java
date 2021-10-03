package dev.tigr.asmp.modification.modifications;

import dev.tigr.asmp.ASMP;
import dev.tigr.asmp.annotations.Annotations;
import dev.tigr.asmp.exceptions.ASMPFieldNotFoundException;
import dev.tigr.asmp.modification.Modification;
import dev.tigr.asmp.util.NodeUtils;
import dev.tigr.asmp.util.Reference;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.lang.reflect.Modifier;

/**
 * @author Tigermouthbear 10/1/21
 */
public class SetterModification extends Modification<Annotations.Setter> {
    public SetterModification(ASMP asmp, Annotations.Setter annotation) {
        super(asmp, annotation);
    }

    @Override
    public void invoke(ClassNode patchNode, ClassNode classNode, MethodNode methodNode) {
        // first add patch to interfaces
        if(!classNode.interfaces.contains(patchNode.name)) classNode.interfaces.add(patchNode.name);

        Reference field = unmapFieldReference(annotation.getValue());
        FieldNode fieldNode = NodeUtils.getField(classNode, field);
        if(fieldNode != null) {
            // check if field is final, if so make not final
            if(Modifier.isFinal(fieldNode.access)) fieldNode.access &= ~Opcodes.ACC_FINAL;

            // then create setter method in classNode
            methodNode.access = Opcodes.ACC_PUBLIC;
            InsnList instructions = new InsnList();

            // make instructions
            instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
            instructions.add(new VarInsnNode(Type.getArgumentTypes(methodNode.desc)[0].getOpcode(Opcodes.ILOAD), 1));
            instructions.add(new FieldInsnNode(Modifier.isStatic(methodNode.access) ? Opcodes.PUTSTATIC : Opcodes.PUTFIELD, field.getOwner(), field.getName(), fieldNode.desc));
            instructions.add(new InsnNode(Type.getReturnType(methodNode.desc).getOpcode(Opcodes.IRETURN)));

            // add to class
            methodNode.instructions = instructions;
            classNode.methods.add(methodNode);
        } else throw new ASMPFieldNotFoundException(classNode.name, field.toString());
    }
}
