// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

import cc.tweaked.gradle.ExternalProjectArtifacts
import cc.tweaked.gradle.VerifyJavaCompile
import cc.tweaked.gradle.setProvider
import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.errorprone.errorprone

plugins {
    id("com.modrinth.minotaur")
    id("cc-tweaked.java-convention")
    id("cc-tweaked.publishing")
}

// Create a new "minecraftEmbeddedProject" configuration that the main embeddedProject extends from, which just contains
// Minecraft specific classes. We use this to recompile all Minecraft-facing code.
val minecraftEmbeddedProject = configurations.dependencyScope("minecraftEmbeddedProject")
configurations.named("embeddedProjectResolved") { extendsFrom(minecraftEmbeddedProject) }
val minecraftEmbeddedProjectResolved = configurations.resolvable("minecraftEmbeddedProjectResolved") {
    extendsFrom(minecraftEmbeddedProject)
    isTransitive = false
    ExternalProjectArtifacts.configure(this)
}

abstract class ModPublishingExtension {
    abstract val output: Property<AbstractArchiveTask>

    init {
        output.finalizeValueOnRead()
    }
}

val modPublishing = project.extensions.create("modPublishing", ModPublishingExtension::class.java)

val isUnstable = extra["isUnstable"] == "true"
val mcVersion = project.extensions.getByType<VersionCatalogsExtension>().named("libs")
    .findVersion("minecraft").get().toString()

val verifyClassesMatch = tasks.register<VerifyJavaCompile>("verifyClassesMatch") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Verify compiling common sources against the loader produces the same classes"

    val clientSources = sourceSets["client"]

    destinationDirectory = layout.buildDirectory.dir(name)
    classpath = clientSources.compileClasspath
    options.annotationProcessorPath = clientSources.annotationProcessorPath

    options.errorprone {
        isEnabled = true
        // Check loader override in the common code.
        check("MissingOverride", CheckSeverity.OFF)
        check("LoaderOverride", CheckSeverity.DEFAULT)
        check("MissingLoaderOverride", CheckSeverity.DEFAULT)
    }

    source = minecraftEmbeddedProjectResolved.map {
        (ExternalProjectArtifacts.sources(it) + sourceSets.main.get().allJava + clientSources.allJava).asFileTree
    }.get()
    referenceDirectories.from(minecraftEmbeddedProjectResolved.map(ExternalProjectArtifacts::classes))
    referenceDirectories.from(sourceSets.main.get().output.classesDirs)
    referenceDirectories.from(clientSources.output.classesDirs)
}

tasks.check { dependsOn(verifyClassesMatch) }

modrinth {
    val modVersion = project.version as String
    token = findProperty("modrinthApiKey") as String? ?: ""
    projectId = "gu7yAYhd"
    versionNumber = modVersion
    versionName = modVersion
    versionType = if (isUnstable) "alpha" else "release"
    uploadFile.setProvider(modPublishing.output)
    gameVersions.add(mcVersion)
    changelog =
        "Release notes can be found on the [GitHub repository](https://github.com/cc-tweaked/CC-Tweaked/releases/tag/v$mcVersion-$modVersion)."

    syncBodyFrom = provider { rootProject.file("doc/mod-page.md").readText() }
}

tasks.publish { dependsOn(tasks.modrinth) }
