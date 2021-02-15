package dev.tigr.asmp.obfuscation;

import dev.tigr.asmp.exceptions.ASMPBadTargetException;
import dev.tigr.asmp.util.Reference;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Interface for reading and writing to SRG files
 * @author Tigermouthbear 2/11/21
 */
public class ObfuscationMapper implements IObfuscationMapper {
    private CsvNameMapper csvNameMapper = null;
    private final ObfuscationMap clazzMap = new ObfuscationMap();
    private final ObfuscationMap methodMap = new ObfuscationMap();
    private final ObfuscationMap fieldMap = new ObfuscationMap();

    public enum Format { SRG, TSRG }

    public void read(File input, Format format) throws IOException {
        read(new FileReader(input), format);
    }

    public void read(ObfuscationMapper obfuscationMapper) {
        clazzMap.putAll(obfuscationMapper.clazzMap);
        methodMap.putAll(obfuscationMapper.methodMap);
        fieldMap.putAll(obfuscationMapper.fieldMap);
    }

    public void read(Reader reader, Format format) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(reader);

        // tsrg vars
        String lastObf = null; // last obf class name
        String lastDeobf = null; // last deobf class name

        // read line by line
        for(String line = bufferedReader.readLine(); line != null; line = bufferedReader.readLine()) {
            if(format == Format.SRG) {
                if(line.startsWith("CL: ")) {
                    // class
                    String[] split = line.substring(4).split(" ");
                    if(split.length != 2) return;
                    clazzMap.put(split[0], split[1]);
                }
                else if(line.startsWith("FD: ")) {
                    // field
                    String[] split = line.substring(4).split(" ");
                    if(split.length != 2) return;
                    fieldMap.put(readSrgEntry(split[0]), readSrgEntry(split[1]));
                }
                else if(line.startsWith("MD: ")) {
                    // method
                    String[] split = line.substring(4).split(" ");
                    if(split.length != 4) return;
                    String obf = readSrgEntry(split[0]) + split[1];
                    String deobf = readSrgEntry(split[2]) + split[3];
                    methodMap.put(obf, deobf);
                }
            } else if(format == Format.TSRG) {
                if(line.startsWith("\t") && lastObf != null) {
                    // field or method
                    String[] split = line.substring(1).split(" ");
                    if(split.length == 2) {
                        // field
                        fieldMap.put(lastObf + remap(split[0]),  lastDeobf + remap(split[1]));
                    } else if(split.length == 3) {
                        // method, gonna have to deobf all method descriptions after reading all classes
                        methodMap.put(lastObf + remap(split[0]) + split[1], lastDeobf + remap(split[2]));
                    }
                } else {
                    // class
                    String[] split = line.split(" ");
                    if(split.length == 2) {
                        lastObf = split[0];
                        lastDeobf = split[1];
                        clazzMap.put(lastObf, lastDeobf);
                        lastObf = "L" + lastObf + ";";
                        lastDeobf = "L" + lastDeobf + ";";
                    }
                }
            }
        }

        // go back over and deobf method descriptions if TSRG
        if(format == Format.TSRG) {
            ObfuscationMap obfuscationMap = new ObfuscationMap();
            for(Map.Entry<String, String> entry: methodMap.entrySet()) {
                int index = entry.getKey().indexOf("(");
                obfuscationMap.put(entry.getKey(), entry.getValue() + mapDesc(entry.getKey().substring(index)));
            }
            methodMap.clear();
            methodMap.putAll(obfuscationMap);
        }

