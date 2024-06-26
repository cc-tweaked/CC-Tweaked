// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

pluginManagement {
    // Duplicated in buildSrc/build.gradle.kts
    repositories {
        mavenCentral()
        gradlePluginPortal()

        maven("https://maven.minecraftforge.net") {
            name = "Forge"
            content {
                includeGroup("net.minecraftforge")
                includeGroup("net.minecraftforge.gradle")
            }
        }

        maven("https://maven.parchmentmc.org") {
            name = "Librarian"
            content {
                includeGroupByRegex("^org\\.parchmentmc.*")
            }
        }

        maven("https://repo.spongepowered.org/repository/maven-public/") {
            name = "Sponge"
            content {
                includeGroup("org.spongepowered")
            }
        }

        maven("https://maven.fabricmc.net/") {
            name = "Fabric"
            content {
                includeGroup("fabric-loom")
                includeGroup("net.fabricmc")
            }
        }

        maven("https://maven.squiddev.cc") {
            name = "SquidDev"
            content {
                includeGroup("cc.tweaked.vanilla-extract")
            }
        }
    }

    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "org.spongepowered.mixin") {
                useModule("org.spongepowered:mixingradle:${requested.version}")
            }
        }
    }
}

val mcVersion: String by settings
rootProject.name = "cc-tweaked-$mcVersion"

include(":core-api")
include(":core")

include(":common-api")
include(":common")
include(":fabric-api")
include(":fabric")
include(":forge-api")
include(":forge")

include(":lints")
include(":standalone")
include(":web")

for (project in rootProject.children) {
    project.projectDir = file("projects/${project.name}")
}
