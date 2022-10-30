plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(libs.kotlin.plugin)
    implementation(libs.spotless)
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
