package dev.tigr.asmp.impl.forge;

import dev.tigr.asmp.obfuscation.IObfuscationMapper;
import net.minecraftforge.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Tigermouthbear 8/1/20
 */
public class ForgeObfuscationMapper implements IObfuscationMapper {
    public static ForgeObfuscationMapper INSTANCE = new ForgeObfuscationMapper();
    private static final Method FIELD_MAP_METHOD;
    private static final Method METHOD_MAP_METHOD;
    static {
        Method field = null;
        Method method = null;
        try {
            field = FMLDeobfuscatingRemapper.class.getDeclaredMethod("getFieldMap", String.class, boolean.class);
            method = FMLDeobfuscatingRemapper.class.getDeclaredMethod("getMethodMap", String.class);
        } catch(NoSuchMethodException e) {
            e.printStackTrace();
        }
        FIELD_MAP_METHOD = field;
        METHOD_MAP_METHOD = method;

        FIELD_MAP_METHOD.setAccessible(true);
        METHOD_MAP_METHOD.setAccessible(true);
    }

    private final Map<String, List<String>> fieldMappings;
    private final Map<String, List<String>> methodMappings;

    public ForgeObfuscationMapper() {
        fieldMappings = read(getClass().getResourceAsStream("/fields.csv"));
        methodMappings = read(getClass().getResourceAsStream("/methods.csv"));
    }

    private Map<String, List<String>> read(InputStream inputStream) {
        Map<String, List<String>> map = new HashMap<>();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

        bufferedReader.lines().forEach(line -> {
            String[] args = line.split(",");
            map.computeIfAbsent(args[1], k -> new ArrayList<>()).add(args[0]);
        });

        return map;
    }

    @Override
    public String unmapClass(String clazz) {
        return FMLDeobfuscatingRemapper.INSTANCE.unmap(clazz);
    }

    @Override
    public String unmapField(String owner, String name) {
        if(!fieldMappings.containsKey(name)) return null;

        // loop over searge and notch to see what matches
        // kind of a hack but works good
        for(Map.Entry<String, String> entry: getFMLFieldMappings(owner).entrySet()) {
            for(String searge: fieldMappings.get(name)) {
                if(searge.equals(entry.getValue())) {
                    String descriptor = entry.getKey();
                    if(descriptor.contains(":")) return entry.getKey().substring(0, descriptor.indexOf(":"));
                }
            }
        }

        return null;
    }

    @Override
    public String unmapMethod(String owner, String name, String desc) {
        if(!methodMappings.containsKey(name)) return null;

        // loop over searge and notch to see what matches
        // kind of a hack but works good
        for(Map.Entry<String, String> entry: getFMLMethodMappings(owner).entrySet()) {
            for(String searge: methodMappings.get(name)) {
                if(searge.equals(entry.getValue())) {
                    String descriptor = entry.getKey();
                    if(descriptor.contains("(")) {
                        int index = descriptor.indexOf("(");
                        if(descriptor.substring(index).equals(desc))
                            return entry.getKey().substring(0, index);
                    }
                }
            }
        }

        return null;
    }

    private Map<String, String> getFMLFieldMappings(String owner) {
        try {
            return (Map<String, String>) FIELD_MAP_METHOD.invoke(FMLDeobfuscatingRemapper.INSTANCE, owner, false);
        } catch(IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private Map<String, String> getFMLMethodMappings(String owner) {
        try {
            return (Map<String, String>) METHOD_MAP_METHOD.invoke(FMLDeobfuscatingRemapper.INSTANCE, owner);
        } catch(IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}
