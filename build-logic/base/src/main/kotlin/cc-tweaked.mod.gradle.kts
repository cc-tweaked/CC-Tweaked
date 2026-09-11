// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

/**
 * Sets up the configurations for writing game tests.
 *
 * See notes in [cc.tweaked.gradle.MinecraftConfigurations] for the general design behind these cursed ideas.
 */

import cc.tweaked.gradle.MinecraftConfigurations

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

configurations.consumable("samplesElement") {
    attributes { attribute(DocsType.DOCS_TYPE_ATTRIBUTE, objects.named(DocsType.SAMPLES)) }
    outgoing.artifacts(sourceSets.named("examples").map { it.allSource.sourceDirectories.files }) {
        type = ArtifactTypeDefinition.DIRECTORY_TYPE
    }
}
