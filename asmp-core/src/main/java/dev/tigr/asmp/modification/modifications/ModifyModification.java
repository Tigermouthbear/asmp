package dev.tigr.asmp.modification.modifications;

import dev.tigr.asmp.ASMP;
import dev.tigr.asmp.annotations.Annotations;
import dev.tigr.asmp.exceptions.ASMPBadArgumentsException;
import dev.tigr.asmp.exceptions.ASMPMethodNotFoundException;
import dev.tigr.asmp.modification.Modification;
import dev.tigr.asmp.util.InsnModifier;
import dev.tigr.asmp.util.NodeUtils;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Gives method node directly patch for patching
 * @author Tigermouthbear 2/10/21
 */
public class ModifyModification extends Modification<Annotations.Modify> {
    public ModifyModification(ASMP asmp, Annotations.Modify annotation) {
        super(asmp, annotation);
    }

    @Override
    public void invoke(ClassNode patchNode, ClassNode classNode, MethodNode modify, Object generated) {
        Method method = NodeUtils.getMethod(generated.getClass(), modify.name, modify.desc);
        if(method != null) {
            String input = annotation.getValue();
            String target = annotation.getAt().getValue();
            if(input.isEmpty()) {
                // user wants classnode passed to method
                try {
                    method.invoke(generated, classNode);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                    throw new ASMPBadArgumentsException(patchNode.name, method.getName());
                }
            } else {
                // we need to find a method node to use from now on
                MethodNode methodNode = NodeUtils.getMethod(classNode, unmapMethodReference(input));
                if(methodNode != null) {
                    if(target.equals("NONE") || target.isEmpty()) {
                        // user just wants raw method node
                        try {
                            method.invoke(generated, methodNode);
                        } catch (IllegalAccessException | InvocationTargetException e) {
                            e.printStackTrace();
                            throw new ASMPBadArgumentsException(patchNode.name, method.getName());
                        }
                    } else {
                        // user wants to use an insn modifier
                        InsnModifier insnModifier = new InsnModifier(asmp, classNode, methodNode, annotation.getAt());
                        try {
                            method.invoke(generated, insnModifier);
                        } catch (IllegalAccessException | InvocationTargetException e) {
                            e.printStackTrace();
                            throw new ASMPBadArgumentsException(patchNode.name, method.getName());
                        }
                    }
                } else throw new ASMPMethodNotFoundException(patchNode.name, method.getName());
            }
        } else throw new ASMPMethodNotFoundException(patchNode.name, modify.name);
    }
}
