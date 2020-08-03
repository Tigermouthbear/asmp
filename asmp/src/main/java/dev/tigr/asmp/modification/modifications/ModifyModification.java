package dev.tigr.asmp.modification.modifications;

import dev.tigr.asmp.ASMP;
import dev.tigr.asmp.annotations.modifications.Modify;
import dev.tigr.asmp.modification.Modification;
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
        String name = unmapMethod(annotation.value());
        String desc = unmapDesc(annotation.desc());

        for(MethodNode methodNode: classNode.methods) {
            if(methodNode.name.equals(name) && (desc.isEmpty() || methodNode.desc.equals(desc))) {
                try {
                    method.invoke(patch, methodNode);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
