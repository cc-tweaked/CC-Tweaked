package cc.tweaked.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.jvm.toolchain.JavaLanguageVersion

/**
 * Configures projects to match a shared configuration.
 */
class CCTweakedPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.extensions.create("cct", CCTweakedExtension::class.java)
    }

    companion object {
        val JAVA_VERSION = JavaLanguageVersion.of(17)
    }
}
