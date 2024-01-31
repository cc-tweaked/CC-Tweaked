// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

/** Default configuration for Forge projects. */

import cc.tweaked.gradle.CCTweakedExtension
import cc.tweaked.gradle.CCTweakedPlugin
import cc.tweaked.gradle.IdeaRunConfigurations
import cc.tweaked.gradle.MinecraftConfigurations

plugins {
    id("cc-tweaked.java-convention")
    id("net.neoforged.gradle.userdev")
}

plugins.apply(CCTweakedPlugin::class.java)

val mcVersion: String by extra

minecraft {
    modIdentifier("computercraft")
}

subsystems {
    parchment {
        val libs = project.extensions.getByType<VersionCatalogsExtension>().named("libs")
        minecraftVersion = libs.findVersion("parchmentMc").get().toString()
        mappingsVersion = libs.findVersion("parchment").get().toString()
    }
}

dependencies {
    val libs = project.extensions.getByType<VersionCatalogsExtension>().named("libs")
    implementation("net.neoforged:neoforge:${libs.findVersion("neoForge").get()}")
}

MinecraftConfigurations.setup(project)

extensions.configure(CCTweakedExtension::class.java) {
    linters(minecraft = true, loader = "forge")
}
