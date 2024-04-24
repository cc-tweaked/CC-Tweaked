// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package cc.tweaked.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import java.io.File

class NodePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create("node", NodeExtension::class.java)
        project.tasks.register(NpmInstall.TASK_NAME, NpmInstall::class.java) {
            projectRoot.convention(extension.projectRoot)
        }
    }
}

abstract class NodeExtension(project: Project) {
    /** The directory containing `package-lock.json` and `node_modules/`. */
    abstract val projectRoot: DirectoryProperty

    init {
        projectRoot.convention(project.layout.projectDirectory)
    }
}

/** Installs node modules as dependencies. */
abstract class NpmInstall : DefaultTask() {
    @get:Internal
    abstract val projectRoot: DirectoryProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    val packageLock: Provider<File> = projectRoot.file("package-lock.json").map { it.asFile }

    @get:OutputDirectory
    val nodeModules: Provider<Directory> = projectRoot.dir("node_modules")

    @TaskAction
    fun install() {
        project.exec {
            commandLine(ProcessHelpers.getExecutable("npm"), "ci")
            workingDir = projectRoot.get().asFile
        }
    }

    companion object {
        internal const val TASK_NAME: String = "npmInstall"
    }
}

abstract class NpxExecToDir : ExecToDir() {
    init {
        dependsOn(NpmInstall.TASK_NAME)
        executable = ProcessHelpers.getExecutable("npx")
    }
}
