// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

/** Default configuration for Forge projects. */

import cc.tweaked.gradle.CCTweakedExtension
import cc.tweaked.gradle.CCTweakedPlugin
import cc.tweaked.gradle.MinecraftConfigurations

plugins {
    id("cc-tweaked.java-convention")
    id("net.neoforged.moddev.legacyforge")
}

plugins.apply(CCTweakedPlugin::class.java)

val mcVersion: String by extra

legacyForge {
    val libs = project.extensions.getByType<VersionCatalogsExtension>().named("libs")
    enable {
        forgeVersion = "$mcVersion-${libs.findVersion("forge").get()}"
        // This causes parameters to not be renamed, which causes issues with ErrorProne.
        // See https://github.com/google/error-prone/issues/5398.
        isDisableRecompilation = false
    }

    parchment {
        minecraftVersion = libs.findVersion("parchmentMc").get().toString()
        mappingsVersion = libs.findVersion("parchment").get().toString()
    }
}

MinecraftConfigurations.setup(project)

extensions.configure(CCTweakedExtension::class.java) {
    linters(minecraft = true, loader = "forge")
}
