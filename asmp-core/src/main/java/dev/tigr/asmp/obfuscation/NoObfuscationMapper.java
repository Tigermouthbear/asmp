package dev.tigr.asmp.obfuscation;

/**
 * Default ObfuscationMapper which is not connected to any mappings
 * @author Tigermouthbear 8/1/20
 */
public class NoObfuscationMapper implements IObfuscationMapper {
    @Override
    public String unmapClass(String name) {
        return name;
    }

    @Override
    public String unmapField(String owner, String name) {
        return name;
    }

    @Override
    public String unmapMethod(String owner, String name, String desc) {
        return name;
    }
}
