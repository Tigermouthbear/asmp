package dev.tigr.asmp.obfuscation;

import dev.tigr.asmp.exceptions.ASMPBadTargetException;
import dev.tigr.asmp.util.Reference;

/**
 * Interface for creating custom obfuscation mappings
 * @author Tigermouthbear 8/1/20
 */
public interface IObfuscationMapper {
    String unmapClass(String name);
    String mapClass(String name);

    String unmapField(String owner, String name);
    String mapField(String owner, String name);

    String unmapMethod(String owner, String name, String desc);
    String mapMethod(String owner, String name, String desc);

    /**
     * unmaps field reference
     * @param descriptor field reference (L + owner + ; + name + : + type)
     * @return {@link Reference} of field
     */
    default Reference unmapFieldReference(String descriptor) {
        boolean valid = descriptor.contains("(") && descriptor.contains(";");
        if(!valid) throw new ASMPBadTargetException(descriptor);

        int index0 = descriptor.indexOf(";");
        String owner = descriptor.substring(1, index0);
        String name = descriptor.substring(index0);
        owner = unmapClass(owner);

        return new Reference(owner, unmapField(owner, name));
    }

    /**
     * maps field reference
     * @param descriptor field reference (L + owner + ; + name + : + type)
     * @return {@link Reference} of field
     */
    default Reference mapFieldReference(String descriptor) {
        boolean valid = descriptor.contains("(") && descriptor.contains(";");
        if(!valid) throw new ASMPBadTargetException(descriptor);

        int index0 = descriptor.indexOf(";");
        String owner = descriptor.substring(1, index0);
        String name = descriptor.substring(index0);
        owner = mapClass(owner);

        return new Reference(owner, mapField(owner, name));
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

        return new Reference(owner, name, desc);
    }

    /**
     * maps method reference
     * @param descriptor method reference (L + owner + ; + name + desc)
     * @return {@link Reference} of method
     */
    default Reference mapMethodReference(String descriptor) {
        boolean valid = descriptor.contains("(") && descriptor.contains(";");
        if(!valid) throw new ASMPBadTargetException(descriptor);

        int index0 = descriptor.indexOf(";");
        int index1 = descriptor.indexOf("(");
        String owner = descriptor.substring(1, index0);
        String name = descriptor.substring(index0 + 1, index1);
        String desc = descriptor.substring(index1);
        owner = mapClass(owner);
        desc = mapDesc(desc);

        // dont try to unmap initialization blocks
        if(!name.equals("<init>") && !name.equals("<clinit>"))
            name = mapMethod(owner, name, desc);

        return new Reference(owner, name, desc);
    }

    /**
     * unmaps classnames in a method description
     * @param desc mapped desc of method
     * @return unmapped desc of method (obf)
     */
    default String unmapDesc(String desc) {
        boolean looking = false;
        StringBuilder sb = new StringBuilder();
        StringBuilder curr = new StringBuilder();

        for(char c: desc.toCharArray()) {
            if(!looking) {
                if(c == 'L') looking = true;
                sb.append(c);
            } else {
                if(c == ';') {
                    sb.append(unmapClass(curr.toString()));
                    sb.append(";");
                    curr = new StringBuilder();
                    looking = false;
                } else curr.append(c);
            }
        }

        return sb.toString();
    }

    /**
     * maps classnames in a method description
     * @param desc mapped desc of method
     * @return unmapped desc of method (obf)
     */
    default String mapDesc(String desc) {
        boolean looking = false;
        StringBuilder sb = new StringBuilder();
        StringBuilder curr = new StringBuilder();

        for(char c: desc.toCharArray()) {
            if(!looking) {
                if(c == 'L') looking = true;
                sb.append(c);
            } else {
                if(c == ';') {
                    sb.append(mapClass(curr.toString()));
                    sb.append(";");
                    curr = new StringBuilder();
                    looking = false;
                } else curr.append(c);
            }
        }

        return sb.toString();
    }
}
