// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package cc.tweaked.gradle

import net.minecraftforge.gradle.common.util.RunConfig
import net.minecraftforge.gradle.common.util.runs.setRunConfigInternal
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.JavaExec
import org.gradle.jvm.toolchain.JavaToolchainService
import java.nio.file.Files

/**
 * Set [JavaExec] task to run a given [RunConfig].
 */
fun JavaExec.setRunConfig(config: RunConfig) {
    dependsOn("prepareRuns")
    setRunConfigInternal(project, this, config)
    doFirst("Create working directory") { Files.createDirectories(workingDir.toPath()) }

    javaLauncher.set(
        project.extensions.getByType(JavaToolchainService::class.java)
            .launcherFor(project.extensions.getByType(JavaPluginExtension::class.java).toolchain),
    )
}
