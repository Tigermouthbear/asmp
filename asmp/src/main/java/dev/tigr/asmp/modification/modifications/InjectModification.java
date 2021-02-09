package dev.tigr.asmp.modification.modifications;

import dev.tigr.asmp.ASMP;
import dev.tigr.asmp.util.NodeUtils;
import dev.tigr.asmp.annotations.At;
import dev.tigr.asmp.annotations.modifications.Inject;
import dev.tigr.asmp.callback.CallbackInfo;
import dev.tigr.asmp.callback.CallbackInfoReturnable;
import dev.tigr.asmp.exceptions.ASMPBadArgumentsException;
import dev.tigr.asmp.exceptions.ASMPMethodNotFoundException;
import dev.tigr.asmp.exceptions.ASMPMissingCallbackException;
import dev.tigr.asmp.exceptions.ASMPMissingStaticModifierException;
import dev.tigr.asmp.modification.Modification;
import dev.tigr.asmp.util.Reference;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
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

            // see if it has method arguments
            Type[] types = Type.getArgumentTypes(methodNode.desc);
            InsnList argInsns = new InsnList();
            if(method.getParameterTypes().length == types.length + 1) {
                // make sure args are correct
                for(int i = 1; i < method.getParameterTypes().length; i++) {
                    argInsns.add(new VarInsnNode(types[i-1].getOpcode(Opcodes.ILOAD), i));

                    if(!unmapClass(method.getParameterTypes()[i].getName().replaceAll("\\.", "/")).equals(types[i-1].getClassName()))
                        throw new ASMPBadArgumentsException(patch.getClass().getName(), method.getName());
                }
            } else if(method.getParameterTypes().length != 1) {
                throw new ASMPBadArgumentsException(patch.getClass().getName(), method.getName());
            }

            // insert the list before the first node
            switch(annotation.at().value()) {
                case HEAD: {
                    // get type
                    Type type = Type.getReturnType(methodNode.desc);
                    int callbackId = methodNode.maxLocals;

                    // create pre list
                    InsnList preList = new InsnList();
                    InsnList postList = new InsnList();
                    if(returnable) {
                        preList.add(new TypeInsnNode(Opcodes.NEW, "dev/tigr/asmp/callback/CallbackInfoReturnable"));
                        preList.add(new InsnNode(Opcodes.DUP));
                        preList.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "dev/tigr/asmp/callback/CallbackInfoReturnable", "<init>", "()V", false));
                        preList.add(new VarInsnNode(Opcodes.ASTORE, callbackId));
                        preList.add(new VarInsnNode(Opcodes.ALOAD, callbackId));
                        preList.add(argInsns);

                        postList.add(new VarInsnNode(Opcodes.ALOAD, callbackId));
                        postList.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "dev/tigr/asmp/callback/CallbackInfoReturnable", "isCancelled", "()Z", false));
                        postList.add(new InsnNode(Opcodes.ICONST_1));
                        LabelNode l0 = new LabelNode();
                        postList.add(new JumpInsnNode(Opcodes.IF_ICMPNE, l0));
                        postList.add(new VarInsnNode(Opcodes.ALOAD, callbackId));
                        postList.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "dev/tigr/asmp/callback/CallbackInfoReturnable", "getValue", "()Ljava/lang/Object;", false));
                        postList.add(NodeUtils.castToNonPrimitive(type));
                        AbstractInsnNode primitiveValueNode = NodeUtils.primitiveValueInsnNode(type);
                        if(primitiveValueNode != null) postList.add(primitiveValueNode);
                        postList.add(new InsnNode(type.getOpcode(Opcodes.IRETURN)));
                        postList.add(l0);
                    } else if(regular) {
                        preList.add(new TypeInsnNode(Opcodes.NEW, "dev/tigr/asmp/callback/CallbackInfo"));
                        preList.add(new InsnNode(Opcodes.DUP));
                        preList.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "dev/tigr/asmp/callback/CallbackInfo", "<init>", "()V", false));
                        preList.add(new VarInsnNode(Opcodes.ASTORE, callbackId));
                        preList.add(new VarInsnNode(Opcodes.ALOAD, callbackId));
                        preList.add(argInsns);

                        postList.add(new VarInsnNode(Opcodes.ALOAD, callbackId));
                        postList.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "dev/tigr/asmp/callback/CallbackInfo", "isCancelled", "()Z", false));
                        postList.add(new InsnNode(Opcodes.ICONST_1));
                        LabelNode l0 = new LabelNode();
                        postList.add(new JumpInsnNode(Opcodes.IF_ICMPNE, l0));
                        postList.add(new InsnNode(Opcodes.RETURN));
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
                    int returnId = methodNode.maxLocals;
                    int callbackId = returnId + 1;

                    // create lists
                    InsnList preList = new InsnList();
                    InsnList postList = new InsnList();

                    if(returnable) {
                        preList.add(new VarInsnNode(storeOp, returnId));
                        preList.add(new TypeInsnNode(Opcodes.NEW, "dev/tigr/asmp/callback/CallbackInfoReturnable"));
                        preList.add(new InsnNode(Opcodes.DUP));
                        preList.add(new VarInsnNode(loadOp, returnId));
                        AbstractInsnNode valueOfNode = NodeUtils.valueOfInsnNode(type);
                        if(valueOfNode != null) preList.add(valueOfNode);
                        preList.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "dev/tigr/asmp/callback/CallbackInfoReturnable", "<init>", "(Ljava/lang/Object;)V", false));
                        preList.add(new VarInsnNode(Opcodes.ASTORE, callbackId));
                        preList.add(new VarInsnNode(Opcodes.ALOAD, callbackId));
                        preList.add(argInsns);

                        postList.add(new VarInsnNode(Opcodes.ALOAD, callbackId));
                        postList.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "dev/tigr/asmp/callback/CallbackInfoReturnable", "getValue", "()Ljava/lang/Object;", false));
                        postList.add(NodeUtils.castToNonPrimitive(type));
                        AbstractInsnNode primitiveValueNode = NodeUtils.primitiveValueInsnNode(type);
                        if(primitiveValueNode != null) postList.add(primitiveValueNode);
                    } else if(regular) {
                        preList.add(new TypeInsnNode(Opcodes.NEW, "dev/tigr/asmp/callback/CallbackInfo"));
                        preList.add(new InsnNode(Opcodes.DUP));
                        preList.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "dev/tigr/asmp/callback/CallbackInfo", "<init>", "()V", false));
                        preList.add(argInsns);
                    }

                    // insert callback and list before all returns
                    returns.forEach(abstractInsnNode -> {
                        methodNode.instructions.insertBefore(abstractInsnNode, preList);
                        methodNode.instructions.insertBefore(abstractInsnNode, injectNode);
                        methodNode.instructions.insertBefore(abstractInsnNode, postList);
                    });
                    break;
                }
                case INVOKE: {
                    Reference reference = unmapMethodReference(annotation.at().target());
                    int index = annotation.at().ordinal();
                    boolean isTargetVoid = reference.getDesc().endsWith("V");

                    List<AbstractInsnNode> insns = new ArrayList<>();
                    for(AbstractInsnNode abstractInsnNode: methodNode.instructions) {
                        if(abstractInsnNode instanceof MethodInsnNode) {
                            MethodInsnNode methodInsnNode = (MethodInsnNode) abstractInsnNode;
                            if(methodInsnNode.owner.equals(reference.getOwner()) && methodInsnNode.name.equals(reference.getName())
                                    && methodInsnNode.desc.equals(reference.getDesc()))
                                insns.add(abstractInsnNode);
                        }
                    }

                    // get opcodes and add local var index
                    Type type = Type.getReturnType(reference.getDesc());
                    int storeOp = type.getOpcode(Opcodes.ISTORE);
                    int loadOp = type.getOpcode(Opcodes.ILOAD);
                    int callbackId = methodNode.maxLocals;
                    int returnId = methodNode.maxLocals + 1;

                    // create lists
                    InsnList preList = new InsnList();
                    InsnList postList = new InsnList();

                    if(isVoid) {
                        if(isTargetVoid) {
                            preList.add(new TypeInsnNode(Opcodes.NEW, "dev/tigr/asmp/callback/CallbackInfo"));
                            preList.add(new InsnNode(Opcodes.DUP));
                            preList.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "dev/tigr/asmp/callback/CallbackInfo", "<init>", "()V", false));
                            preList.add(new VarInsnNode(Opcodes.ASTORE, callbackId));
                            preList.add(new VarInsnNode(Opcodes.ALOAD, callbackId));
                            preList.add(argInsns);

                            postList.add(new VarInsnNode(Opcodes.ALOAD, callbackId));
                            postList.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "dev/tigr/asmp/callback/CallbackInfo", "isCancelled", "()Z", false));
                            postList.add(new InsnNode(Opcodes.ICONST_1));
                            LabelNode l0 = new LabelNode();
                            postList.add(new JumpInsnNode(Opcodes.IF_ICMPNE, l0));
                            postList.add(new InsnNode(Opcodes.RETURN));
                            postList.add(l0);
                        } else {
                            preList.add(new VarInsnNode(storeOp, returnId));
                            preList.add(new TypeInsnNode(Opcodes.NEW, "dev/tigr/asmp/callback/CallbackInfoReturnable"));
                            preList.add(new InsnNode(Opcodes.DUP));
                            preList.add(new VarInsnNode(loadOp, returnId));
                            AbstractInsnNode valueOfNode = NodeUtils.valueOfInsnNode(type);
                            if(valueOfNode != null) preList.add(valueOfNode);
                            preList.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "dev/tigr/asmp/callback/CallbackInfoReturnable", "<init>", "(Ljava/lang/Object;)V", false));
                            preList.add(new VarInsnNode(Opcodes.ASTORE, callbackId));
                            preList.add(new VarInsnNode(Opcodes.ALOAD, callbackId));
                            preList.add(argInsns);

                            postList.add(new VarInsnNode(Opcodes.ALOAD, callbackId));
                            postList.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "dev/tigr/asmp/callback/CallbackInfoReturnable", "isCancelled", "()Z", false));
                            postList.add(new InsnNode(Opcodes.ICONST_1));
                            LabelNode l0 = new LabelNode();
                            postList.add(new JumpInsnNode(Opcodes.IF_ICMPNE, l0));
                            postList.add(new InsnNode(Opcodes.RETURN));
                            postList.add(l0);
                            postList.add(new VarInsnNode(Opcodes.ALOAD, callbackId));
                            postList.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "dev/tigr/asmp/callback/CallbackInfoReturnable", "getValue", "()Ljava/lang/Object;", false));
                            postList.add(NodeUtils.castToNonPrimitive(type));
                            AbstractInsnNode primitiveValueNode = NodeUtils.primitiveValueInsnNode(type);
                            if(primitiveValueNode != null) postList.add(primitiveValueNode);
                        }
                    } else {
                        if(isTargetVoid) {
                            Type returnType = Type.getReturnType(methodNode.desc);

                            preList.add(new TypeInsnNode(Opcodes.NEW, "dev/tigr/asmp/callback/CallbackInfoReturnable"));
                            preList.add(new InsnNode(Opcodes.DUP));
                            preList.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "dev/tigr/asmp/callback/CallbackInfoReturnable", "<init>", "()V", false));
                            preList.add(new VarInsnNode(Opcodes.ASTORE, callbackId));
                            preList.add(new VarInsnNode(Opcodes.ALOAD, callbackId));
                            preList.add(argInsns);

                            postList.add(new VarInsnNode(Opcodes.ALOAD, callbackId));
                            postList.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "dev/tigr/asmp/callback/CallbackInfoReturnable", "isCancelled", "()Z", false));
                            postList.add(new InsnNode(Opcodes.ICONST_1));
                            LabelNode l0 = new LabelNode();
                            postList.add(new JumpInsnNode(Opcodes.IF_ICMPNE, l0));
                            postList.add(new VarInsnNode(Opcodes.ALOAD, callbackId));
                            postList.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "dev/tigr/asmp/callback/CallbackInfoReturnable", "getValue", "()Ljava/lang/Object;", false));
                            postList.add(NodeUtils.castToNonPrimitive(returnType));
                            AbstractInsnNode primitiveValueNode = NodeUtils.primitiveValueInsnNode(returnType);
                            if(primitiveValueNode != null) postList.add(primitiveValueNode);
                            postList.add(new InsnNode(returnType.getOpcode(Opcodes.IRETURN)));
                            postList.add(l0);
                        } else {
                            preList.add(new VarInsnNode(storeOp, returnId));
                            preList.add(new TypeInsnNode(Opcodes.NEW, "dev/tigr/asmp/callback/CallbackInfoReturnable"));
                            preList.add(new InsnNode(Opcodes.DUP));
                            preList.add(new VarInsnNode(loadOp, returnId));
                            AbstractInsnNode valueOfNode = NodeUtils.valueOfInsnNode(type);
                            if(valueOfNode != null) preList.add(valueOfNode);
                            preList.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "dev/tigr/asmp/callback/CallbackInfoReturnable", "<init>", "(Ljava/lang/Object;)V", false));
                            preList.add(new VarInsnNode(Opcodes.ASTORE, callbackId));
                            preList.add(new VarInsnNode(Opcodes.ALOAD, callbackId));
                            preList.add(argInsns);

                            postList.add(new VarInsnNode(Opcodes.ALOAD, callbackId));
                            postList.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "dev/tigr/asmp/callback/CallbackInfoReturnable", "getValue", "()Ljava/lang/Object;", false));
                            postList.add(NodeUtils.castToNonPrimitive(type));
                            AbstractInsnNode primitiveValueNode = NodeUtils.primitiveValueInsnNode(type);
                            if(primitiveValueNode != null) postList.add(primitiveValueNode);
                        }
                    }

                    // insert callback and list before all returns or at ordinal
                    if(index == -1) {
                        insns.forEach(abstractInsnNode -> {
                            methodNode.instructions.insert(abstractInsnNode, postList);
                            methodNode.instructions.insert(abstractInsnNode, injectNode);
                            methodNode.instructions.insert(abstractInsnNode, preList);
                        });
                    } else {
                        AbstractInsnNode insn = insns.get(index);
                        methodNode.instructions.insert(insn, postList);
                        methodNode.instructions.insert(insn, injectNode);
                        methodNode.instructions.insert(insn, preList);
                    }

                    break;
                }
            }
        } else throw new ASMPMethodNotFoundException(patch.getClass().getName(), method.getName());
    }
}
