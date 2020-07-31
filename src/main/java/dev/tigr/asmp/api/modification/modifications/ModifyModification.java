package dev.tigr.asmp.api.modification.modifications;

import dev.tigr.asmp.api.annotations.Modify;
import dev.tigr.asmp.api.modification.Modification;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Gives method node directly patch for patching
 * @author Tigermouthbear 7/31/20
 */
public class ModifyModification extends Modification<Modify> {
    public ModifyModification(Modify annotation) {
        super(annotation);
    }

    @Override
    public void invoke(ClassNode classNode, Object patch, Method method) {
        for(MethodNode methodNode: classNode.methods) {
            if(methodNode.name.equals(annotation.value()) && (annotation.desc().isEmpty() || methodNode.desc.equals(annotation.desc()))) {
                try {
                    method.invoke(patch, methodNode);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
