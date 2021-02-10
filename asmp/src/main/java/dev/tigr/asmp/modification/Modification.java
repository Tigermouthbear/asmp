package dev.tigr.asmp.modification;

import dev.tigr.asmp.ASMP;
import dev.tigr.asmp.obfuscation.IObfuscationMapper;
import dev.tigr.asmp.util.Reference;
import org.objectweb.asm.tree.ClassNode;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * Used to create custom patch types and match annotations to them
 * @param <T> Annotation which holds a {@link Modificate} annotation and corresponds to this modification
 * @author Tigermouthbear 7/31/20
 */
public abstract class Modification<T extends Annotation> {
    /**
     * Stores the {@link ASMP} instance this modification is linked to
     */
    private final ASMP asmp;

    /**
     * Stores the annotation for the patch method
     */
    protected final T annotation;

    public Modification(ASMP asmp, T annotation) {
        this.asmp = asmp;
        this.annotation = annotation;
    }

    /**
     * Called when the {@link ClassNode} is patched, the corresponding method should be invoked in this method
     * @param classNode {@link ClassNode} being patched
     * @param patch the object which holds the patching method
     * @param method the method that has the annotation and should be invoked during this method
     */
    public abstract void invoke(ClassNode classNode, Object patch, Method method);

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
     * @param type type of field
     * @return unmapped/obfuscated name of the field
     */
    public String unmapField(String owner, String name, String type) {
        return asmp.getObfuscationMapper().unmapField(owner, name, type);
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
     * @return obfuscation mapper used for this modification
     */
    protected IObfuscationMapper getObfuscationMapper() {
        return asmp.getObfuscationMapper();
    }
}
