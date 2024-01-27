// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

/** Default configuration for Fabric projects. */

import cc.tweaked.gradle.CCTweakedExtension
import cc.tweaked.gradle.CCTweakedPlugin
import cc.tweaked.gradle.IdeaRunConfigurations
import cc.tweaked.gradle.MinecraftConfigurations

plugins {
    `java-library`
    id("fabric-loom")
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
    splitModDependencies.set(true)
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
                project.dependencies.create(
                    group = "org.parchmentmc.data",
                    name = "parchment-${libs.findVersion("parchmentMc").get()}",
                    version = libs.findVersion("parchment").get().toString(),
                    ext = "zip",
                ),
            )
        },
    )

    modImplementation(libs.findLibrary("fabric-loader").get())
    modImplementation(libs.findLibrary("fabric-api").get())

    // Depend on error prone annotations to silence a lot of compile warnings.
    compileOnlyApi(libs.findLibrary("errorProne.annotations").get())
}

tasks.ideaSyncTask {
    doLast { IdeaRunConfigurations(project).patch() }
}
