// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

/** Default configuration for Fabric projects. */

import cc.tweaked.gradle.CCTweakedExtension
import cc.tweaked.gradle.DependencyCheck
import cc.tweaked.gradle.MinecraftConfigurations

plugins {
    `java-library`
    id("net.fabricmc.fabric-loom-remap")
    id("cc-tweaked.java-convention")
}

repositories {
    maven("https://maven.parchmentmc.org/") {
        name = "Parchment"
        content {
            includeGroup("org.parchmentmc.data")
        }
    }
}

loom {
    splitEnvironmentSourceSets()
    splitModDependencies = true
}

MinecraftConfigurations.setup(project)

val cct = extensions.getByType(CCTweakedExtension::class.java)
cct.linters(minecraft = true)

dependencies {
    val libs = project.extensions.getByType<VersionCatalogsExtension>().named("libs")

    minecraft("com.mojang:minecraft:${libs.findVersion("minecraft").get()}")
    mappings(
        loom.layered {
            officialMojangMappings()
            parchment(
                dependencyFactory.create(
                    "org.parchmentmc.data",
                    "parchment-${libs.findVersion("parchmentMc").get()}",
                    libs.findVersion("parchment").get().toString(),
                    null,
                    "zip",
                ),
            )
        },
    )

    modImplementation(libs.findLibrary("fabric-loader").get())
    modImplementation(libs.findLibrary("fabric-api").get())

    // Depend on error prone annotations to silence a lot of compile warnings.
    compileOnlyApi(libs.findLibrary("errorProne.annotations").get())
}

tasks.named("checkDependencyConsistency", DependencyCheck::class.java) {
    val libs = project.extensions.getByType<VersionCatalogsExtension>().named("libs")
    // Minecraft depends on lwjgl, but Fabric forces it to a more recent version
    for (lwjgl in listOf(
        "lwjgl",
        "lwjgl-glfw",
        "lwjgl-jemalloc",
        "lwjgl-openal",
        "lwjgl-opengl",
        "lwjgl-stb",
        "lwjgl-tinyfd",
    )) {
        override("org.lwjgl", lwjgl, "3.3.2")
    }
}

/**
 * Given our list of source/class directories, build a list of files in the "client" source set.
 */
fun getClientFiles(files: FileCollection): List<String> {
    val out = mutableListOf<String>()
    files
        // This is a massive hack. *Ideally* we'd like to filter our embedded projects list to those with a client
        // capability, but I cannot find a sensible way to do that.
        // Instead, we look for directories which are probably "build/classes/XXX/client" or "src/client/XXX"
        .filter { dir -> dir.name == "client" || dir.parentFile.name == "client" }
        .asFileTree.visit { if (!isDirectory) out.add(path) }
    return out
}

tasks.remapJar {
    additionalClientOnlyEntries.addAll(cct.embeddedProjectClassesAndResources.map(::getClientFiles))
}

tasks.remapSourcesJar {
    additionalClientOnlyEntries.addAll(cct.embeddedProjectSources.map(::getClientFiles))
}
