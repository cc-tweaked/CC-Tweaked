// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

pluginManagement {
    // Duplicated in build-logic/settings.gradle.kts
    repositories {
        mavenCentral()
        gradlePluginPortal()

        exclusiveContent {
            forRepositories(maven("https://maven.fabricmc.net/"))
            filter {
                includeGroup("fabric-loom")
                includeGroup("net.fabricmc")
                includeGroup("net.fabricmc.unpick")
            }
        }

        exclusiveContent {
            forRepositories(maven("https://maven.squiddev.cc"))
            filter {
                includeGroup("cc.tweaked.vanilla-extract")
            }
        }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version ("1.0.0")
}

enableFeaturePreview("STABLE_CONFIGURATION_CACHE")

rootProject.name = "cc-tweaked"

includeBuild("build-logic")

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
