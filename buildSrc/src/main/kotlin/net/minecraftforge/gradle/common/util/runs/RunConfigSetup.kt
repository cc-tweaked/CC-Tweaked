// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package net.minecraftforge.gradle.common.util.runs

import net.minecraftforge.gradle.common.util.RunConfig
import org.gradle.api.Project
import org.gradle.process.CommandLineArgumentProvider
import org.gradle.process.JavaExecSpec
import java.io.File
import java.util.function.Supplier
import java.util.stream.Collectors
import java.util.stream.Stream

/**
 * Set up a [JavaExecSpec] to execute a [RunConfig].
 *
 * [MinecraftRunTask] sets up all its properties when the task is executed, rather than when configured. As such, it's
 * not possible to use [cc.tweaked.gradle.copyToFull] like we do for Fabric. Instead, we set up the task manually.
 *
 * Unfortunately most of the functionality we need is package-private, and so we have to put our code into the package.
 */
internal fun setRunConfigInternal(project: Project, spec: JavaExecSpec, config: RunConfig) {
    spec.workingDir = File(config.workingDirectory)

    spec.mainClass.set(config.main)
    for (source in config.allSources) spec.classpath(source.runtimeClasspath)

    val originalTask = project.tasks.named(config.taskName, MinecraftRunTask::class.java)

    // Add argument and JVM argument via providers, to be as lazy as possible with fetching artifacts.
    val lazyTokens = RunConfigGenerator.configureTokensLazy(
        project, config, RunConfigGenerator.mapModClassesToGradle(project, config),
        originalTask.get().minecraftArtifacts,
        originalTask.get().runtimeClasspathArtifacts,
    )
    spec.argumentProviders.add(
        CommandLineArgumentProvider {
            RunConfigGenerator.getArgsStream(config, lazyTokens, false).toList()
        },
    )
    spec.jvmArgumentProviders.add(
        CommandLineArgumentProvider {
            (if (config.isClient) config.jvmArgs + originalTask.get().additionalClientArgs.get() else config.jvmArgs).map { config.replace(lazyTokens, it) } +
                config.properties.map { (k, v) -> "-D${k}=${config.replace(lazyTokens, v)}" }
        },
    )

    for ((key, value) in config.environment) spec.environment(key, config.replace(lazyTokens, value))
}
