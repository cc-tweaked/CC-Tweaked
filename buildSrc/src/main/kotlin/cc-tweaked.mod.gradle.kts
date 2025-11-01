// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

/**
 * Sets up the configurations for writing game tests.
 *
 * See notes in [cc.tweaked.gradle.MinecraftConfigurations] for the general design behind these cursed ideas.
 */

import cc.tweaked.gradle.CCTweakedPlugin
import cc.tweaked.gradle.MinecraftConfigurations
import cc.tweaked.gradle.clientClasses
import cc.tweaked.gradle.commonClasses

plugins {
    kotlin("jvm")
    id("cc-tweaked.java-convention")
}

val main = sourceSets["main"]
val client = sourceSets["client"]

MinecraftConfigurations.createDerivedConfiguration(project, MinecraftConfigurations.DATAGEN)
MinecraftConfigurations.createDerivedConfiguration(project, MinecraftConfigurations.EXAMPLES)
MinecraftConfigurations.createDerivedConfiguration(project, MinecraftConfigurations.TEST_MOD)

// Set up generated resources
sourceSets.main { resources.srcDir("src/generated/resources") }
sourceSets.named("examples") { resources.srcDir("src/examples/generatedResources") }

// Make sure our examples compile.
tasks.check { dependsOn(tasks.named("compileExamplesJava")) }

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

kotlin.compilerOptions.jvmTarget = CCTweakedPlugin.KOTLIN_TARGET
