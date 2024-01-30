// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package cc.tweaked.gradle

import net.neoforged.gradle.common.runs.run.RunImpl
import net.neoforged.gradle.common.runs.tasks.RunExec
import net.neoforged.gradle.dsl.common.runs.run.Run
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.JavaExec
import org.gradle.jvm.toolchain.JavaToolchainService

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
}
