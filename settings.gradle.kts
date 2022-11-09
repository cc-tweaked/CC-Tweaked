pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://maven.minecraftforge.net")
        maven("https://maven.parchmentmc.org")
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "org.spongepowered.mixin") {
                useModule("org.spongepowered:mixingradle:${requested.version}")
            }
        }
    }
}

val mcVersion: String by settings
rootProject.name = "cc-tweaked-$mcVersion"

include(":core-api")
include(":core")

include(":mc-stubs")
include(":forge-stubs")
include(":common-api")
include(":forge-api")

include(":lints")
include(":web")

for (project in rootProject.children) {
    project.projectDir = file("projects/${project.name}")
}
