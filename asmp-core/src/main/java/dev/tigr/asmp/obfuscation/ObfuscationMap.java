package dev.tigr.asmp.obfuscation;

import java.util.Collection;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

/**
 * a map with bidirectional
 * @author Tigermouthbear 2/12/21
 */
public class ObfuscationMap implements Map<String, String> {
    private final Map<String, String> obf2deobf = new Hashtable<>();
    private final Map<String, String> deobf2obf = new Hashtable<>();

    @Override
    public synchronized String put(String obf, String deobf) {
        obf2deobf.put(obf, deobf);
        deobf2obf.put(deobf, obf);
        return obf;
    }

    public synchronized String getDeobf(String obf) {
        return obf2deobf.getOrDefault(obf, obf);
    }

    public synchronized String getObf(String deobf) {
        return deobf2obf.getOrDefault(deobf, deobf);
    }

    @Override
    public synchronized void clear() {
        obf2deobf.clear();
        deobf2obf.clear();
    }

    @Override
    public int size() {
        return obf2deobf.size();
    }

    @Override
    public boolean isEmpty() {
        return obf2deobf.isEmpty();
    }

    @Override
    public boolean containsKey(Object o) {
        return obf2deobf.containsKey(o);
    }

    @Override
    public boolean containsValue(Object o) {
        return obf2deobf.containsValue(o);
    }

    @Override
    public String get(Object o) {
        return obf2deobf.get(o);
    }

    @Override
    public String remove(Object o) {
        return null;
    }

    @Override
    public void putAll(Map<? extends String, ? extends String> map) {
        map.forEach(this::put);
    }

    @Override
    public Set<String> keySet() {
        return obf2deobf.keySet();
    }

    @Override
    public Collection<String> values() {
        return obf2deobf.values();
    }

    @Override
    public Set<Entry<String, String>> entrySet() {
        return obf2deobf.entrySet();
    }
}
