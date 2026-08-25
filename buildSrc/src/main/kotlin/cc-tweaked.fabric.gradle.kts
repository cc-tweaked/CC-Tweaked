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
    id("net.fabricmc.fabric-loom")
    id("cc-tweaked.java-convention")
}

plugins.apply(CCTweakedPlugin::class.java)

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

    minecraft("com.mojang:minecraft:${libs.findVersion("minecraft").get().toString()}")

    implementation(libs.findLibrary("fabric-loader").get())
    implementation(libs.findLibrary("fabric-api").get())

    // Depend on error prone annotations to silence a lot of compile warnings.
    compileOnly(libs.findLibrary("errorProne.annotations").get())
}
