package dev.tigr.asmp.plugin

import groovy.transform.PackageScope
import org.gradle.api.Project
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.tasks.Jar

/**
 * @author Tigermouthbear 2/22/21
 */
class ASMPExtension {
    String output
    String intermediaryOutput

    // properties needed to pass to the annotation processor
    String input
    String inputFormat = null
    String mappingsName
    String outputFormat = null
    String intermediaryInput = null
    String intermediaryInputFormat = null
    String intermediaryMappingsName = null
    String intermediaryOutputFormat

    // names of jar tasks to add mappings to
    Set<String> tasks = null

    ASMPExtension(Project project) {
        project.afterEvaluate {
            // find locations for file outputs
            String buildPath = project.buildDir.path
            output = buildPath + File.separator + "tmp" + File.separator + "asmp" + File.separator + mappingsName
            intermediaryOutput = buildPath + File.separator + "tmp" + File.separator + "asmp" + File.separator + intermediaryMappingsName

            project.sourceSets.each { set ->
                // find compile task and add args
                def compileTask = project.tasks[set.compileJavaTaskName]
                if(!(compileTask instanceof JavaCompile)) throw new RuntimeException("[ASMP] Cannot add asmp args non JavaCompile task")
                setCompilerArgs(compileTask)

                // create pack task
                if(tasks == null) throw new RuntimeException("[ASMP] Jar task(s) not specified!")
                project.tasks.withType(Jar.class).forEach { jarTask ->
                    if(tasks.contains(jarTask.name)) {
                        project.tasks.maybeCreate("asmpPack${jarTask.name.capitalize()}", ASMPPackTask.class).configure {
                            jar = jarTask
                            mappings = [new File(this.output), new File(this.intermediaryOutput)]
                            dependsOn(compileTask)
                            jarTask.dependsOn(delegate)
                        }
                    }
                }
            }
        }
    }

    @PackageScope
    void setCompilerArgs(JavaCompile javaCompile) {
        javaCompile.options.compilerArgs << "-Aasmp.input=" + input
        if(inputFormat != null) javaCompile.options.compilerArgs << "-Aasmp.input.format=" + inputFormat
        javaCompile.options.compilerArgs << "-Aasmp.output=" + output
        if(outputFormat != null) javaCompile.options.compilerArgs << "-Aasmp.output.format=" + outputFormat
        if(intermediaryInput != null) javaCompile.options.compilerArgs << "-Aasmp.intermediary.input=" + intermediaryInput
        if(intermediaryInputFormat != null) javaCompile.options.compilerArgs << "-Aasmp.intermediary.input.format=" + intermediaryInputFormat
        if(intermediaryMappingsName != null) javaCompile.options.compilerArgs << "-Aasmp.intermediary.output=" + intermediaryOutput
        if(intermediaryOutputFormat != null) javaCompile.options.compilerArgs << "-Aasmp.intermediary.output.format=" + intermediaryOutputFormat
    }
}
