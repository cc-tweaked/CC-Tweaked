plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

// Duplicated in settings.gradle.kts
repositories {
    mavenCentral()
    gradlePluginPortal()

    maven("https://maven.minecraftforge.net") {
        name = "Forge"
        content {
            includeGroup("net.minecraftforge")
            includeGroup("net.minecraftforge.gradle")
        }
    }

    maven("https://maven.parchmentmc.org") {
        name = "Librarian"
        content {
            includeGroupByRegex("^org\\.parchmentmc.*")
        }
    }

    maven("https://repo.spongepowered.org/repository/maven-public/") {
        name = "Sponge"
        content {
            includeGroup("org.spongepowered")
        }
    }
}

dependencies {
    implementation(libs.errorProne.plugin)
    implementation(libs.kotlin.plugin)
    implementation(libs.spotless)

    implementation(libs.vanillaGradle)
    implementation(libs.forgeGradle)
    implementation(libs.librarian)
}

gradlePlugin {
    plugins {
        register("cc-tweaked") {
            id = "cc-tweaked"
            implementationClass = "cc.tweaked.gradle.CCTweakedPlugin"
        }

        register("cc-tweaked.illuaminate") {
            id = "cc-tweaked.illuaminate"
            implementationClass = "cc.tweaked.gradle.IlluaminatePlugin"
        }

        register("cc-tweaked.node") {
            id = "cc-tweaked.node"
            implementationClass = "cc.tweaked.gradle.NodePlugin"
        }
    }
}
