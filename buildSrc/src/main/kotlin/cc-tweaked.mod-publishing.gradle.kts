// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

import net.darkhax.curseforgegradle.TaskPublishCurseForge
import cc.tweaked.gradle.setProvider

plugins {
    id("net.darkhax.curseforgegradle")
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
val mcVersion: String by extra

val publishCurseForge by tasks.registering(TaskPublishCurseForge::class) {
    group = PublishingPlugin.PUBLISH_TASK_GROUP
    description = "Upload artifacts to CurseForge"

    apiToken = findProperty("curseForgeApiKey") ?: ""
    enabled = apiToken != ""

    val mainFile = upload("282001", modPublishing.output)
    mainFile.changelog =
        "Release notes can be found on the [GitHub repository](https://github.com/cc-tweaked/CC-Tweaked/releases/tag/v$mcVersion-$modVersion)."
    mainFile.changelogType = "markdown"
    mainFile.releaseType = if (isUnstable) "alpha" else "release"
    mainFile.gameVersions.add(mcVersion)
}

tasks.publish { dependsOn(publishCurseForge) }

modrinth {
    token.set(findProperty("modrinthApiKey") as String? ?: "")
    projectId.set("gu7yAYhd")
    versionNumber.set(modVersion)
    versionName.set(modVersion)
    versionType.set(if (isUnstable) "alpha" else "release")
    uploadFile.setProvider(modPublishing.output)
    gameVersions.add(mcVersion)
    changelog.set("Release notes can be found on the [GitHub repository](https://github.com/cc-tweaked/CC-Tweaked/releases/tag/v$mcVersion-$modVersion).")

    syncBodyFrom.set(provider { rootProject.file("doc/mod-page.md").readText() })
}

tasks.publish { dependsOn(tasks.modrinth) }
