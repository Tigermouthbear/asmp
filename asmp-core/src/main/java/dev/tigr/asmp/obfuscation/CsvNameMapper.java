package dev.tigr.asmp.obfuscation;

import java.io.*;

/**
 * loads method/field node name remapping data from csv file and allows
 * remapping of {@link SrgObfuscationMapper}
 * @author Tigermouthbear 2/12/21
 */
public class CsvNameMapper {
    private final ObfuscationMap mappings = new ObfuscationMap();

    public void read(Reader reader) {
        try {
            BufferedReader bufferedReader = new BufferedReader(reader);
            for(String line = bufferedReader.readLine(); line != null; line = bufferedReader.readLine()) {
                readLine(line);
            }
            bufferedReader.close();
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    public void read(File input) {
        try {
            read(new FileReader(input));
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    public void readLine(String line) {
        String[] split = line.split(",");
        if(split.length < 2) return;
        mappings.put(split[0], split[1]);
    }

    public ObfuscationMap getMappings() {
        return mappings;
    }
}
