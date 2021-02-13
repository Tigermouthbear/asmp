package dev.tigr.asmp.obfuscation;

import java.io.*;
import java.util.Map;

/**
 * Interface for reading and writing to SRG files
 * @author Tigermouthbear 2/11/21
 */
public class SrgMapper {
    private CsvNameMapper csvNameMapper = null;
    private final ObfuscationMap clazzMap = new ObfuscationMap();
    private final ObfuscationMap methodMap = new ObfuscationMap();
    private final ObfuscationMap fieldMap = new ObfuscationMap();

    public void read(Reader reader) {
        try {
            BufferedReader bufferedReader = new BufferedReader(reader);
            for(String line = bufferedReader.readLine(); line != null; line = bufferedReader.readLine()) {
                if(line.startsWith("CL: ")) readClass(line.substring(4));
                else if(line.startsWith("FD: ")) readField(line.substring(4));
                else if(line.startsWith("MD: ")) readMethod(line.substring(4));
            }
            bufferedReader.close();
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    public void read(File input) {
        try {
            read(new FileReader(input));
        } catch(FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void read(SrgMapper srgMapper) {
        clazzMap.putAll(srgMapper.clazzMap);
        methodMap.putAll(srgMapper.methodMap);
        fieldMap.putAll(srgMapper.fieldMap);
    }

    public void setNameMapper(CsvNameMapper csvNameMapper) {
        this.csvNameMapper = csvNameMapper;
    }

    private String remap(String name) {
        if(csvNameMapper == null) return name;
        return csvNameMapper.getMappings().getDeobf(name);
    }

    public void clear() {
        clazzMap.clear();
        methodMap.clear();
        fieldMap.clear();
    }

    // com/example/test
    private void readClass(String line) {
        String[] split = line.split(" ");
        if(split.length != 2) return;
        clazzMap.put(split[0], split[1]);
    }

    // Lcom/example/test;field
    private void readField(String line) {
        String[] split = line.split(" ");
        if(split.length != 2) return;
        fieldMap.put(readEntry(split[0]), readEntry(split[1]));
    }

    // Lcom/example/test;name()V
    private void readMethod(String line) {
        String[] split = line.split(" ");
        if(split.length != 4) return;
        String obf = readEntry(split[0]) + split[1];
        String deobf = readEntry(split[2]) + split[3];
        methodMap.put(obf, deobf);
    }

    // com/example/test/field --> Lcom/example/test;name
    private String readEntry(String pair) {
        int index = pair.lastIndexOf("/");
        String clazz = pair.substring(0, index);
        String name = pair.substring(index + 1);
        name = remap(name);
        return "L" + clazz + ";" + name;
    }

    public void write(File output) throws IOException {
        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(output));
        for(Map.Entry<String, String> entry: clazzMap.entrySet()) writeClass(entry, bufferedWriter);
        for(Map.Entry<String, String> entry: fieldMap.entrySet()) writeField(entry, bufferedWriter);
        for(Map.Entry<String, String> entry: methodMap.entrySet()) writeMethod(entry, bufferedWriter);
        bufferedWriter.close();
    }

    private void writeClass(Map.Entry<String, String> entry, BufferedWriter bufferedWriter) throws IOException {
        bufferedWriter.write("CL: ");
        bufferedWriter.write(entry.getKey()); // deobf
        bufferedWriter.write(" ");
        bufferedWriter.write(entry.getValue()); // obf
        bufferedWriter.newLine();
    }

    private void writeField(Map.Entry<String, String> entry, BufferedWriter bufferedWriter) throws IOException {
        bufferedWriter.write("FD: ");
        bufferedWriter.write(writeEntry(entry.getKey())); // deobf
        bufferedWriter.write(" ");
        bufferedWriter.write(writeEntry(entry.getValue())); // obf
        bufferedWriter.newLine();
    }

    private void writeMethod(Map.Entry<String, String> entry, BufferedWriter bufferedWriter) throws IOException {
        bufferedWriter.write("MD: ");

        String obf = writeEntry(entry.getKey());
        int obfIndex = obf.indexOf("(");
        String deobf = writeEntry(entry.getValue());
        int deobfIndex = deobf.indexOf("(");

        bufferedWriter.write(obf.substring(0, obfIndex));
        bufferedWriter.write(" ");
        bufferedWriter.write(obf.substring(obfIndex));
        bufferedWriter.write(" ");
        bufferedWriter.write(deobf.substring(0, deobfIndex));
        bufferedWriter.write(" ");
        bufferedWriter.write(deobf.substring(deobfIndex));
        bufferedWriter.write(" ");
        bufferedWriter.newLine();
    }

    // Lcom/example/test;name --> com/example/test/field
    private String writeEntry(String pair) {
        char[] chars = pair.substring(1).toCharArray();
        chars[pair.indexOf(";") - 1] = '/';
        return String.valueOf(chars);
    }

    public void addClass(String obf, String deobf) {
        clazzMap.put(obf, deobf);
    }

    public void addField(String obf, String deobf) {
        fieldMap.put(obf, deobf);
    }

    public void addMethod(String obf, String deobf) {
        methodMap.put(obf, deobf);
    }

    public ObfuscationMap getClassMap() {
        return clazzMap;
    }

    public ObfuscationMap getFieldMap() {
        return fieldMap;
    }

    public ObfuscationMap getMethodMap() {
        return methodMap;
    }
}
