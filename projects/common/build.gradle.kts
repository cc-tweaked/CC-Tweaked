// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

import cc.tweaked.gradle.annotationProcessorEverywhere
import cc.tweaked.gradle.clientClasses
import cc.tweaked.gradle.commonClasses

plugins {
    id("cc-tweaked.publishing")
    id("cc-tweaked.vanilla")
    id("cc-tweaked.gametest")
}

minecraft {
    accessWideners(
        "src/main/resources/computercraft.accesswidener",
        "src/main/resources/computercraft-common.accesswidener",
    )
}

dependencies {
    // Pull in our other projects. See comments in MinecraftConfigurations on this nastiness.
    implementation(project(":core"))
    implementation(commonClasses(project(":common-api")))
    clientImplementation(clientClasses(project(":common-api")))

    compileOnly(libs.bundles.externalMods.common)

    compileOnly(libs.mixin)
    annotationProcessorEverywhere(libs.autoService)
    testFixturesAnnotationProcessor(libs.autoService)

    testImplementation(testFixtures(project(":core")))
    testImplementation(libs.bundles.test)
    testRuntimeOnly(libs.bundles.testRuntime)

    testModImplementation(testFixtures(project(":core")))
    testModImplementation(testFixtures(project(":common")))
    testModImplementation(libs.bundles.kotlin)
}
