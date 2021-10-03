package dev.tigr.asmp.modification.modifications;

import dev.tigr.asmp.ASMP;
import dev.tigr.asmp.annotations.Annotations;
import dev.tigr.asmp.exceptions.ASMPMethodNotFoundException;
import dev.tigr.asmp.modification.Modification;
import dev.tigr.asmp.util.NodeUtils;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * @author Tigermouthbear 3/17/21
 */
public class OverwriteModification extends Modification<Annotations.Overwrite> {
    public OverwriteModification(ASMP asmp, Annotations.Overwrite annotation) {
        super(asmp, annotation);
    }

    @Override
    public void invoke(ClassNode patchNode, ClassNode classNode, MethodNode overwrite) {
        // find method node
        MethodNode methodNode = NodeUtils.getMethod(classNode, unmapMethodReference(annotation.getValue()));
        if(methodNode != null) {
            methodNode.instructions = overwrite.instructions;
            shadowVariables(patchNode, methodNode);
        } else throw new ASMPMethodNotFoundException(patchNode.name, overwrite.name);
    }
}
