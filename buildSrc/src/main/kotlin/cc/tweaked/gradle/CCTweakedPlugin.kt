package cc.tweaked.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Configures projects to match a shared configuration.
 */
class CCTweakedPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.extensions.create("cct", CCTweakedExtension::class.java)
    }
}
