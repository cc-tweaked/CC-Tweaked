// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

import cc.tweaked.gradle.setProvider

plugins {
    id("com.modrinth.minotaur")
    id("cc-tweaked.publishing")
}

abstract class ModPublishingExtension {
    abstract val output: Property<AbstractArchiveTask>

    init {
        output.finalizeValueOnRead()
    }
}

val modPublishing = project.extensions.create("modPublishing", ModPublishingExtension::class.java)

val isUnstable = project.properties["isUnstable"] == "true"
val modVersion: String by extra
val mcVersion = project.extensions.getByType<VersionCatalogsExtension>().named("libs")
    .findVersion("minecraft").get().toString()

modrinth {
    token = findProperty("modrinthApiKey") as String? ?: ""
    projectId = "gu7yAYhd"
    versionNumber = modVersion
    versionName = modVersion
    versionType = if (isUnstable) "alpha" else "release"
    uploadFile.setProvider(modPublishing.output)
    gameVersions.add(mcVersion)
    changelog = "Release notes can be found on the [GitHub repository](https://github.com/cc-tweaked/CC-Tweaked/releases/tag/v$mcVersion-$modVersion)."

    syncBodyFrom = provider { rootProject.file("doc/mod-page.md").readText() }
}

tasks.publish { dependsOn(tasks.modrinth) }
