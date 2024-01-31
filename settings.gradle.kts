// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

pluginManagement {
    // Duplicated in buildSrc/build.gradle.kts
    repositories {
        mavenCentral()
        gradlePluginPortal()

        maven("https://maven.neoforged.net/releases") {
            name = "NeoForge"
            content {
                includeGroup("net.minecraftforge")
                includeGroup("net.neoforged")
                includeGroup("net.neoforged.gradle")
                includeModule("codechicken", "DiffPatch")
                includeModule("net.covers1624", "Quack")
            }
        }

        maven("https://maven.fabricmc.net/") {
            name = "Fabric"
            content {
                includeGroup("fabric-loom")
                includeGroup("net.fabricmc")
            }
        }

        maven("https://squiddev.cc/maven") {
            name = "SquidDev"
            content {
                includeGroup("cc.tweaked.vanilla-extract")
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
