package dev.tigr.asmp.modification.modifications;

import dev.tigr.asmp.ASMP;
import dev.tigr.asmp.annotations.Annotations;
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


/**
 * @author Tigermouthbear 8/1/20
 */
public class InjectModification extends Modification<Annotations.Inject> {
    public InjectModification(ASMP asmp, Annotations.Inject annotation) {
        super(asmp, annotation);
    }

    @Override
    public void invoke(String patchClassName, ClassNode classNode, MethodNode injection) {
        // add/copy injection to class
        injection.access = Opcodes.ACC_PRIVATE;
        injection.name = injection.name + "_asmp_injection" + classNode.methods.size(); // unique name
        classNode.methods.add(injection);

        // find method node
        MethodNode methodNode = NodeUtils.getMethod(classNode, unmapMethodReference(annotation.getMethod()));
        if(methodNode != null) {
            // general info about method node and injections
            String target = annotation.getAt().getValue();
            Type[] argumentTypes = Type.getArgumentTypes(methodNode.desc);
            Type returnType = Type.getReturnType(methodNode.desc);

            // make sure it has callback and they are correct
            Type[] methodParameters = Type.getArgumentTypes(injection.desc);
            boolean returnable = methodParameters[0].getClassName().equals(CallbackInfoReturnable.class.getName());
            boolean regular = methodParameters[0].getClassName().equals(CallbackInfo.class.getName());
            boolean isVoid = returnType == Type.VOID_TYPE;
            if(!(regular || returnable) && !(isVoid && target.equals("HEAD")))
                throw new ASMPMissingCallbackException(patchClassName, injection.name.replaceAll("_asmp_injection" + classNode.methods.size(), ""));

            // create inject node and find locations to inject
            MethodInsnNode injectNode = new MethodInsnNode(Opcodes.INVOKEVIRTUAL, classNode.name, injection.name, injection.desc);
            InsnModifier insnModifier = new InsnModifier(asmp, classNode, methodNode, annotation.getAt());

            // see if it has method arguments and compile load arguments insns
            InsnList argInsns = new InsnList();
            if(methodParameters.length == argumentTypes.length + 1) {
                for(int i = 1; i < methodParameters.length; i++)
                    argInsns.add(new VarInsnNode(argumentTypes[i-1].getOpcode(Opcodes.ILOAD), i));
            } else if(methodParameters.length != 1) {
                throw new ASMPBadArgumentsException(patchClassName, injection.name.replaceAll("_asmp_injection" + classNode.methods.size(), ""));
            }

            // generate bytecode inserts for loading callback method
            InsnList preList = new InsnList();
            InsnList postList = new InsnList();
            switch(target) {
                case "HEAD": {
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
                    break;
                }
                case "RETURN": {
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
                    break;
                }
                case "INVOKE": {
                    Type type = Type.getReturnType(insnModifier.getTargetReference().getDesc());
                    boolean isTargetVoid = insnModifier.getTargetReference().isVoid();
                    int storeOp = type.getOpcode(Opcodes.ISTORE);
                    int loadOp = type.getOpcode(Opcodes.ILOAD);
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
                    break;
                }
            }

            // insert bytecode inserts and inject node
            if(target.equals("INVOKE")) {
                insnModifier.insertAfter(postList);
                insnModifier.insertAfter(injectNode);
                insnModifier.insertAfter(preList);
            } else {
                insnModifier.insertBefore(preList);
                insnModifier.insertBefore(injectNode);
                insnModifier.insertBefore(postList);
            }
        } else throw new ASMPMethodNotFoundException(patchClassName, injection.name.replaceAll("_asmp_injection" + classNode.methods.size(), ""));
    }
}
