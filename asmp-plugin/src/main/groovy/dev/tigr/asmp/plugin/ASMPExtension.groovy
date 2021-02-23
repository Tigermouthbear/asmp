package dev.tigr.asmp.plugin

import groovy.transform.PackageScope
import org.gradle.api.Project
import org.gradle.api.tasks.compile.JavaCompile

/**
 * @author Tigermouthbear 2/22/21
 */
class ASMPExtension {
    // properties needed to pass to the annotation processor
    String input
    String inputFormat = null
    String output
    String outputFormat = null
    String intermediaryInput = null
    String intermediaryInputFormat = null
    String intermediaryOutput = null
    String intermediaryOutputFormat = null

    ASMPExtension(Project project) {
        project.afterEvaluate {
            project.sourceSets.each { set ->
                def compileTask = project.tasks[set.compileJavaTaskName]
                if(!(compileTask instanceof JavaCompile)) throw new RuntimeException("Cannot add asmp args non JavaCompile task")
                setCompilerArgs(compileTask)
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
        if(intermediaryOutput != null) javaCompile.options.compilerArgs << "-Aasmp.intermediary.output=" + intermediaryOutput
        if(intermediaryOutputFormat != null) javaCompile.options.compilerArgs << "-Aasmp.intermediary.output.format=" + intermediaryOutputFormat
    }
}
