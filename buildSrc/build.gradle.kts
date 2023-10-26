// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
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

    maven("https://repo.spongepowered.org/repository/maven-public/") {
        name = "Sponge"
        content {
            includeGroup("org.spongepowered")
        }
    }

    maven("https://maven.fabricmc.net/") {
        name = "Fabric"
        content {
            includeGroup("net.fabricmc")
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
    implementation(libs.vanillaGradle)
    implementation(libs.vineflower)
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
