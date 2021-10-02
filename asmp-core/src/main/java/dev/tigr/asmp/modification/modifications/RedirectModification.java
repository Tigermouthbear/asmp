package dev.tigr.asmp.modification.modifications;

import dev.tigr.asmp.ASMP;
import dev.tigr.asmp.annotations.Annotations;
import dev.tigr.asmp.exceptions.ASMPMethodNotFoundException;
import dev.tigr.asmp.modification.Modification;
import dev.tigr.asmp.util.InsnModifier;
import dev.tigr.asmp.util.NodeUtils;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * @author Tigermouthbear 3/17/21
 */
public class RedirectModification extends Modification<Annotations.Redirect> {
    public RedirectModification(ASMP asmp, Annotations.Redirect annotation) {
        super(asmp, annotation);
    }

    @Override
    public void invoke(String patchClassName, ClassNode classNode, MethodNode redirect) {
        // add/copy injection to class
        redirect.access = Opcodes.ACC_PRIVATE;
        redirect.name = redirect.name + "_asmp_redirect" + classNode.methods.size(); // unique name
        classNode.methods.add(redirect);

        // find method node
        MethodNode methodNode = NodeUtils.getMethod(classNode, unmapMethodReference(annotation.getMethod()));
        if(methodNode != null) {
            // replace with redirect
            MethodInsnNode redirectNode = new MethodInsnNode(Opcodes.INVOKEVIRTUAL, classNode.name, redirect.name, redirect.desc);
            InsnModifier insnModifier = new InsnModifier(asmp, classNode, methodNode, annotation.getAt());
            insnModifier.replace(redirectNode);
        }
        else throw new ASMPMethodNotFoundException(patchClassName, redirect.name.replaceAll("_asmp_redirect" + classNode.methods.size(), ""));
    }
}
