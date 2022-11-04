package cc.tweaked.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.testing.jacoco.tasks.JacocoReport

/**
 * Configures projects to match a shared configuration.
 */
class CCTweakedPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val sourceSets = project.extensions.getByType(SourceSetContainer::class.java)

        val cct = project.extensions.create("cct", CCTweakedExtension::class.java)
        cct.sourceDirectories.add(SourceSetReference.internal(sourceSets.getByName("main")))

        project.afterEvaluate {
            cct.sourceDirectories.disallowChanges()
        }

        // Set up jacoco to read from /all/ our source directories.
        project.tasks.named("jacocoTestReport", JacocoReport::class.java) {
            for (ref in cct.sourceSets.get()) sourceDirectories.from(ref.allSource.sourceDirectories)
        }
    }

    companion object {
        val JAVA_VERSION = JavaLanguageVersion.of(17)
    }
}
