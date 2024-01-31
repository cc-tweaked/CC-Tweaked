// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package cc.tweaked.gradle

import net.neoforged.gradle.common.runs.run.RunImpl
import net.neoforged.gradle.common.runs.tasks.RunExec
import net.neoforged.gradle.dsl.common.extensions.RunnableSourceSet
import net.neoforged.gradle.dsl.common.runs.run.Run
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.SourceSet
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.findByType
import java.nio.file.Files

/**
 * Set [JavaExec] task to run a given [RunConfig].
 *
 * See also [RunExec].
 */
fun JavaExec.setRunConfig(config: Run) {
    mainClass.set(config.mainClass)
    workingDir = config.workingDirectory.get().asFile
    argumentProviders.add { config.programArguments.get() }
    jvmArgumentProviders.add { config.jvmArguments.get() }

    environment(config.environmentVariables.get())
    systemProperties(config.systemProperties.get())

    config.modSources.get().forEach { classpath(it.runtimeClasspath) }
    classpath(config.classpath)
    classpath(config.dependencies.get().configuration)

    (config as RunImpl).taskDependencies.forEach { dependsOn(it) }

    javaLauncher.set(
        project.extensions.getByType(JavaToolchainService::class.java)
            .launcherFor(project.extensions.getByType(JavaPluginExtension::class.java).toolchain),
    )

    doFirst("Create working directory") { Files.createDirectories(workingDir.toPath()) }
}

/**
 * Add a new [Run.modSource] with a specific mod id.
 */
fun Run.modSourceAs(sourceSet: SourceSet, mod: String) {
    // NeoGradle requires a RunnableSourceSet to be present, so we inject it into other project's source sets.
    val runnable = sourceSet.extensions.findByType<RunnableSourceSet>() ?: run {
        val extension = sourceSet.extensions.create<RunnableSourceSet>(RunnableSourceSet.NAME, project)
        extension.modIdentifier = mod
        extension.modIdentifier.finalizeValueOnRead()
        extension
    }
    if (runnable.modIdentifier.get() != mod) throw IllegalArgumentException("Multiple mod identifiers")

    modSource(sourceSet)
}
