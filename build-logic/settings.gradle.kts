// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }

    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS

    // Duplicated in root settings.gradle.kts
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

rootProject.name = "build-logic"

include("base")
