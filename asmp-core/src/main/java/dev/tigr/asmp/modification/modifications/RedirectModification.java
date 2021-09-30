package dev.tigr.asmp.modification.modifications;

import dev.tigr.asmp.ASMP;
import dev.tigr.asmp.annotations.modifications.Redirect;
import dev.tigr.asmp.exceptions.ASMPMethodNotFoundException;
import dev.tigr.asmp.modification.Modification;
import dev.tigr.asmp.util.InsnModifier;
import dev.tigr.asmp.util.NodeUtils;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.lang.reflect.Method;

/**
 * @author Tigermouthbear 3/17/21
 */
public class RedirectModification extends Modification<Redirect> {
    public RedirectModification(ASMP asmp, Redirect annotation) {
        super(asmp, annotation);
    }

    @Override
    public void invoke(ClassNode classNode, Object patch, Method method) {
        // add/copy injection to class
        MethodNode redirect = NodeUtils.readMethodNode(patch, method);
        redirect.access = Opcodes.ACC_PRIVATE;
        redirect.name = redirect.name + "_asmp_redirect" + classNode.methods.size(); // unique name
        classNode.methods.add(redirect);

        // find method node
        MethodNode methodNode = NodeUtils.getMethod(classNode, unmapMethodReference(annotation.method()));
        if(methodNode != null) {
            // replace with redirect
            MethodInsnNode redirectNode = new MethodInsnNode(Opcodes.INVOKEVIRTUAL, classNode.name, redirect.name, redirect.desc);
            InsnModifier insnModifier = new InsnModifier(asmp, classNode, methodNode, annotation.at());
            insnModifier.replace(redirectNode);
        }
        else throw new ASMPMethodNotFoundException(patch.getClass().getName(), method.getName());
    }
}
