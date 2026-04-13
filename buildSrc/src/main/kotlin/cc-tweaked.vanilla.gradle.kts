// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

/** Default configuration for non-modloader-specific Minecraft projects. */

import cc.tweaked.gradle.CCTweakedExtension
import cc.tweaked.gradle.CCTweakedPlugin
import cc.tweaked.gradle.MinecraftConfigurations

plugins {
    id("cc-tweaked.java-convention")
    id("net.fabricmc.fabric-loom")
}

plugins.apply(CCTweakedPlugin::class.java)

val mcVersion: String by extra

val libs = project.extensions.getByType<VersionCatalogsExtension>().named("libs")

loom {
    splitEnvironmentSourceSets()
    splitModDependencies = true
}

dependencies {
    minecraft("com.mojang:minecraft:$mcVersion")

    // Depend on error prone annotations to silence a lot of compile warnings.
    compileOnly(libs.findLibrary("errorProne.annotations").get())
}

MinecraftConfigurations.setup(project)

extensions.configure(CCTweakedExtension::class.java) {
    linters(minecraft = true, loader = null)
}
