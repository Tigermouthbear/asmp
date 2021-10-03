package dev.tigr.asmp.modification.modifications;

import dev.tigr.asmp.ASMP;
import dev.tigr.asmp.annotations.Annotations;
import dev.tigr.asmp.exceptions.ASMPMethodNotFoundException;
import dev.tigr.asmp.modification.Modification;
import dev.tigr.asmp.util.InsnModifier;
import dev.tigr.asmp.util.NodeUtils;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

/**
 * @author Tigermouthbear 3/17/21
 */
public class RedirectModification extends Modification<Annotations.Redirect> {
    public RedirectModification(ASMP asmp, Annotations.Redirect annotation) {
        super(asmp, annotation);
    }

    @Override
    public void invoke(ClassNode patchNode, ClassNode classNode, MethodNode redirect) {
        // add/copy injection to class
        redirect.access = Opcodes.ACC_PRIVATE;
        redirect.name = redirect.name + "_asmp_redirect" + classNode.methods.size(); // unique name
        classNode.methods.add(redirect);
        shadowVariables(patchNode, redirect);

        // find method node
        MethodNode methodNode = NodeUtils.getMethod(classNode, unmapMethodReference(annotation.getMethod()));
        if(methodNode != null) {
            // replace with redirect
            MethodInsnNode redirectNode = new MethodInsnNode(Opcodes.INVOKEVIRTUAL, classNode.name, redirect.name, redirect.desc);
            InsnModifier insnModifier = new InsnModifier(asmp, classNode, methodNode, annotation.getAt());

            // insert prep bytecode before
            for(AbstractInsnNode abstractInsnNode: insnModifier.getTargets()) {
                int prev = methodNode.instructions.indexOf(abstractInsnNode) - 1;

                // store to stack, load 0, then load from stack
                if(prev >= 0) {
                    AbstractInsnNode prevNode = methodNode.instructions.get(prev);
                    if(prevNode != null) {
                        boolean store = true;
                        int id = methodNode.maxLocals + 1;
                        int opcode = Opcodes.ASTORE;
                        if(prevNode instanceof FieldInsnNode) {
                            opcode = Type.getType(((FieldInsnNode) prevNode).desc).getOpcode(Opcodes.ISTORE);
                        } else if(prevNode instanceof MethodInsnNode) {
                            opcode = Type.getReturnType(((MethodInsnNode) prevNode).desc).getOpcode(Opcodes.ISTORE);
                        } else if(prevNode instanceof VarInsnNode) {
                            if(NodeUtils.isLoad(prevNode)) {
                                opcode = NodeUtils.loadToStore(prevNode.getOpcode());
                            } else if(NodeUtils.isStore(prevNode)) {
                                store = false;
                                opcode = prevNode.getOpcode();
                                id = ((VarInsnNode) prevNode).var;
                            }
                        }

                        ASMP.LOGGER.info("OPCODE: " + opcode + " ID: " + id);

                        InsnList insnList = new InsnList();
                        if(store) insnList.add(new VarInsnNode(opcode, id));
                        insnList.add(new VarInsnNode(Opcodes.ALOAD, 0));
                        insnList.add(new VarInsnNode(NodeUtils.storeToLoad(opcode), id));
                        methodNode.instructions.insertBefore(abstractInsnNode, insnList);
                    }
                }
            }

            insnModifier.replace(redirectNode);
        }
        else throw new ASMPMethodNotFoundException(patchNode.name, redirect.name.replaceAll("_asmp_redirect" + classNode.methods.size(), ""));
    }
}
