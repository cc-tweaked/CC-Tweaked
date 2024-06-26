// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    alias(libs.plugins.gradleVersions)
    alias(libs.plugins.versionCatalogUpdate)
}

// Duplicated in settings.gradle.kts
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

    maven("https://maven.fabricmc.net/") {
        name = "Fabric"
        content {
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

dependencies {
    implementation(libs.errorProne.plugin)
    implementation(libs.kotlin.plugin)
    implementation(libs.spotless)

    implementation(libs.curseForgeGradle)
    implementation(libs.fabric.loom)
    implementation(libs.forgeGradle)
    implementation(libs.ideaExt)
    implementation(libs.librarian)
    implementation(libs.minotaur)
    implementation(libs.vanillaExtract)
}

gradlePlugin {
    plugins {
        register("cc-tweaked") {
            id = "cc-tweaked"
            implementationClass = "cc.tweaked.gradle.CCTweakedPlugin"
        }

        register("cc-tweaked.illuaminate") {
            id = "cc-tweaked.illuaminate"
            implementationClass = "cc.tweaked.gradle.IlluaminatePlugin"
        }

        register("cc-tweaked.node") {
            id = "cc-tweaked.node"
            implementationClass = "cc.tweaked.gradle.NodePlugin"
        }
    }
}

versionCatalogUpdate {
    sortByKey.set(false)
    keep { keepUnusedLibraries.set(true) }
    catalogFile.set(file("../gradle/libs.versions.toml"))
}
