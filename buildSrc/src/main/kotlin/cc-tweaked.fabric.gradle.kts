// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

/** Default configuration for Fabric projects. */

import cc.tweaked.gradle.CCTweakedExtension
import cc.tweaked.gradle.CCTweakedPlugin
import cc.tweaked.gradle.DependencyCheck
import cc.tweaked.gradle.MinecraftConfigurations

plugins {
    `java-library`
    id("net.fabricmc.fabric-loom-remap")
    id("cc-tweaked.java-convention")
}

plugins.apply(CCTweakedPlugin::class.java)

val mcVersion: String by extra

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

extensions.configure(CCTweakedExtension::class.java) {
    linters(minecraft = true, loader = "fabric")
}

dependencies {
    val libs = project.extensions.getByType<VersionCatalogsExtension>().named("libs")

    minecraft("com.mojang:minecraft:$mcVersion")
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
