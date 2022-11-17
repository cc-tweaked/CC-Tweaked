package cc.tweaked.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.jvm.toolchain.JavaLanguageVersion

/**
 * Configures projects to match a shared configuration.
 */
class CCTweakedPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val cct = project.extensions.create("cct", CCTweakedExtension::class.java)

        project.plugins.withType(JavaPlugin::class.java) {
            val sourceSets = project.extensions.getByType(JavaPluginExtension::class.java).sourceSets
            cct.sourceDirectories.add(SourceSetReference.internal(sourceSets.getByName("main")))
        }
    }

    companion object {
        val JAVA_VERSION = JavaLanguageVersion.of(17)
    }
}
