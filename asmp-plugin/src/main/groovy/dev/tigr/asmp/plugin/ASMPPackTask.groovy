package dev.tigr.asmp.plugin

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.jvm.tasks.Jar

/**
 * task to add generated mappings from annotation processor
 * to jar
 * @author Tigermouthbear
 */
class ASMPPackTask extends DefaultTask {
    Jar jar
    Set<File> mappings

    @TaskAction
    def run() {
        mappings.each { file ->
            jar.from(file)
        }
    }
}
