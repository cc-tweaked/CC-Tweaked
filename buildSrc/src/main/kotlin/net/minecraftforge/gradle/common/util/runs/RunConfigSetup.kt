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
    fun lazyTokens(): MutableMap<String, Supplier<String>> {
        return RunConfigGenerator.configureTokensLazy(
            project, config, RunConfigGenerator.mapModClassesToGradle(project, config),
            originalTask.get().minecraftArtifacts.files,
            originalTask.get().runtimeClasspathArtifacts.files,
        )
    }
    spec.argumentProviders.add(
        CommandLineArgumentProvider {
            RunConfigGenerator.getArgsStream(config, lazyTokens(), false).toList()
        },
    )
    spec.jvmArgumentProviders.add(
        CommandLineArgumentProvider {
            val lazyTokens = lazyTokens()
            (if (config.isClient) config.jvmArgs + originalTask.get().additionalClientArgs.get() else config.jvmArgs).map { config.replace(lazyTokens, it) } +
                config.properties.map { (k, v) -> "-D${k}=${config.replace(lazyTokens, v)}" }
        },
    )

    // We can't configure environment variables lazily, so we do these now with a more minimal lazyTokens set.
    val lazyTokens = mutableMapOf<String, Supplier<String>>()
    for ((k, v) in config.tokens) lazyTokens[k] = Supplier<String> { v }
    for ((k, v) in config.lazyTokens) lazyTokens[k] = v
    lazyTokens.compute(
        "source_roots",
        { key: String, sourceRoots: Supplier<String>? ->
            Supplier<String> {
                val modClasses = RunConfigGenerator.mapModClassesToGradle(project, config)
                (if (sourceRoots != null) Stream.concat<String>(
                    sourceRoots.get().split(File.pathSeparator).stream(), modClasses,
                ) else modClasses).distinct().collect(Collectors.joining(File.pathSeparator))
            }
        },
    )
    for ((key, value) in config.environment) spec.environment(key, config.replace(lazyTokens, value))
}
