package dev.tigr.asmp.modification.modifications;

import dev.tigr.asmp.ASMP;
import dev.tigr.asmp.NodeUtils;
import dev.tigr.asmp.annotations.At;
import dev.tigr.asmp.annotations.modifications.Inject;
import dev.tigr.asmp.callback.CallbackInfo;
import dev.tigr.asmp.callback.CallbackInfoReturnable;
import dev.tigr.asmp.exceptions.ASMPMissingCallbackException;
import dev.tigr.asmp.exceptions.ASMPMissingStaticModifierException;
import dev.tigr.asmp.modification.Modification;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.LocalVariablesSorter;
import org.objectweb.asm.tree.*;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Tigermouthbear 8/1/20
 */
public class InjectModification extends Modification<Inject> {
    public InjectModification(ASMP asmp, Inject annotation) {
        super(asmp, annotation);
    }

    @Override
    public void invoke(ClassNode classNode, Object patch, Method method) {
        if(!Modifier.isStatic(method.getModifiers())) throw new ASMPMissingStaticModifierException(patch.getClass().getName(), method.getName());
        MethodNode methodNode = NodeUtils.getMethod(classNode, unmapMethodReference(annotation.method()));
        if(methodNode != null) {
            MethodInsnNode injectNode = new MethodInsnNode(Opcodes.INVOKESTATIC, patch.getClass().getName().replaceAll("\\.", "/"), method.getName(), NodeUtils.getDescriptor(method));

            // make sure it has callback
            boolean returnable = method.getParameterTypes()[0] == CallbackInfoReturnable.class;
            boolean regular = method.getParameterTypes()[0] == CallbackInfo.class;
            boolean isVoid = methodNode.desc.endsWith("V");
            if(!(regular || returnable) && !(isVoid && annotation.at().value() == At.Target.HEAD))
                throw new ASMPMissingCallbackException(patch.getClass().getName(), method.getName());

            // insert the list before the first node
            switch(annotation.at().value()) {
                case HEAD: {
                    // get opcodes and add local var index
                    Type type = Type.getReturnType(methodNode.desc);
                    LocalVariablesSorter localVariablesSorter = new LocalVariablesSorter(methodNode.access, methodNode.desc, methodNode);
                    int callbackId = localVariablesSorter.newLocal(Type.getType(returnable ? CallbackInfoReturnable.class : CallbackInfo.class));

                    // create pre list
                    InsnList preList = new InsnList();
                    InsnList postList = new InsnList();
                    if(returnable) {
                        preList.add(new TypeInsnNode(Opcodes.NEW, "dev/tigr/asmp/callback/CallbackInfoReturnable"));
                        preList.add(new InsnNode(Opcodes.DUP));
                        preList.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "dev/tigr/asmp/callback/CallbackInfoReturnable", "<init>", "()V", false));
                        preList.add(new VarInsnNode(Opcodes.ASTORE, callbackId));
                        preList.add(new VarInsnNode(Opcodes.ALOAD, callbackId));

                        postList.add(new VarInsnNode(Opcodes.ALOAD, callbackId));
                        postList.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "dev/tigr/asmp/callback/CallbackInfoReturnable", "isCancelled", "()Z", false));
                        postList.add(new InsnNode(Opcodes.ICONST_1));
                        LabelNode l0 = new LabelNode();
                        postList.add(new JumpInsnNode(Opcodes.IF_ICMPNE, l0));
                        postList.add(new VarInsnNode(Opcodes.ALOAD, callbackId));
                        postList.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "dev/tigr/asmp/callback/CallbackInfoReturnable", "getValue", "()Ljava/lang/Object;", false));
                        postList.add(new TypeInsnNode(Opcodes.CHECKCAST, NodeUtils.getNonPrimitiveClass(type)));
                        postList.add(NodeUtils.primitiveValueInsnNode(type));
                        postList.add(new InsnNode(type.getOpcode(Opcodes.IRETURN)));
                        postList.add(l0);
                    } else if(regular) {
                        preList.add(new TypeInsnNode(Opcodes.NEW, "dev/tigr/asmp/callback/CallbackInfo"));
                        preList.add(new InsnNode(Opcodes.DUP));
                        preList.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "dev/tigr/asmp/callback/CallbackInfo", "<init>", "()V", false));
                        preList.add(new VarInsnNode(Opcodes.ASTORE, callbackId));
                        preList.add(new VarInsnNode(Opcodes.ALOAD, callbackId));

                        postList.add(new VarInsnNode(Opcodes.ALOAD, callbackId));
                        postList.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "dev/tigr/asmp/callback/CallbackInfo", "isCancelled", "()Z", false));
                        postList.add(new InsnNode(Opcodes.ICONST_1));
                        LabelNode l0 = new LabelNode();
                        postList.add(new JumpInsnNode(Opcodes.IF_ICMPNE, l0));
                        postList.add(new InsnNode(type.getOpcode(Opcodes.RETURN)));
                        postList.add(l0);
                    }

                    // insert callback and list at head
                    AbstractInsnNode first = methodNode.instructions.getFirst();
                    methodNode.instructions.insertBefore(first, preList);
                    methodNode.instructions.insertBefore(first, injectNode);
                    methodNode.instructions.insertBefore(first, postList);
                    break;
                }
                case RETURN: {
                    // find returns
                    List<AbstractInsnNode> returns = new ArrayList<>();
                    for(AbstractInsnNode abstractInsnNode: methodNode.instructions) {
                        if(NodeUtils.isReturn(abstractInsnNode)) returns.add(abstractInsnNode);
                    }

                    // get opcodes and add local var index
                    Type type = Type.getReturnType(methodNode.desc);
                    int storeOp = type.getOpcode(Opcodes.ISTORE);
                    int loadOp = type.getOpcode(Opcodes.ILOAD);
                    LocalVariablesSorter localVariablesSorter = new LocalVariablesSorter(methodNode.access, methodNode.desc, methodNode);
                    int returnId = localVariablesSorter.newLocal(type);
                    int callbackId = localVariablesSorter.newLocal(Type.getType(CallbackInfoReturnable.class));

                    // create lists
                    InsnList preList = new InsnList();
                    InsnList postList = new InsnList();

                    preList.add(new VarInsnNode(storeOp, returnId));
                    preList.add(new TypeInsnNode(Opcodes.NEW, "dev/tigr/asmp/callback/CallbackInfoReturnable"));
                    preList.add(new InsnNode(Opcodes.DUP));
                    preList.add(new VarInsnNode(loadOp, returnId));
                    preList.add(NodeUtils.valueOfInsnNode(type));
                    preList.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "dev/tigr/asmp/callback/CallbackInfoReturnable", "<init>", "(Ljava/lang/Object;)V", false));
                    preList.add(new VarInsnNode(Opcodes.ASTORE, callbackId));
                    preList.add(new VarInsnNode(Opcodes.ALOAD, callbackId));

                    postList.add(new VarInsnNode(Opcodes.ALOAD, callbackId));
                    postList.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "dev/tigr/asmp/callback/CallbackInfoReturnable", "getValue", "()Ljava/lang/Object;", false));
                    postList.add(new TypeInsnNode(Opcodes.CHECKCAST, NodeUtils.getNonPrimitiveClass(type)));
                    postList.add(NodeUtils.primitiveValueInsnNode(type));

                    // insert callback and list before all returns
                    returns.forEach(abstractInsnNode -> {
                        methodNode.instructions.insertBefore(abstractInsnNode, preList);
                        methodNode.instructions.insertBefore(abstractInsnNode, injectNode);
                        methodNode.instructions.insertBefore(abstractInsnNode, postList);
                    });
                    break;
                }
                case INVOKE:
                    throw new IllegalArgumentException("This feature hasn't been developed yet!");
            }
        }
    }
}
