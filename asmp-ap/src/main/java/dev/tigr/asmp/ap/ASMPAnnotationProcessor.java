package dev.tigr.asmp.ap;

import dev.tigr.asmp.annotations.At;
import dev.tigr.asmp.annotations.Patch;
import dev.tigr.asmp.annotations.modifications.Inject;
import dev.tigr.asmp.annotations.modifications.Modify;
import dev.tigr.asmp.exceptions.ASMPMissingApSetting;
import dev.tigr.asmp.obfuscation.ObfuscationMapper;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.io.File;
import java.io.IOException;
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
@SupportedOptions({
        "asmp.input",
        "asmp.input.format",
        "asmp.output",
        "asmp.output.format",
        "asmp.intermediary.input",
        "asmp.intermediary.input.format",
        "asmp.intermediary.output",
        "asmp.intermediary.output.format"
})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class ASMPAnnotationProcessor extends AbstractProcessor {
    private final ObfuscationMapper obfuscationMapper = new ObfuscationMapper();
    private final ObfuscationMapper outputObfuscationMapper = new ObfuscationMapper();
    private final ObfuscationMapper intermediaryObfuscationMapper = new ObfuscationMapper();
    private ObfuscationMapper.Format outputFormat;
    private File outputFile;
    private boolean intermediary;
    private ObfuscationMapper.Format intermediaryOutputFormat;
    private File intermediaryOutputFile = null;

    @Override
    public void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);

        processingEnvironment.getMessager().printMessage(Diagnostic.Kind.NOTE, "Running ASMP annotation processor...");

        // mandatory settings
        String input = processingEnvironment.getOptions().get("asmp.input");
        String inputFormatIn = processingEnvironment.getOptions().get("asmp.input.format");
        String output = processingEnvironment.getOptions().get("asmp.output");
        String outputFormatIn = processingEnvironment.getOptions().get("asmp.output.format");
        if(input == null || inputFormatIn == null) throw new ASMPMissingApSetting("asmp.input");
        if(output == null) throw new ASMPMissingApSetting("asmp.output");
        ObfuscationMapper.Format inputFormat = getFormat(inputFormatIn);
        outputFormat =  getFormat(outputFormatIn);

        // optional intermediary settings
        String intermediaryInput = processingEnvironment.getOptions().get("asmp.intermediary.input");
        String intermediaryInputFormatIn = processingEnvironment.getOptions().get("asmp.intermediary.input.format");
        String intermediaryOutput = processingEnvironment.getOptions().get("asmp.intermediary.output");
        String intermediaryOutputFormatIn = processingEnvironment.getOptions().get("asmp.intermediary.output.format");
        ObfuscationMapper.Format intermediaryInputFormat = getFormat(intermediaryInputFormatIn);
        intermediaryOutputFormat = getFormat(intermediaryOutputFormatIn);

        // find input and output files
        File inputFile = new File(input);
        if(!inputFile.exists()) throw new RuntimeException("ASMP mapping input file not found! " + inputFile.getAbsolutePath());
        outputFile = new File(output);
        if(!outputFile.getParentFile().exists())
            outputFile.getParentFile().mkdirs();
        if(!outputFile.exists()) {
            try {
                outputFile.createNewFile();
            } catch(IOException e) {
                e.printStackTrace();
            }
        }

        // find intermediary files
        File intermediaryInputFile = null;
        if(intermediaryInput != null) {
            intermediary = true;
            intermediaryInputFile = new File(intermediaryInput);
            if(!intermediaryInputFile.exists()) throw new RuntimeException("ASMP intermediary input file not found! " + intermediaryInputFile.getAbsolutePath());
        }
        if(intermediaryOutput != null) {
            intermediaryOutputFile = new File(intermediaryOutput);
            if(!intermediaryOutputFile.getParentFile().exists())
                intermediaryOutputFile.getParentFile().mkdirs();
            if(!intermediaryOutputFile.exists()) {
                try {
                    intermediaryOutputFile.createNewFile();
                } catch(IOException e) {
                    e.printStackTrace();
                }
            }
        }

        // read intermediaries and change input mappings
        if(intermediary && intermediaryInputFile != null) {
            try {
                intermediaryObfuscationMapper.read(intermediaryInputFile, intermediaryInputFormat);
            } catch(IOException e) {
                e.printStackTrace();
            }

            // join the intermediaries
            obfuscationMapper.setIntermediaries(intermediaryObfuscationMapper);
        }

        // read mappings
        try {
            obfuscationMapper.read(inputFile, inputFormat);
        } catch(IOException e) {
            e.printStackTrace();
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
        if(intermediaryOutputFile != null && intermediary) {
            try {
                outputObfuscationMapper.replaceDeobf(intermediaryObfuscationMapper);
                outputObfuscationMapper.write(intermediaryOutputFile, intermediaryOutputFormat);
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

    private ObfuscationMapper.Format getFormat(String format) {
        return format == null ? ObfuscationMapper.Format.SRG : (format.equalsIgnoreCase("tsrg") ? ObfuscationMapper.Format.TSRG : ObfuscationMapper.Format.SRG);
    }
}
