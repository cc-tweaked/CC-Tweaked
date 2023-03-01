// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

import cc.tweaked.gradle.clientClasses
import cc.tweaked.gradle.commonClasses

/**
 * Sets up the configurations for writing game tests.
 *
 * See notes in [cc.tweaked.gradle.MinecraftConfigurations] for the general design behind these cursed ideas.
 */

plugins {
    id("cc-tweaked.kotlin-convention")
    id("cc-tweaked.java-convention")
}

val main = sourceSets["main"]
val client = sourceSets["client"]

// Both testMod and testFixtures inherit from the main and client classpath, just so we have access to Minecraft classes.
val testMod by sourceSets.creating {
    compileClasspath += main.compileClasspath + client.compileClasspath
    runtimeClasspath += main.runtimeClasspath + client.runtimeClasspath
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
    add(testMod.implementationConfigurationName, client.output)
}

// Similar to java-test-fixtures, but tries to avoid putting the obfuscated jar on the classpath.

val testFixtures by sourceSets.creating {
    compileClasspath += main.compileClasspath + client.compileClasspath
}

java.registerFeature("testFixtures") {
    usingSourceSet(testFixtures)
    disablePublication()
}

dependencies {
    val libs = project.extensions.getByType<VersionCatalogsExtension>().named("libs")
    add(testFixtures.apiConfigurationName, libs.findBundle("test").get())
    // Consumers of this project already have the common and client classes on the classpath, so it's fine for these
    // to be compile-only.
    add(testFixtures.compileOnlyApiConfigurationName, commonClasses(project))
    add(testFixtures.compileOnlyApiConfigurationName, clientClasses(project))

    testImplementation(testFixtures(project))
}
