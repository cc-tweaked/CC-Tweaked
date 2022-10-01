plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

gradlePlugin {
    plugins {
        register("cc-tweaked.illuaminate") {
            id = "cc-tweaked.illuaminate"
            implementationClass = "cc.tweaked.gradle.IlluaminatePlugin"
        }
    }
}
