// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

/** Default configuration for non-modloader-specific Minecraft projects. */

import cc.tweaked.gradle.CCTweakedExtension
import cc.tweaked.gradle.MinecraftConfigurations

plugins {
    id("cc-tweaked.java-convention")
    id("net.fabricmc.fabric-loom")
}

loom {
    splitEnvironmentSourceSets()
    splitModDependencies = true
}


MinecraftConfigurations.setup(project)

extensions.configure(CCTweakedExtension::class.java) {
    linters(minecraft = true)
}

dependencies {
    val libs = project.extensions.getByType<VersionCatalogsExtension>().named("libs")

    minecraft("com.mojang:minecraft:${libs.findVersion("minecraft").get().toString()}")

    // Depend on error prone annotations to silence a lot of compile warnings.
    compileOnly(libs.findLibrary("errorProne.annotations").get())
}
