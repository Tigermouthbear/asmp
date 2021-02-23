package dev.tigr.asmp.plugin

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

/**
 * task to add generated mappings from annotation processor
 * to jar
 * @author Tigermouthbear
 */
class ASMPTask extends DefaultTask {
    @TaskAction
    def run() {
        // TODO: ADD ALL RESOURCES AND ACCOUNT FOR REBOF TASKS
    }
}
