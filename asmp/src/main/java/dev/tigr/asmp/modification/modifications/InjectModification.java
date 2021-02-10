package dev.tigr.asmp.modification.modifications;

import dev.tigr.asmp.ASMP;
import dev.tigr.asmp.annotations.At;
import dev.tigr.asmp.annotations.modifications.Inject;
import dev.tigr.asmp.callback.CallbackInfo;
import dev.tigr.asmp.callback.CallbackInfoReturnable;
import dev.tigr.asmp.exceptions.ASMPBadArgumentsException;
import dev.tigr.asmp.exceptions.ASMPMethodNotFoundException;
import dev.tigr.asmp.exceptions.ASMPMissingCallbackException;
import dev.tigr.asmp.modification.Modification;
import dev.tigr.asmp.util.InsnModifier;
import dev.tigr.asmp.util.NodeUtils;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.lang.reflect.Method;

/**
 * @author Tigermouthbear 8/1/20
 */
public class InjectModification extends Modification<Inject> {
    public InjectModification(ASMP asmp, Inject annotation) {
        super(asmp, annotation);
    }

    @Override
    public void invoke(ClassNode classNode, Object patch, Method method) {
        // add/copy injection to class
        MethodNode injection = NodeUtils.readMethodNode(patch, method);
        injection.access = Opcodes.ACC_PRIVATE;
        injection.name = injection.name + "_asmp_injection" + classNode.methods.size(); // unique name
        classNode.methods.add(injection);

        // find method node
        MethodNode methodNode = NodeUtils.getMethod(classNode, unmapMethodReference(annotation.method()));
        if(methodNode != null) {
            // general info about method node and injections
            At.Target target = annotation.at().value();
            Type[] argumentTypes = Type.getArgumentTypes(methodNode.desc);
            Type returnType = Type.getReturnType(methodNode.desc);

            // make sure it has callback and they are correct
            boolean returnable = method.getParameterTypes()[0] == CallbackInfoReturnable.class;
            boolean regular = method.getParameterTypes()[0] == CallbackInfo.class;
            boolean isVoid = returnType == Type.VOID_TYPE;
            if(!(regular || returnable) && !(isVoid && target == At.Target.HEAD))
                throw new ASMPMissingCallbackException(patch.getClass().getName(), method.getName());

            // create inject node and find locations to inject
            MethodInsnNode injectNode = new MethodInsnNode(Opcodes.INVOKEVIRTUAL, classNode.name, injection.name, injection.desc);
            InsnModifier insnModifier = new InsnModifier(this, methodNode, annotation.at());

            // see if it has method arguments and compile load arguments insns
            InsnList argInsns = new InsnList();
            if(method.getParameterTypes().length == argumentTypes.length + 1) {
                // make sure args are correct
                for(int i = 1; i < method.getParameterTypes().length; i++) {
                    argInsns.add(new VarInsnNode(argumentTypes[i-1].getOpcode(Opcodes.ILOAD), i));

                    if(!unmapClass(method.getParameterTypes()[i].getName().replaceAll("\\.", "/")).equals(argumentTypes[i-1].getClassName()))
                        throw new ASMPBadArgumentsException(patch.getClass().getName(), method.getName());
                }
            } else if(method.getParameterTypes().length != 1) {
                throw new ASMPBadArgumentsException(patch.getClass().getName(), method.getName());
            }

            // generate bytecode inserts for loading callback method
            InsnList preList = new InsnList();
            InsnList postList = new InsnList();
            if(target == At.Target.HEAD) {
                int callbackId = methodNode.maxLocals;

                if(returnable) {
                    preList.add(new TypeInsnNode(Opcodes.NEW, "dev/tigr/asmp/callback/CallbackInfoReturnable"));
                    preList.add(new InsnNode(Opcodes.DUP));
                    preList.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "dev/tigr/asmp/callback/CallbackInfoReturnable", "<init>", "()V", false));
                    preList.add(new VarInsnNode(Opcodes.ASTORE, callbackId));
                    preList.add(new VarInsnNode(Opcodes.ALOAD, 0));
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
                } else if(regular) {
                    preList.add(new TypeInsnNode(Opcodes.NEW, "dev/tigr/asmp/callback/CallbackInfo"));
                    preList.add(new InsnNode(Opcodes.DUP));
                    preList.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "dev/tigr/asmp/callback/CallbackInfo", "<init>", "()V", false));
                    preList.add(new VarInsnNode(Opcodes.ASTORE, callbackId));
                    preList.add(new VarInsnNode(Opcodes.ALOAD, 0));
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
            } else if(target == At.Target.RETURN) {
                int storeOp = returnType.getOpcode(Opcodes.ISTORE);
                int loadOp = returnType.getOpcode(Opcodes.ILOAD);

                if(returnable) {
                    int returnId = methodNode.maxLocals;
                    int callbackId = returnId + 1;

                    preList.add(new VarInsnNode(storeOp, returnId));
                    preList.add(new TypeInsnNode(Opcodes.NEW, "dev/tigr/asmp/callback/CallbackInfoReturnable"));
                    preList.add(new InsnNode(Opcodes.DUP));
                    preList.add(new VarInsnNode(loadOp, returnId));
                    AbstractInsnNode valueOfNode = NodeUtils.valueOfInsnNode(returnType);
                    if(valueOfNode != null) preList.add(valueOfNode);
                    preList.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "dev/tigr/asmp/callback/CallbackInfoReturnable", "<init>", "(Ljava/lang/Object;)V", false));
                    preList.add(new VarInsnNode(Opcodes.ASTORE, callbackId));
                    preList.add(new VarInsnNode(Opcodes.ALOAD, 0));
                    preList.add(new VarInsnNode(Opcodes.ALOAD, callbackId));
                    preList.add(argInsns);

                    postList.add(new VarInsnNode(Opcodes.ALOAD, callbackId));
                    postList.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "dev/tigr/asmp/callback/CallbackInfoReturnable", "getValue", "()Ljava/lang/Object;", false));
                    postList.add(NodeUtils.castToNonPrimitive(returnType));
                    AbstractInsnNode primitiveValueNode = NodeUtils.primitiveValueInsnNode(returnType);
                    if(primitiveValueNode != null) postList.add(primitiveValueNode);
                } else {
                    int callbackId = methodNode.maxLocals;

                    preList.add(new TypeInsnNode(Opcodes.NEW, "dev/tigr/asmp/callback/CallbackInfo"));
                    preList.add(new InsnNode(Opcodes.DUP));
                    preList.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "dev/tigr/asmp/callback/CallbackInfo", "<init>", "()V", false));
                    preList.add(new VarInsnNode(Opcodes.ASTORE, callbackId));
                    preList.add(new VarInsnNode(Opcodes.ALOAD, 0));
                    preList.add(new VarInsnNode(Opcodes.ALOAD, callbackId));
                    preList.add(argInsns);
                }
            } else if(target == At.Target.INVOKE) {
                Type type = Type.getReturnType(insnModifier.getTargetReference().getDesc());
                boolean isTargetVoid = insnModifier.getTargetReference().isVoid();
                int storeOp = returnType.getOpcode(Opcodes.ISTORE);
                int loadOp = returnType.getOpcode(Opcodes.ILOAD);
                int callbackId = methodNode.maxLocals;
                int returnId = methodNode.maxLocals + 1;

                if(isVoid) {
                    if(isTargetVoid) {
                        preList.add(new TypeInsnNode(Opcodes.NEW, "dev/tigr/asmp/callback/CallbackInfo"));
                        preList.add(new InsnNode(Opcodes.DUP));
                        preList.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "dev/tigr/asmp/callback/CallbackInfo", "<init>", "()V", false));
                        preList.add(new VarInsnNode(Opcodes.ASTORE, callbackId));
                        preList.add(new VarInsnNode(Opcodes.ALOAD, 0));
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
                        preList.add(new VarInsnNode(Opcodes.ALOAD, 0));
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
                        preList.add(new TypeInsnNode(Opcodes.NEW, "dev/tigr/asmp/callback/CallbackInfoReturnable"));
                        preList.add(new InsnNode(Opcodes.DUP));
                        preList.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "dev/tigr/asmp/callback/CallbackInfoReturnable", "<init>", "()V", false));
                        preList.add(new VarInsnNode(Opcodes.ASTORE, callbackId));
                        preList.add(new VarInsnNode(Opcodes.ALOAD, 0));
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
                        preList.add(new VarInsnNode(Opcodes.ALOAD, 0));
                        preList.add(new VarInsnNode(Opcodes.ALOAD, callbackId));
                        preList.add(argInsns);

                        postList.add(new VarInsnNode(Opcodes.ALOAD, callbackId));
                        postList.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "dev/tigr/asmp/callback/CallbackInfoReturnable", "getValue", "()Ljava/lang/Object;", false));
                        postList.add(NodeUtils.castToNonPrimitive(type));
                        AbstractInsnNode primitiveValueNode = NodeUtils.primitiveValueInsnNode(type);
                        if(primitiveValueNode != null) postList.add(primitiveValueNode);
                    }
                }
            }

            // insert bytecode inserts and inject node
            if(target == At.Target.INVOKE) {
                insnModifier.insertAfter(postList);
                insnModifier.insertAfter(injectNode);
                insnModifier.insertAfter(preList);
            } else {
                insnModifier.insertBefore(preList);
                insnModifier.insertBefore(injectNode);
                insnModifier.insertBefore(postList);
            }
        } else throw new ASMPMethodNotFoundException(patch.getClass().getName(), method.getName());
    }
}
