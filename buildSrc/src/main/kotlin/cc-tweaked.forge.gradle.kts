// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

/** Default configuration for Forge projects. */

import cc.tweaked.gradle.CCTweakedExtension
import cc.tweaked.gradle.CCTweakedPlugin
import cc.tweaked.gradle.IdeaRunConfigurations
import cc.tweaked.gradle.MinecraftConfigurations

plugins {
    id("net.minecraftforge.gradle")
    // We must apply java-convention after Forge, as we need the fg extension to be present.
    id("cc-tweaked.java-convention")
    id("org.parchmentmc.librarian.forgegradle")
}

plugins.apply(CCTweakedPlugin::class.java)

val mcVersion: String by extra

minecraft {
    val libs = project.extensions.getByType<VersionCatalogsExtension>().named("libs")
    mappings("parchment", "${libs.findVersion("parchmentMc").get()}-${libs.findVersion("parchment").get()}-$mcVersion")

    accessTransformer(project(":forge").file("src/main/resources/META-INF/accesstransformer.cfg"))
}

MinecraftConfigurations.setup(project)

extensions.configure(CCTweakedExtension::class.java) {
    linters(minecraft = true, loader = "forge")
}

dependencies {
    val libs = project.extensions.getByType<VersionCatalogsExtension>().named("libs")
    "minecraft"("net.minecraftforge:forge:$mcVersion-${libs.findVersion("forge").get()}")
}

tasks.configureEach {
    // genIntellijRuns isn't registered until much later, so we need this silly hijinks.
    if (name == "genIntellijRuns") doLast { IdeaRunConfigurations(project).patch() }
}
