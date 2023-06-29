// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package net.minecraftforge.gradle.common.util.runs

import net.minecraftforge.gradle.common.util.RunConfig
import org.gradle.api.Project
import org.gradle.process.JavaExecSpec
import java.io.File

/**
 * Set up a [JavaExecSpec] to execute a [RunConfig].
 *
 * [MinecraftRunTask] sets up all its properties when the task is executed, rather than when configured. As such, it's
 * not possible to use [cc.tweaked.gradle.copyToFull] like we do for Fabric. Instead, we set up the task manually.
 *
 * Unfortunately most of the functionality we need is package-private, and so we have to put our code into the package.
 */
internal fun setRunConfigInternal(project: Project, spec: JavaExecSpec, config: RunConfig) {
    val originalTask = project.tasks.named(config.taskName, MinecraftRunTask::class.java).get()

    spec.workingDir = File(config.workingDirectory)

    spec.mainClass.set(config.main)
    for (source in config.allSources) spec.classpath(source.runtimeClasspath)

    val lazyTokens = RunConfigGenerator.configureTokensLazy(
        project, config,
        RunConfigGenerator.mapModClassesToGradle(project, config),
        originalTask.minecraftArtifacts.files,
        originalTask.runtimeClasspathArtifacts.files,
    )

    spec.args(RunConfigGenerator.getArgsStream(config, lazyTokens, false).toList())

    spec.jvmArgs(
        (if (config.isClient) config.jvmArgs + originalTask.additionalClientArgs.get() else config.jvmArgs)
            .map { config.replace(lazyTokens, it) },
    )

    for ((key, value) in config.environment) spec.environment(key, config.replace(lazyTokens, value))
    for ((key, value) in config.properties) spec.systemProperty(key, config.replace(lazyTokens, value))
}