        bufferedReader.close();
    }

    // com/example/test/field --> Lcom/example/test;name
    private String readSrgEntry(String pair) {
        int index = pair.lastIndexOf("/");
        String clazz = pair.substring(0, index);
        String name = pair.substring(index + 1);
        name = remap(name);
        return "L" + clazz + ";" + name;
    }

    public void setNameMapper(CsvNameMapper csvNameMapper) {
        this.csvNameMapper = csvNameMapper;
    }

    private String remap(String name) {
        if(csvNameMapper == null) return name;
        return csvNameMapper.getMappings().getDeobf(name);
    }

    public void write(File output, Format format) throws IOException {
        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(output));

        if(format == Format.SRG) {
            for(Map.Entry<String, String> entry: clazzMap.entrySet()) {
                bufferedWriter.write("CL: ");
                bufferedWriter.write(entry.getKey()); // deobf
                bufferedWriter.write(" ");
                bufferedWriter.write(entry.getValue()); // obf
                bufferedWriter.newLine();
            }
            for(Map.Entry<String, String> entry: fieldMap.entrySet()) {
                bufferedWriter.write("FD: ");
                bufferedWriter.write(writeSrgEntry(entry.getKey())); // deobf
                bufferedWriter.write(" ");
                bufferedWriter.write(writeSrgEntry(entry.getValue())); // obf
                bufferedWriter.newLine();
            }
            for(Map.Entry<String, String> entry: methodMap.entrySet()) {
                bufferedWriter.write("MD: ");

                String obf = writeSrgEntry(entry.getKey());
                int obfIndex = obf.indexOf("(");
                String deobf = writeSrgEntry(entry.getValue());
                int deobfIndex = deobf.indexOf("(");

                bufferedWriter.write(obf.substring(0, obfIndex));
                bufferedWriter.write(" ");
                bufferedWriter.write(obf.substring(obfIndex));
                bufferedWriter.write(" ");
                bufferedWriter.write(deobf.substring(0, deobfIndex));
                bufferedWriter.write(" ");
                bufferedWriter.write(deobf.substring(deobfIndex));
                bufferedWriter.newLine();
            }
        } else if(format == Format.TSRG) {
            throw new RuntimeException("TSRG writing not supported yet!");
        }

        bufferedWriter.close();
    }

    // Lcom/example/test;name --> com/example/test/field
    private String writeSrgEntry(String pair) {
        char[] chars = pair.substring(1).toCharArray();
        chars[pair.indexOf(";") - 1] = '/';
        return String.valueOf(chars);
    }

    @Override
    public String unmapClass(String name) {
        return clazzMap.getObf(name);
    }

    @Override
    public String mapClass(String name) {
        return clazzMap.getDeobf(name);
    }

    @Override
    public String unmapField(String owner, String name) {
        return unmapFieldReference("L" + owner + ";" + name).getName();
    }

    @Override
    public String mapField(String owner, String name) {
        return mapFieldReference("L" + owner + ";" + name).getName();
    }

    @Override
    public String unmapMethod(String owner, String name, String desc) {
        return unmapMethodReference("L" + owner + ";" + name + desc).getName();
    }

    @Override
    public String mapMethod(String owner, String name, String desc) {
        return mapMethodReference("L" + owner + ";" + name + desc).getName();
    }

    @Override
    public Reference unmapFieldReference(String descriptor) {
        boolean valid = descriptor.contains(";");
        if(!valid) throw new ASMPBadTargetException(descriptor);

        descriptor = fieldMap.getObf(descriptor);

        int index0 = descriptor.indexOf(";");
        String owner = descriptor.substring(1, index0);
        String name = descriptor.substring(index0 + 1);

        return new Reference(owner, name);
    }

    @Override
    public Reference mapFieldReference(String descriptor) {
        boolean valid = descriptor.contains(";");
        if(!valid) throw new ASMPBadTargetException(descriptor);

        descriptor = fieldMap.getDeobf(descriptor);

        int index0 = descriptor.indexOf(";");
        String owner = descriptor.substring(1, index0);
        String name = descriptor.substring(index0 + 1);

        return new Reference(owner, name);
    }

    @Override
    public Reference unmapMethodReference(String descriptor) {
        boolean initializer = descriptor.contains("<init>") || descriptor.contains("<clinit>");
        boolean inherited = descriptor.contains(";<super>.");
        boolean valid = descriptor.contains("(") && descriptor.contains(";");
        if(!valid) throw new ASMPBadTargetException(descriptor);

        // inherited is when a method from the super class is specified
        // the descriptor looks like: Ltest/class;<super>.method()V
        // turns into Lobf;obf()V where obf is the name of class or method
        // this works most of the time, but I couldn't find another way to do it
        // with the lack of inheritance information in mapping files
        if(inherited) {
            String owner = descriptor.substring(1, descriptor.indexOf(";"));
            String end = descriptor.substring(descriptor.indexOf(".") + 1);
            Optional<String> key = methodMap.values().stream().filter(method -> method.endsWith(end)).findFirst();
            if(!key.isPresent()) throw new RuntimeException("Invalid ASMP super method target: " + descriptor);
            descriptor = methodMap.getObf(key.get());
            descriptor = "L" + unmapClass(owner) + descriptor.substring(descriptor.indexOf(";"));
        } else if(!initializer) descriptor = methodMap.getObf(descriptor);

        int index0 = descriptor.indexOf(";");
        int index1 = descriptor.indexOf("(");
        String owner = descriptor.substring(1, index0);
        String name = descriptor.substring(index0 + 1, index1);
        String desc = descriptor.substring(index1);

        if(initializer) {
            owner = unmapClass(owner);
            desc = unmapDesc(desc);
        }

        return new Reference(owner, name, desc);
    }

    @Override
    public Reference mapMethodReference(String descriptor) {
        boolean initializer = descriptor.contains("<init>") || descriptor.contains("<clinit>");
        boolean inherited = descriptor.contains(";<super>.");
        boolean valid = descriptor.contains("(") && descriptor.contains(";");
        if(!valid) throw new ASMPBadTargetException(descriptor);

        // inherited is when a method from the super class is specified
        // the descriptor looks like: Ltest/class;<super>.method()V
        // turns into Lobf;obf()V where obf is the name of class or method
        // this works most of the time, but I couldn't find another way to do it
        // with the lack of inheritance information in mapping files
        if(inherited) {
            String owner = descriptor.substring(1, descriptor.indexOf(";"));
            String end = descriptor.substring(descriptor.indexOf(".") + 1);
            Optional<String> key = methodMap.values().stream().filter(method -> method.endsWith(end)).findFirst();
            if(!key.isPresent()) throw new RuntimeException("Invalid ASMP super method target: " + descriptor);
            descriptor = methodMap.getObf(key.get());
            descriptor = "L" + mapClass(owner) + descriptor.substring(descriptor.indexOf(";"));
        } else if(!initializer) descriptor = methodMap.getDeobf(descriptor);

        int index0 = descriptor.indexOf(";");
        int index1 = descriptor.indexOf("(");
        String owner = descriptor.substring(1, index0);
        String name = descriptor.substring(index0 + 1, index1);
        String desc = descriptor.substring(index1);

        if(initializer) {
            owner = mapClass(owner);
            desc = mapDesc(desc);
        }

        return new Reference(owner, name, desc);
    }

    public void addClass(String obf, String deobf) {
        clazzMap.put(obf, deobf);
    }

    public void addField(String obf, String deobf) {
        fieldMap.put(obf, deobf);
    }

    public void addMethod(String obf, String deobf) {
        if(obf.contains("<init>") || obf.contains("<clinit>")) {
            // only add class name and desc classes when initialization block
            String obfOwner = obf.substring(1, obf.indexOf(";"));
            List<String> obfDesc = getDescClasses(obf.substring(obf.indexOf("(")));
            String deobfOwner = deobf.substring(1, deobf.indexOf(";"));
            List<String> deobfDesc = getDescClasses(deobf.substring(deobf.indexOf("(")));
            clazzMap.put(obfOwner, deobfOwner);
            for(int i = 0; i < obfDesc.size(); i++) clazzMap.put(obfDesc.get(i), deobfDesc.get(i));
        } else methodMap.put(obf, deobf);
    }

    private List<String> getDescClasses(String desc) {
        boolean looking = false;
        List<String> list = new ArrayList<>();
        StringBuilder curr = new StringBuilder();

        for(char c: desc.toCharArray()) {
            if(!looking) {
                if(c == 'L') looking = true;
            } else {
                if(c == ';') {
                    list.add(curr.toString());
                    curr = new StringBuilder();
                    looking = false;
                } else curr.append(c);
            }
        }

        return list;
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
