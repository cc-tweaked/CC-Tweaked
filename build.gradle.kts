// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

import cc.tweaked.gradle.JUnitExt
import net.fabricmc.loom.api.LoomGradleExtensionAPI
import net.fabricmc.loom.util.gradle.SourceSetHelper
import org.jetbrains.gradle.ext.*
import org.jetbrains.gradle.ext.Application

plugins {
    publishing
    alias(libs.plugins.taskTree)
    alias(libs.plugins.githubRelease)
    alias(libs.plugins.gradleVersions)
    alias(libs.plugins.versionCatalogUpdate)
    id("org.jetbrains.gradle.plugin.idea-ext")
    id("cc-tweaked")
}

val isUnstable = project.properties["isUnstable"] == "true"
val modVersion: String by extra
val mcVersion: String by extra

githubRelease {
    token(findProperty("githubApiKey") as String? ?: "")
    owner.set("cc-tweaked")
    repo.set("CC-Tweaked")
    targetCommitish.set(cct.gitBranch)

    tagName.set("v$mcVersion-$modVersion")
    releaseName.set("[$mcVersion] $modVersion")
    body.set(
        provider {
            "## " + project(":core").file("src/main/resources/data/computercraft/lua/rom/help/whatsnew.md")
                .readLines()
                .takeWhile { it != "Type \"help changelog\" to see the full version history." }
                .joinToString("\n").trim()
        },
    )
    prerelease.set(isUnstable)
}

tasks.publish { dependsOn(tasks.githubRelease) }

idea.project.settings.runConfigurations {
    register<JUnitExt>("Core Tests") {
        vmParameters = "-ea"
        moduleName = "${idea.project.name}.core.test"
        packageName = ""
    }

    register<JUnitExt>("CraftOS Tests") {
        vmParameters = "-ea"
        moduleName = "${idea.project.name}.core.test"
        className = "dan200.computercraft.core.ComputerTestDelegate"
    }

    register<JUnitExt>("CraftOS Tests (Fast)") {
        vmParameters = "-ea -Dcc.skip_keywords=slow"
        moduleName = "${idea.project.name}.core.test"
        className = "dan200.computercraft.core.ComputerTestDelegate"
    }

    register<JUnitExt>("Common Tests") {
        vmParameters = "-ea"
        moduleName = "${idea.project.name}.common.test"
        packageName = ""
    }

    register<JUnitExt>("Fabric Tests") {
        val fabricProject = evaluationDependsOn(":fabric")
        val classPathGroup = fabricProject.extensions.getByType<LoomGradleExtensionAPI>().mods
            .joinToString(File.pathSeparator + File.pathSeparator) { modSettings ->
                SourceSetHelper.getClasspath(modSettings, project).joinToString(File.pathSeparator) { it.absolutePath }
            }

        vmParameters = "-ea -Dfabric.classPathGroups=$classPathGroup"
        moduleName = "${idea.project.name}.fabric.test"
        packageName = ""
    }

    register<JUnitExt>("Forge Tests") {
        vmParameters = "-ea"
        moduleName = "${idea.project.name}.forge.test"
        packageName = ""
    }

    register<Application>("Standalone") {
        moduleName = "${idea.project.name}.standalone.main"
        mainClass = "cc.tweaked.standalone.Main"
        programParameters = "--resources=projects/core/src/main/resources --term=80x30 --allow-local-domains"
    }
}

// Build with the IntelliJ, rather than through Gradle. This may require setting the "Compiler Output" option in
// "Project Structure".
idea.project.settings.delegateActions {
    delegateBuildRunToGradle = false
    testRunner = ActionDelegationConfig.TestRunner.PLATFORM
}

idea.project.settings.compiler.javac {
    // We want ErrorProne to be present when compiling via IntelliJ, as it offers some helpful warnings
    // and errors. Loop through our source sets and find the appropriate flags.
    moduleJavacAdditionalOptions = subprojects
        .asSequence()
        .map { evaluationDependsOn(it.path) }
        .flatMap { project ->
            val sourceSets = project.extensions.findByType(SourceSetContainer::class) ?: return@flatMap sequenceOf()
            sourceSets.asSequence().map { sourceSet ->
                val name = "${idea.project.name}.${project.name}.${sourceSet.name}"
                val compile = project.tasks.named(sourceSet.compileJavaTaskName, JavaCompile::class).get()
                name to compile.options.allCompilerArgs.joinToString(" ") { if (it.contains(" ")) "\"$it\"" else it }
            }
        }
        .toMap()
}

versionCatalogUpdate {
    sortByKey.set(false)
    pin { versions.addAll("fastutil", "guava", "netty", "slf4j") }
    keep { keepUnusedLibraries.set(true) }
}
