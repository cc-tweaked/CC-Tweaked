// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package cc.tweaked.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Dependency
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.AbstractExecTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class IlluaminateExtension {
    /** The version of illuaminate to use. */
    abstract val version: Property<String>

    /** The path to illuaminate. If not given, illuaminate will be downloaded automatically. */
    abstract val file: Property<File>
}

class IlluaminatePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create("illuaminate", IlluaminateExtension::class.java)
        extension.file.convention(setupDependency(project, extension.version))

        project.tasks.register(SetupIlluaminate.NAME, SetupIlluaminate::class.java) {
            file.set(extension.file.map { it.absolutePath })
        }
    }

    /** Set up a repository for illuaminate and download our binary from it. */
    private fun setupDependency(project: Project, version: Provider<String>): Provider<File> {
        project.repositories.ivy {
            name = "Illuaminate"
            setUrl("https://squiddev.cc/illuaminate/bin/")
            patternLayout {
                artifact("[revision]/[artifact]-[ext]")
            }
            metadataSources {
                artifact()
            }
            content {
                includeModule("cc.squiddev", "illuaminate")
            }
        }

        return version.map {
            val dep = illuaminateArtifact(project, it)
            val configuration = project.configurations.detachedConfiguration(dep)
            configuration.isTransitive = false
            configuration.resolve().single()
        }
    }

    /** Define a dependency for illuaminate from a version number and the current operating system. */
    private fun illuaminateArtifact(project: Project, version: String): Dependency {
        val osName = System.getProperty("os.name").lowercase()
        val (os, suffix) = when {
            osName.contains("windows") -> Pair("windows", ".exe")
            osName.contains("mac os") || osName.contains("darwin") -> Pair("macos", "")
            osName.contains("linux") -> Pair("linux", "")
            else -> error("Unsupported OS $osName for illuaminate")
        }

        val osArch = System.getProperty("os.arch").lowercase()
        val arch = when {
            // On macOS the x86_64 binary will work for both ARM and Intel Macs through Rosetta.
            os == "macos" -> "x86_64"
            osArch == "arm" || osArch.startsWith("aarch") -> error("Unsupported architecture '$osArch' for illuaminate")
            osArch.contains("64") -> "x86_64"
            else -> error("Unsupported architecture '$osArch' for illuaminate")
        }

        return project.dependencies.create(
            mapOf(
                "group" to "cc.squiddev",
                "name" to "illuaminate",
                "version" to version,
                "ext" to "$os-$arch$suffix",
            ),
        )
    }
}

private val Task.illuaminatePath: String? // "?" needed to avoid overload ambiguity in setExecutable below.
    get() = project.extensions.getByType(IlluaminateExtension::class.java).file.get().absolutePath

/** Prepares illuaminate for being run. This simply requests the dependency and then marks it as executable. */
abstract class SetupIlluaminate : DefaultTask() {
    @get:Input
    abstract val file: Property<String>

    @TaskAction
    fun setExecutable() {
        val file = File(this.file.get())
        if (file.canExecute()) {
            didWork = false
            return
        }

        file.setExecutable(true)
    }

    companion object {
        const val NAME: String = "setupIlluaminate"
    }
}

abstract class IlluaminateExec : AbstractExecTask<IlluaminateExec>(IlluaminateExec::class.java) {
    init {
        dependsOn(SetupIlluaminate.NAME)
        executable = illuaminatePath
    }
}

abstract class IlluaminateExecToDir : ExecToDir() {
    init {
        dependsOn(SetupIlluaminate.NAME)
        executable = illuaminatePath
    }
}
