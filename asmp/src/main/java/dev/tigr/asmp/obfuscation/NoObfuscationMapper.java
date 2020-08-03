package dev.tigr.asmp.obfuscation;

/**
 * Default ObfuscationMapper which is not connected to any mappings
 * @author Tigermouthbear 8/1/20
 */
public class NoObfuscationMapper implements IObfuscationMapper {
    @Override
    public String unmapClass(String clazz) {
        return clazz;
    }

    @Override
    public String unmapField(String field) {
        return field;
    }

    @Override
    public String unmapMethod(String method) {
        return method;
    }
}
