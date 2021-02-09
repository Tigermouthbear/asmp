package dev.tigr.asmp.modification.modifications;

import dev.tigr.asmp.ASMP;
import dev.tigr.asmp.exceptions.ASMPMethodNotFoundException;
import dev.tigr.asmp.util.NodeUtils;
import dev.tigr.asmp.annotations.modifications.Modify;
import dev.tigr.asmp.modification.Modification;
import dev.tigr.asmp.util.Reference;
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
        Reference unmapped = unmapMethodReference(annotation.value());
        ASMP.LOGGER.info(unmapped.getOwner() + " " + unmapped.getName() + " " + unmapped.getDesc());
        MethodNode methodNode = NodeUtils.getMethod(classNode, unmapped);
        if(methodNode != null) {
            try {
                method.invoke(patch, methodNode);
            } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        } else throw new ASMPMethodNotFoundException(patch.getClass().getName(), method.getName());
    }
}
