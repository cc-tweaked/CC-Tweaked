import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

/**
 * Sets up the configurations for writing game tests.
 *
 * See notes in [cc.tweaked.gradle.MinecraftConfigurations] for the general design behind these cursed ideas.
 */

plugins {
    id("cc-tweaked.kotlin-convention")
    id("cc-tweaked.java-convention")
}

val main = sourceSets.main.get()

// Both testMod and testFixtures inherit from the main classpath, just so we have access to Minecraft classes.
val testMod by sourceSets.creating {
    compileClasspath += main.compileClasspath
    runtimeClasspath += main.runtimeClasspath
}

configurations {
    named(testMod.compileClasspathConfigurationName) {
        shouldResolveConsistentlyWith(compileClasspath.get())
    }

    named(testMod.runtimeClasspathConfigurationName) {
        shouldResolveConsistentlyWith(runtimeClasspath.get())
    }
}

// Like the main test configurations, we're safe to depend on source set outputs.
dependencies {
    add(testMod.implementationConfigurationName, main.output)
}

// Similar to java-test-fixtures, but tries to avoid putting the obfuscated jar on the classpath.

val testFixtures by sourceSets.creating {
    compileClasspath += main.compileClasspath
}

java.registerFeature("testFixtures") {
    usingSourceSet(testFixtures)
    disablePublication()
}

dependencies {
    add(testFixtures.implementationConfigurationName, main.output)

    testImplementation(testFixtures(project))
    add(testMod.implementationConfigurationName, testFixtures(project))
}
