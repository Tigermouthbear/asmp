package dev.tigr.asmp.modification.modifications;

import dev.tigr.asmp.ASMP;
import dev.tigr.asmp.annotations.modifications.Modify;
import dev.tigr.asmp.exceptions.ASMPBadArgumentsException;
import dev.tigr.asmp.exceptions.ASMPMethodNotFoundException;
import dev.tigr.asmp.modification.Modification;
import dev.tigr.asmp.util.NodeUtils;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Gives method node directly patch for patching
 * @author Tigermouthbear 7/31/20
 */
public class ModifyModification extends Modification<Modify> {
    public ModifyModification(ASMP asmp, Modify annotation) {
        super(asmp, annotation);
    }

    @Override
    public void invoke(ClassNode classNode, Object patch, Method method) {
        String input = annotation.value();
        if(input.isEmpty()) {
            try {
                method.invoke(patch, classNode);
            } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
                throw new ASMPBadArgumentsException(patch.getClass().getName(), method.getName());
            }
        } else {
            // its a method so find and pass the method node in
            MethodNode methodNode = NodeUtils.getMethod(classNode, unmapMethodReference(input));
            if(methodNode != null) {
                try {
                    method.invoke(patch, methodNode);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                    throw new ASMPBadArgumentsException(patch.getClass().getName(), method.getName());
                }
            } else throw new ASMPMethodNotFoundException(patch.getClass().getName(), method.getName());
        }
    }
}
