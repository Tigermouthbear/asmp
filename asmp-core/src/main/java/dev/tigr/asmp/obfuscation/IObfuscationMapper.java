package dev.tigr.asmp.obfuscation;

import dev.tigr.asmp.exceptions.ASMPBadTargetException;
import dev.tigr.asmp.util.Reference;

/**
 * Interface for creating custom obfuscation mappings
 * @author Tigermouthbear 8/1/20
 */
public interface IObfuscationMapper {
    String unmapClass(String name);

    String unmapField(String owner, String name, String type);

    String unmapMethod(String owner, String name, String desc);

    /**
     * unmaps field reference
     * @param descriptor field reference (L + owner + ; + name + : + type)
     * @return {@link Reference} of field
     */
    default Reference unmapFieldReference(String descriptor) {
        boolean valid = descriptor.contains("(") && descriptor.contains(";");
        if(!valid) throw new ASMPBadTargetException(descriptor);

        int index0 = descriptor.indexOf(";");
        int index1 = descriptor.indexOf(":");
        String owner = descriptor.substring(1, index0);
        String name = descriptor.substring(index0 + 1, index1);
        String type = descriptor.substring(index1 + 1);
        owner = unmapClass(owner);
        type = type.contains(";") ? unmapClass(type) : type;

        return new Reference(owner, unmapField(owner, name, type), type, true);
    }

    /**
     * unmaps method reference
     * @param descriptor method reference (L + owner + ; + name + desc)
     * @return {@link Reference} of method
     */
    default Reference unmapMethodReference(String descriptor) {
        boolean valid = descriptor.contains("(") && descriptor.contains(";");
        if(!valid) throw new ASMPBadTargetException(descriptor);

        int index0 = descriptor.indexOf(";");
        int index1 = descriptor.indexOf("(");
        String owner = descriptor.substring(1, index0);
        String name = descriptor.substring(index0 + 1, index1);
        String desc = descriptor.substring(index1);
        owner = unmapClass(owner);
        desc = unmapDesc(desc);

        // dont try to unmap initialization blocks
        if(!name.equals("<init>") && !name.equals("<clinit>"))
            name = unmapMethod(owner, name, desc);

        return new Reference(owner, name, desc, false);
    }

    /**
     * unmaps classnames in a method description
     * @param desc mapped desc of method
     * @return unmapped desc of method (obf)
     */
    default String unmapDesc(String desc) {
        boolean looking = false;
        StringBuilder curr = new StringBuilder();

        for(char c: desc.toCharArray()) {
            if(!looking) {
                if(c == 'L') looking = true;
            } else {
                if(c == ';') {
                    desc = desc.replace(curr, unmapClass(curr.toString()));
                    curr = new StringBuilder();
                    looking = false;
                } else curr.append(c);
            }
        }

        return desc;
    }
}
