// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

import org.jetbrains.gradle.ext.compiler
import org.jetbrains.gradle.ext.settings

plugins {
    publishing
    alias(libs.plugins.taskTree)
    alias(libs.plugins.githubRelease)
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
