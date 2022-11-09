/** Default configuration for Forge projects. */

import cc.tweaked.gradle.CCTweakedExtension
import cc.tweaked.gradle.CCTweakedPlugin
import cc.tweaked.gradle.IdeaRunConfigurations
import cc.tweaked.gradle.MinecraftConfigurations

plugins {
    id("cc-tweaked.java-convention")
    id("net.minecraftforge.gradle")
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

dependencies {
    val libs = project.extensions.getByType<VersionCatalogsExtension>().named("libs")
    "minecraft"("net.minecraftforge:forge:$mcVersion-${libs.findVersion("forge").get()}")
}

tasks.configureEach {
    // genIntellijRuns isn't registered until much later, so we need this silly hijinks.
    if (name == "genIntellijRuns") doLast { IdeaRunConfigurations(project).patch() }
}
