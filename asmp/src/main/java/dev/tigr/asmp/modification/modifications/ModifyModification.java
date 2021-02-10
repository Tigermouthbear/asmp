package dev.tigr.asmp.modification.modifications;

import dev.tigr.asmp.ASMP;
import dev.tigr.asmp.annotations.At;
import dev.tigr.asmp.annotations.modifications.Modify;
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
public class ModifyModification extends Modification<Modify> {
    public ModifyModification(ASMP asmp, Modify annotation) {
        super(asmp, annotation);
    }

    @Override
    public void invoke(ClassNode classNode, Object patch, Method method) {
        String input = annotation.value();
        At.Target target = annotation.at().value();
        if(input.isEmpty()) {
            // user wants classnode passed to method
            try {
                method.invoke(patch, classNode);
            } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
                throw new ASMPBadArgumentsException(patch.getClass().getName(), method.getName());
            }
        } else {
            // we need to find a method node to use from now on
            MethodNode methodNode = NodeUtils.getMethod(classNode, unmapMethodReference(input));
            if(methodNode != null) {
                if(target == At.Target.NONE) {
                    // user just wants raw method node
                    try {
                        method.invoke(patch, methodNode);
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        e.printStackTrace();
                        throw new ASMPBadArgumentsException(patch.getClass().getName(), method.getName());
                    }
                } else {
                    // user wants to use an insn modifier
                    InsnModifier insnModifier = new InsnModifier(this, methodNode, annotation.at());
                    try {
                        method.invoke(patch, insnModifier);
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        e.printStackTrace();
                        throw new ASMPBadArgumentsException(patch.getClass().getName(), method.getName());
                    }
                }
            } else throw new ASMPMethodNotFoundException(patch.getClass().getName(), method.getName());
        }
    }
}
