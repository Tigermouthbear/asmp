package dev.tigr.asmp.ap;

import dev.tigr.asmp.annotations.At;
import dev.tigr.asmp.annotations.Patch;
import dev.tigr.asmp.annotations.modifications.Inject;
import dev.tigr.asmp.annotations.modifications.Modify;
import dev.tigr.asmp.exceptions.ASMPMissingApSetting;
import dev.tigr.asmp.obfuscation.CsvNameMapper;
import dev.tigr.asmp.obfuscation.ObfuscationMap;
import dev.tigr.asmp.obfuscation.ObfuscationMapper;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 * remaps needed srgs from file into new file with options for intermediaries
 * @author Tigermouthbear 2/12/21
 */
@SupportedAnnotationTypes({
    "dev.tigr.asmp.annotations.Patch",
    "dev.tigr.asmp.annotations.Inject",
    "dev.tigr.asmp.annotations.Modify"
})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class ASMPAnnotationProcessor extends AbstractProcessor {
    private final CsvNameMapper csvNameMapper = new CsvNameMapper();
    private final ObfuscationMapper obfuscationMapper = new ObfuscationMapper();
    private final ObfuscationMapper inputObfuscationMapper = new ObfuscationMapper();
    private final ObfuscationMapper outputObfuscationMapper = new ObfuscationMapper();
    private ObfuscationMapper.Format outputFormat;
    private File outputFile;

    // if there are intermediary csv mappings to use
    private boolean intermediary;
    // whether to save these intermediaries
    private boolean saveIntermediary;
    private File intermediaryOutputFile;

    @Override
    public void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);

        // mandatory settings
        String input = processingEnvironment.getOptions().get("asmp.input");
        String inputFormatIn = processingEnvironment.getOptions().get("asmp.input.format");
        String output = processingEnvironment.getOptions().get("asmp.output");
        String outputFormatIn = processingEnvironment.getOptions().get("asmp.output.format");
        if(input == null || inputFormatIn == null) throw new ASMPMissingApSetting("asmp.input");
        if(output == null) throw new ASMPMissingApSetting("asmp.output");
        ObfuscationMapper.Format inputFormat = inputFormatIn.equalsIgnoreCase("tsrg") ? ObfuscationMapper.Format.TSRG : ObfuscationMapper.Format.SRG;
        outputFormat = outputFormatIn == null ? ObfuscationMapper.Format.SRG : (outputFormatIn.equalsIgnoreCase("tsrg") ? ObfuscationMapper.Format.TSRG : ObfuscationMapper.Format.SRG);

        // optional intermediary settings
        String methods = processingEnvironment.getOptions().get("asmp.intermediary.methods");
        String fields = processingEnvironment.getOptions().get("asmp.intermediary.fields");
        String intermediaryOutput = processingEnvironment.getOptions().get("asmp.intermediary.output");
        if(methods != null || fields != null) intermediary = true;
        if(intermediaryOutput != null) {
            saveIntermediary = true;
            intermediaryOutputFile = new File(intermediaryOutput);
            if(!intermediaryOutputFile.exists()) {
                try {
                    intermediaryOutputFile.createNewFile();
                } catch(IOException e) {
                    e.printStackTrace();
                }
            }
        }

        // find srg input
        File inputFile = new File(input);
        if(!inputFile.exists()) throw new RuntimeException("ASMP SRG input file not found!");

        // find output location
        outputFile = new File(output);
        if(!outputFile.exists()) {
            try {
                outputFile.createNewFile();
            } catch(IOException e) {
                e.printStackTrace();
            }
        }

        // read intermediaries
        if(intermediary) {
            loadIntermediaryMapper(processingEnvironment, methods);
            loadIntermediaryMapper(processingEnvironment, fields);
            inputObfuscationMapper.setNameMapper(csvNameMapper);
        }

        // read mappings
        try {
            inputObfuscationMapper.read(inputFile, inputFormat);
        } catch(IOException e) {
            e.printStackTrace();
        }
        obfuscationMapper.read(inputObfuscationMapper);
    }

    private void loadIntermediaryMapper(ProcessingEnvironment processingEnvironment, String fields) {
        if(fields != null) {
            File file = new File(fields);
            if(file.exists()) csvNameMapper.read(file);
            else processingEnvironment.getMessager().printMessage(Diagnostic.Kind.ERROR, "[ASMP] Intermediary file " + fields + " does not exist!");
        }
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        if(roundEnvironment.processingOver()) return true;

        // add all mappings
        addPatches(roundEnvironment);
        addInject(roundEnvironment);
        addModify(roundEnvironment);

        // save srg
        try {
            outputObfuscationMapper.write(outputFile, outputFormat);
        } catch(IOException e) {
            e.printStackTrace();
        }

        // generate intermediary mappings if selected
        if(saveIntermediary) {
            ObfuscationMap obfuscationMap = new ObfuscationMap();
            for(Map.Entry<String, String> entry: outputObfuscationMapper.getClassMap().entrySet()) {
                obfuscationMap.put(inputObfuscationMapper.getClassMap().getDeobf(entry.getKey()), entry.getValue());
            }
            outputObfuscationMapper.getClassMap().clear();
            outputObfuscationMapper.getClassMap().putAll(obfuscationMap);
            obfuscationMap.clear();

            for(Map.Entry<String, String> entry: outputObfuscationMapper.getFieldMap().entrySet()) {
                obfuscationMap.put(inputObfuscationMapper.getFieldMap().getDeobf(entry.getKey()), entry.getValue());
            }
            outputObfuscationMapper.getFieldMap().clear();
            outputObfuscationMapper.getFieldMap().putAll(obfuscationMap);
            obfuscationMap.clear();

            for(Map.Entry<String, String> entry: outputObfuscationMapper.getMethodMap().entrySet()) {
                if(entry.getKey().contains("<init>") || entry.getKey().contains("<clinit>")) {
                    int index = entry.getKey().indexOf(";");
                    String name = entry.getKey().substring(1, index);
                    obfuscationMap.put(entry.getKey().replaceFirst(name, inputObfuscationMapper.getClassMap().getDeobf(name)), entry.getValue());
                } else obfuscationMap.put(inputObfuscationMapper.getMethodMap().getDeobf(entry.getKey()), entry.getValue());
            }
            outputObfuscationMapper.getMethodMap().clear();
            outputObfuscationMapper.getMethodMap().putAll(obfuscationMap);
            obfuscationMap.clear();

            try {
                outputObfuscationMapper.write(intermediaryOutputFile, outputFormat);
            } catch(IOException e) {
                e.printStackTrace();
            }
        }

        return true;
    }

    private void addPatches(RoundEnvironment roundEnvironment) {
        for(Element element: roundEnvironment.getElementsAnnotatedWith(Patch.class)) {
            String name = element.getAnnotation(Patch.class).value().replaceAll("\\.", "/");
            outputObfuscationMapper.addClass(obfuscationMapper.unmapClass(name), name);
        }
    }

    private void addAt(At at) {
        if(at.value().equals("INVOKE")) {
            outputObfuscationMapper.addMethod(obfuscationMapper.unmapMethodReference(at.target()).toString(), at.target());
        }
    }

    private void addInject(RoundEnvironment roundEnvironment) {
        for(Element element: roundEnvironment.getElementsAnnotatedWith(Inject.class)) {
            Inject inject = element.getAnnotation(Inject.class);
            addAt(inject.at());
            outputObfuscationMapper.addMethod(obfuscationMapper.unmapMethodReference(inject.method()).toString(), inject.method());
        }
    }

    private void addModify(RoundEnvironment roundEnvironment) {
        for(Element element: roundEnvironment.getElementsAnnotatedWith(Modify.class)) {
            Modify modify = element.getAnnotation(Modify.class);
            if(modify.value().isEmpty()) continue;
            addAt(modify.at());
            outputObfuscationMapper.addMethod(obfuscationMapper.unmapMethodReference(modify.value()).toString(), modify.value());
        }
    }
}
