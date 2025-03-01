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
    id("net.neoforged.moddev")
}

plugins.apply(CCTweakedPlugin::class.java)

val mcVersion: String by extra

neoForge {
    val libs = project.extensions.getByType<VersionCatalogsExtension>().named("libs")
    version = libs.findVersion("neoForge").get().toString()

    parchment {
        minecraftVersion = libs.findVersion("parchmentMc").get().toString()
        mappingsVersion = libs.findVersion("parchment").get().toString()
    }
}

MinecraftConfigurations.setup(project)

extensions.configure(CCTweakedExtension::class.java) {
    linters(minecraft = true, loader = "forge")
}
