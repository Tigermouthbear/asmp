package dev.tigr.asmp.obfuscation;

/**
 * Interface for creating custom obfuscation mappings
 * @author Tigermouthbear 8/1/20
 */
public interface IObfuscationMapper {
    String unmapClass(String clazz);

    String unmapField(String field);

    String unmapMethod(String method);

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
