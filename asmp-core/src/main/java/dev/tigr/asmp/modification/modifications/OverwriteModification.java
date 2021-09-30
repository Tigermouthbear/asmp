package dev.tigr.asmp.modification.modifications;

import dev.tigr.asmp.ASMP;
import dev.tigr.asmp.annotations.modifications.Overwrite;
import dev.tigr.asmp.exceptions.ASMPMethodNotFoundException;
import dev.tigr.asmp.modification.Modification;
import dev.tigr.asmp.util.NodeUtils;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.lang.reflect.Method;

/**
 * @author Tigermouthbear 3/17/21
 */
public class OverwriteModification extends Modification<Overwrite> {
    public OverwriteModification(ASMP asmp, Overwrite annotation) {
        super(asmp, annotation);
    }

    @Override
    public void invoke(ClassNode classNode, Object patch, Method method) {
        // get insns for overwriting
        MethodNode overwrite = NodeUtils.readMethodNode(patch, method);

        // find method node
        MethodNode methodNode = NodeUtils.getMethod(classNode, unmapMethodReference(annotation.value()));
        if(methodNode != null) methodNode.instructions = overwrite.instructions;
        else throw new ASMPMethodNotFoundException(patch.getClass().getName(), method.getName());
    }
}
