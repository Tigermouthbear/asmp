package dev.tigr.asmp.api.modification;

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
     * Stores the annotation for the patch method
     */
    protected final T annotation;

    public Modification(T annotation) {
        this.annotation = annotation;
    }

    /**
     * Called when the {@link ClassNode} is patched, the corresponding method should be invoked in this method
     * @param classNode {@link ClassNode} being patched
     * @param patch the object which holds the patching method
     * @param method the method that has the annotation and should be invoked during this method
     */
    public abstract void invoke(ClassNode classNode, Object patch, Method method);
}
