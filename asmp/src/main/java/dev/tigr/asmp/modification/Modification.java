package dev.tigr.asmp.modification;

import dev.tigr.asmp.ASMP;
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
    protected String unmapClass(String clazz) {
        return asmp.getObfuscationMapper().unmapClass(clazz);
    }

    /**
     * Unmaps name of the field using the current {@link dev.tigr.asmp.obfuscation.IObfuscationMapper}
     * @param field field name to unmap
     * @return unmapped/obfuscated name of the field
     */
    protected String unmapField(String field) {
        return asmp.getObfuscationMapper().unmapField(field);
    }

    /**
     * Unmaps name of the method using the current {@link dev.tigr.asmp.obfuscation.IObfuscationMapper}
     * @param method method name to unmap
     * @return unmapped/obfuscated name of the method
     */
    protected String unmapMethod(String method) {
        return asmp.getObfuscationMapper().unmapMethod(method);
    }

    /**
     * Unmaps description of the method using the current {@link dev.tigr.asmp.obfuscation.IObfuscationMapper}
     * @param desc description to unmap
     * @return unmapped/obfuscated description
     */
    protected String unmapDesc(String desc) {
        return asmp.getObfuscationMapper().unmapDesc(desc);
    }

    /**
     * Unmaps a method reference (name + desc)
     * @param descriptor method reference/descriptor (name + desc)
     * @return unmapped method reference
     */
    protected String unmapMethodReference(String descriptor) {
        return asmp.getObfuscationMapper().unmapMethodReference(descriptor);
    }
}
