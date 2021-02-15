package dev.tigr.asmp.util;

import dev.tigr.asmp.ASMP;
import dev.tigr.asmp.annotations.At;
import org.objectweb.asm.tree.*;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Finds nodes referenced in {@link At} annotation and allows modification of instructions
 * @author Tigermouthbear 2/10/21
 */
public class InsnModifier {
    private final ASMP asmp;
    private final ClassNode classNode;
    private final MethodNode methodNode;
    private final List<AbstractInsnNode> nodes = new ArrayList<>();
    private final Reference reference;

    public InsnModifier(ASMP asmp, ClassNode classNode, MethodNode methodNode, At at) {
        this.asmp = asmp;
        this.classNode = classNode;
        this.methodNode = methodNode;
        reference = at.target().isEmpty() ? null : asmp.getObfuscationMapper().unmapMethodReference(at.target());

        switch(at.value()) {
            case "HEAD":
                nodes.add(methodNode.instructions.getFirst());
                break;
            case "RETURN":
                for(AbstractInsnNode abstractInsnNode: methodNode.instructions) {
                    if(NodeUtils.isReturn(abstractInsnNode)) nodes.add(abstractInsnNode);
                }
                break;
            case "INVOKE":
                // find method insns that match target
                Reference reference = collapseSuperClass(asmp.getObfuscationMapper().unmapMethodReference(at.target()));
                for(AbstractInsnNode abstractInsnNode: methodNode.instructions) {
                    if(abstractInsnNode instanceof MethodInsnNode) {
                        MethodInsnNode methodInsnNode = (MethodInsnNode) abstractInsnNode;
                        if(methodInsnNode.owner.equals(reference.getOwner()) && methodInsnNode.name.equals(reference.getName())
                                && methodInsnNode.desc.equals(reference.getDesc()))
                            nodes.add(abstractInsnNode);
                    }
                }

                // remove if ordinal is not -1
                if(at.ordinal() != -1) {
                    AbstractInsnNode abstractInsnNode = nodes.get(at.ordinal());
                    nodes.clear();
                    nodes.add(abstractInsnNode);
                }
                break;
        }
    }

    private Reference collapseSuperClass(Reference reference) {
        return reference;
    }

    public void insertBefore(AbstractInsnNode abstractInsnNode) {
        nodes.forEach(target -> methodNode.instructions.insertBefore(target, abstractInsnNode));
    }

    public void insertBefore(AbstractInsnNode... abstractInsnNodes) {
        for(AbstractInsnNode abstractInsnNode: abstractInsnNodes) insertBefore(abstractInsnNode);
    }

    public void insertBefore(InsnList insnList) {
        nodes.forEach(target -> methodNode.instructions.insertBefore(target, insnList));
    }

    public void insertBefore(InsnList... insnLists) {
        for(InsnList insnList: insnLists) insertBefore(insnList);
    }

    public void insertAfter(AbstractInsnNode abstractInsnNode) {
        nodes.forEach(target -> methodNode.instructions.insert(target, abstractInsnNode));
    }

    public void insertAfter(AbstractInsnNode... abstractInsnNodes) {
        for(AbstractInsnNode abstractInsnNode: abstractInsnNodes) insertAfter(abstractInsnNode);
    }

    public void insertAfter(InsnList insnList) {
        nodes.forEach(target -> methodNode.instructions.insert(target, insnList));
    }

    public void insertAfter(InsnList... insnLists) {
        for(InsnList insnList: insnLists) insertAfter(insnList);
    }

    public void replace(InsnList insnList) {
        nodes.forEach(target -> {
            methodNode.instructions.insert(target, insnList);
            methodNode.instructions.remove(target);
        });
    }

    public MethodNode getMethodNode() {
        return methodNode;
    }

    public Reference getTargetReference() {
        return reference;
    }
}
