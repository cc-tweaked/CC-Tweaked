// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

pluginManagement {
    // Duplicated in buildSrc/build.gradle.kts
    repositories {
        mavenCentral()
        gradlePluginPortal()

        maven("https://maven.fabricmc.net/") {
            name = "Fabric"
            content {
                includeGroup("fabric-loom")
                includeGroup("net.fabricmc")
                includeModule("org.jetbrains", "intellij-fernflower")
            }
        }

        maven("https://repo.sleeping.town") {
            name = "Voldeloom"
            content {
                includeGroup("agency.highlysuspect")
            }
        }
    }

    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "agency.highlysuspect.voldeloom") {
                useModule("agency.highlysuspect:voldeloom:${requested.version}")
            }
        }
    }
}

rootProject.name = "cc-tweaked"

// includeBuild("vendor/Cobalt")
