package dev.tigr.asmp.modification.modifications;

import dev.tigr.asmp.ASMP;
import dev.tigr.asmp.annotations.At;
import dev.tigr.asmp.annotations.modifications.Inject;
import dev.tigr.asmp.modification.Modification;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;

import java.lang.reflect.InvocationTargetException;
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
        At at = annotation.at();
        String name = unmapMethod(annotation.method());
        String desc = unmapDesc(annotation.desc());

        for(MethodNode methodNode: classNode.methods) {
            if(methodNode.name.equals(name) && (desc.isEmpty() || methodNode.desc.equals(desc))) {
                InsnList insnList = new InsnList(); // create insn list

                // pass list to method
                try {
                    method.invoke(patch, insnList);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }

                // insert the list before the first node
                switch(at.value()) {
                    case HEAD:
                        methodNode.instructions.insertBefore(methodNode.instructions.getFirst(), insnList);
                        break;
                    case RETURN:
                        methodNode.instructions.insertBefore(methodNode.instructions.getLast(), insnList);
                    case INVOKE:
                        ASMP.LOGGER.info("This feature hasn't been developed yet!");
                }
            }
        }
    }
}
