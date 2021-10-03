package dev.tigr.asmp.modification;

import dev.tigr.asmp.ASMP;
import dev.tigr.asmp.obfuscation.IObfuscationMapper;
import dev.tigr.asmp.util.NodeUtils;
import dev.tigr.asmp.util.Reference;
import org.objectweb.asm.tree.*;

/**
 * Used to create custom patch types and match annotations to them
 * @author Tigermouthbear 7/31/20
 */
public abstract class Modification<T> {
    /**
     * Stores the {@link ASMP} instance this modification is linked to
     */
    protected final ASMP asmp;

    /**
     * Stores the annotation for the patch method
     */
    protected final T annotation;

    public Modification(ASMP asmp, T annotation) {
        this.asmp = asmp;
        this.annotation = annotation;
    }

    /**
     * Called when the {@link ClassNode} is patched, all modifications are handled here
     * @param patchNode {@link ClassNode} of the patch class being run - owner of {@param methodNode}
     * @param classNode {@link ClassNode} being patched
     * @param methodNode the {@link MethodNode} containing the instructions for the patch
     * @param generatedObject an object generated with all methods that need to be run at transformation time (check {@link dev.tigr.asmp.modification.modifications.ModifyModification}
     */
    public void invoke(ClassNode patchNode, ClassNode classNode, MethodNode methodNode, Object generatedObject) {
        invoke(patchNode, classNode, methodNode);
    }

    /**
     * Called when the {@link ClassNode} is patched, all modifications are handled here
     * @param patchNode {@link ClassNode} of the patch class being run - owner of {@param methodNode}
     * @param classNode {@link ClassNode} being patched
     * @param methodNode the {@link MethodNode} containing the instructions for the patch
     */
    public void invoke(ClassNode patchNode, ClassNode classNode, MethodNode methodNode) {
    }


    /**
     * Unmaps name of the class using the current {@link dev.tigr.asmp.obfuscation.IObfuscationMapper}
     * @param clazz class name to unmap
     * @return unmapped/obfuscated name of the class
     */
    public String unmapClass(String clazz) {
        return asmp.getObfuscationMapper().unmapClass(clazz);
    }

    /**
     * Unmaps name of the field using the current {@link dev.tigr.asmp.obfuscation.IObfuscationMapper}
     * @param owner owner of field
     * @param name name of field
     * @return unmapped/obfuscated name of the field
     */
    public String unmapField(String owner, String name) {
        return asmp.getObfuscationMapper().unmapField(owner, name);
    }

    /**
     * Unmaps name of the method using the current {@link dev.tigr.asmp.obfuscation.IObfuscationMapper}
     * @param owner owner of method
     * @param name name of method
     * @param desc desc of method
     * @return unmapped/obfuscated name of the method
     */
    public String unmapMethod(String owner, String name, String desc) {
        return asmp.getObfuscationMapper().unmapMethod(owner, name, desc);
    }

    /**
     * Unmaps description of the method using the current {@link dev.tigr.asmp.obfuscation.IObfuscationMapper}
     * @param desc description to unmap
     * @return unmapped/obfuscated description
     */
    public String unmapDesc(String desc) {
        return asmp.getObfuscationMapper().unmapDesc(desc);
    }

    /**
     * Unmaps a field reference
     * @param descriptor field reference (owner + name + desc)
     * @return unmapped field reference
     */
    public Reference unmapFieldReference(String descriptor) {
        return asmp.getObfuscationMapper().unmapFieldReference(descriptor);
    }

    /**
     * Unmaps a method reference
     * @param descriptor method reference/descriptor (owner + name + desc)
     * @return unmapped method reference
     */
    public Reference unmapMethodReference(String descriptor) {
        return asmp.getObfuscationMapper().unmapMethodReference(descriptor);
    }

    /**
     * Replaces all shadowed fields insns to the correct owner
     * @param patchNode {@link ClassNode} which holds patch data
     * @param methodNode {@link MethodNode} which shadow fields are being fixed
     */
    public void shadowVariables(ClassNode patchNode, MethodNode methodNode) {
        for(AbstractInsnNode abstractInsnNode: methodNode.instructions) {
            if(abstractInsnNode instanceof FieldInsnNode) {
                FieldInsnNode fieldInsnNode = (FieldInsnNode) abstractInsnNode;
                if(fieldInsnNode.owner.equals(patchNode.name)) {
                    FieldNode fieldNode = NodeUtils.getField(patchNode, fieldInsnNode.name, fieldInsnNode.desc);
                    if(fieldNode == null) continue;
                    for(AnnotationNode annotationNode: fieldNode.visibleAnnotations) {
                        if(annotationNode.desc.equals("Ldev/tigr/asmp/annotations/Shadow;")) {
                            if(annotationNode.values.get(0).equals("value")) {
                                Reference reference = unmapFieldReference((String) annotationNode.values.get(1));
                                fieldInsnNode.owner = reference.getOwner();
                                fieldInsnNode.name = reference.getName();
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * @return obfuscation mapper used for this modification
     */
    protected IObfuscationMapper getObfuscationMapper() {
        return asmp.getObfuscationMapper();
    }
}
