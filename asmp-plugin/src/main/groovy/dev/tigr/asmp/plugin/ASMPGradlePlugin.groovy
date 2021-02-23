package dev.tigr.asmp.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * @author Tigermouthbear 2/22/21
 */
class ASMPGradlePlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        project.extensions.create("asmp", ASMPExtension.class, project)
    }
}
