package dev.tigr.asmp.ap;

import dev.tigr.asmp.annotations.At;
import dev.tigr.asmp.annotations.Patch;
import dev.tigr.asmp.annotations.modifications.Inject;
import dev.tigr.asmp.annotations.modifications.Modify;
import dev.tigr.asmp.exceptions.ASMPMissingApSetting;
import dev.tigr.asmp.obfuscation.CsvNameMapper;
import dev.tigr.asmp.obfuscation.ObfuscationMap;
import dev.tigr.asmp.obfuscation.SrgObfuscationMapper;
import dev.tigr.asmp.obfuscation.SrgMapper;

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
    "dev.tigr.asmp.annotations.At",
    "dev.tigr.asmp.annotations.Inject",
    "dev.tigr.asmp.annotations.Modify"
})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class ASMPAnnotationProcessor extends AbstractProcessor {
    private final SrgObfuscationMapper srgObfuscationMapper = new SrgObfuscationMapper();
    private final CsvNameMapper csvNameMapper = new CsvNameMapper();
    private final SrgMapper inputSrgMapper = new SrgMapper();
    private final SrgMapper outputSrgMapper = new SrgMapper();
    private File inputFile;
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
        String output = processingEnvironment.getOptions().get("asmp.output");
        if(input == null) throw new ASMPMissingApSetting("asmp.input");
        if(output == null) throw new ASMPMissingApSetting("asmp.output");

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
        inputFile = new File(input);
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
            inputSrgMapper.setNameMapper(csvNameMapper);
        }

        // read srg
        try {
            inputSrgMapper.read(inputFile);
            srgObfuscationMapper.read(inputSrgMapper);
        } catch(IOException e) {
            e.printStackTrace();
        }

        processingEnvironment.getMessager().printMessage(Diagnostic.Kind.NOTE, "loaded ASMP AP!");
    }

    private void loadIntermediaryMapper(ProcessingEnvironment processingEnvironment, String fields) {
        if(fields != null) {
            File file = new File(fields);
            if(file.exists()) {
                try {
                    csvNameMapper.read(file);
                } catch(IOException e) {
                    e.printStackTrace();
                }
            }
            else processingEnvironment.getMessager().printMessage(Diagnostic.Kind.ERROR, "[ASMP] Intermediary file " + fields + " does not exist!");
        }
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        if(roundEnvironment.processingOver()) return true;

        // add all mappings
        addPatches(roundEnvironment);
        addAt(roundEnvironment);
        addInject(roundEnvironment);
        addModify(roundEnvironment);

        // save srg
        try {
            outputSrgMapper.write(outputFile);
        } catch(IOException e) {
            e.printStackTrace();
        }

        // generate intermediary mappings if selected
        if(saveIntermediary) {
            // read srg without remapping again
            SrgMapper srgMapper = new SrgMapper();
            try {
                srgMapper.read(inputFile);
            } catch(IOException e) {
                e.printStackTrace();
            }

            ObfuscationMap obfuscationMap = new ObfuscationMap();
            for(Map.Entry<String, String> entry: outputSrgMapper.getClassMap().entrySet()) {
                obfuscationMap.put(srgMapper.getClassMap().getDeobf(entry.getKey()), entry.getValue());
            }
            outputSrgMapper.getClassMap().clear();
            outputSrgMapper.getClassMap().putAll(obfuscationMap);
            obfuscationMap.clear();

            for(Map.Entry<String, String> entry: outputSrgMapper.getFieldMap().entrySet()) {
                obfuscationMap.put(srgMapper.getFieldMap().getDeobf(entry.getKey()), entry.getValue());
            }
            outputSrgMapper.getFieldMap().clear();
            outputSrgMapper.getFieldMap().putAll(obfuscationMap);
            obfuscationMap.clear();

            for(Map.Entry<String, String> entry: outputSrgMapper.getMethodMap().entrySet()) {
                if(entry.getKey().contains("<init>") || entry.getKey().contains("<clinit>")) {
                    int index = entry.getKey().indexOf(";");
                    String name = entry.getKey().substring(1, index);
                    obfuscationMap.put(entry.getKey().replaceFirst(name, srgMapper.getClassMap().getDeobf(name)), entry.getValue());
                } else obfuscationMap.put(srgMapper.getMethodMap().getDeobf(entry.getKey()), entry.getValue());
            }
            outputSrgMapper.getMethodMap().clear();
            outputSrgMapper.getMethodMap().putAll(obfuscationMap);
            obfuscationMap.clear();

            try {
                outputSrgMapper.write(intermediaryOutputFile);
            } catch(IOException e) {
                e.printStackTrace();
            }
        }

        return true;
    }

    private void addPatches(RoundEnvironment roundEnvironment) {
        for(Element element: roundEnvironment.getElementsAnnotatedWith(Patch.class)) {
            String name = element.getAnnotation(Patch.class).value().replaceAll("\\.", "/");
            outputSrgMapper.addClass(srgObfuscationMapper.unmapClass(name), name);
        }
    }

    private void addAt(RoundEnvironment roundEnvironment) {
        for(Element element: roundEnvironment.getElementsAnnotatedWith(At.class)) {
            At at = element.getAnnotation(At.class);
            if(at.value() == At.Target.INVOKE) {
                outputSrgMapper.addMethod(srgObfuscationMapper.unmapMethodReference(at.target()).toString(), at.target());
            }
        }
    }

    private void addInject(RoundEnvironment roundEnvironment) {
        for(Element element: roundEnvironment.getElementsAnnotatedWith(Inject.class)) {
            Inject inject = element.getAnnotation(Inject.class);
            outputSrgMapper.addMethod(srgObfuscationMapper.unmapMethodReference(inject.method()).toString(), inject.method());
        }
    }

    private void addModify(RoundEnvironment roundEnvironment) {
        for(Element element: roundEnvironment.getElementsAnnotatedWith(Modify.class)) {
            Modify modify = element.getAnnotation(Modify.class);
            if(modify.value().isEmpty()) continue;
            outputSrgMapper.addMethod(srgObfuscationMapper.unmapMethodReference(modify.value()).toString(), modify.value());
        }
    }
}
