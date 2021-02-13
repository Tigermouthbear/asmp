package dev.tigr.asmp.obfuscation;

import dev.tigr.asmp.exceptions.ASMPBadTargetException;
import dev.tigr.asmp.util.Reference;

import java.io.File;
import java.io.Reader;

/**
 * reads an .srg file and provides an interface for deobfuscating with it's mappings
 * @author Tigermouthbear 2/11/21
 */
public class SrgObfuscationMapper implements IObfuscationMapper {
    private final SrgMapper srgMapper = new SrgMapper();

    public void read(Reader reader) {
        srgMapper.read(reader);
    }

    public void read(File file) {
        srgMapper.read(file);
    }

    public void read(SrgMapper srgMapper) {
        this.srgMapper.read(srgMapper);
    }

    @Override
    public String unmapClass(String name) {
        return srgMapper.getClassMap().getObf(name);
    }

    @Override
    public String unmapField(String owner, String name) {
        return unmapFieldReference("L" + owner + ";" + name).getName();
    }

    @Override
    public String unmapMethod(String owner, String name, String desc) {
        return unmapMethodReference("L" + owner + ";" + name + desc).getName();
    }

    @Override
    public Reference unmapFieldReference(String descriptor) {
        boolean valid = descriptor.contains(";");
        if(!valid) throw new ASMPBadTargetException(descriptor);

        descriptor = srgMapper.getFieldMap().getObf(descriptor);

        int index0 = descriptor.indexOf(";");
        String owner = descriptor.substring(1, index0);
        String name = descriptor.substring(index0 + 1);

        return new Reference(owner, name);
    }

    @Override
    public Reference unmapMethodReference(String descriptor) {
        boolean initializer = descriptor.contains("<init>") || descriptor.contains("<clinit>");
        boolean valid = descriptor.contains("(") && descriptor.contains(";");
        if(!valid) throw new ASMPBadTargetException(descriptor);

        if(!initializer) descriptor = srgMapper.getMethodMap().getObf(descriptor);

        int index0 = descriptor.indexOf(";");
        int index1 = descriptor.indexOf("(");
        String owner = descriptor.substring(1, index0);
        String name = descriptor.substring(index0 + 1, index1);
        String desc = descriptor.substring(index1);

        if(initializer) owner = unmapClass(owner);

        return new Reference(owner, name, desc);
    }
}
