/** Default configuration for non-modloader-specific Minecraft projects. */

import cc.tweaked.gradle.CCTweakedExtension
import cc.tweaked.gradle.CCTweakedPlugin
import cc.tweaked.gradle.MinecraftConfigurations

plugins {
    id("cc-tweaked.java-convention")
    id("org.spongepowered.gradle.vanilla")
}

plugins.apply(CCTweakedPlugin::class.java)

val mcVersion: String by extra

minecraft {
    version(mcVersion)
}

dependencies {
    val libs = project.extensions.getByType<VersionCatalogsExtension>().named("libs")

    // Depend on error prone annotations to silence a lot of compile warnings.
    compileOnlyApi(libs.findLibrary("errorProne.annotations").get())
}

MinecraftConfigurations.setup(project)
