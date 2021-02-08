package dev.tigr.asmp.impl.forge;

import dev.tigr.asmp.obfuscation.IObfuscationMapper;
import net.minecraftforge.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Tigermouthbear 8/1/20
 */
public class ForgeObfuscationMapper implements IObfuscationMapper {
    public static ForgeObfuscationMapper INSTANCE = new ForgeObfuscationMapper();

    private final Map<String, String> fieldMappings;
    private final Map<String, String> methodMappings;

    public ForgeObfuscationMapper() {
        fieldMappings = read(getClass().getResourceAsStream("/fields.csv"));
        methodMappings = read(getClass().getResourceAsStream("/methods.csv"));
    }

    private Map<String, String> read(InputStream inputStream) {
        Map<String, String> map = new HashMap<>();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

        bufferedReader.lines().forEach(line -> {
            String[] args = line.split(",");
            map.put(args[1], args[0]);
        });

        return map;
    }

    @Override
    public String unmapClass(String clazz) {
        return FMLDeobfuscatingRemapper.INSTANCE.unmap(clazz);
    }

    @Override
    public String unmapField(String field) {
        return unmap(fieldMappings, field);
    }

    @Override
    public String unmapMethod(String method) {
        return unmap(methodMappings, method);
    }

    private String unmap(Map<String, String> mappings, String input) {
        String unmapped = mappings.getOrDefault(input, input);
        if(!unmapped.equals(input)) unmapped = unmapped.split("_")[2];
        return unmapped;
    }
}
