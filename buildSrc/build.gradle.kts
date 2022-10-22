plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(libs.spotless)
}

gradlePlugin {
    plugins {
        register("cc-tweaked.illuaminate") {
            id = "cc-tweaked.illuaminate"
            implementationClass = "cc.tweaked.gradle.IlluaminatePlugin"
        }
    }
}
