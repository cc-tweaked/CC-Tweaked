// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

import cc.tweaked.gradle.getAbsolutePath

plugins {
    `java-library`
    `java-test-fixtures`

    id("cc-tweaked.kotlin-convention")
    id("cc-tweaked.java-convention")
    id("cc-tweaked.publishing")
    id("cc-tweaked")
}

val modVersion: String by extra

dependencies {
    api(project(":core-api"))
    implementation(libs.cobalt)
    implementation(libs.fastutil)
    implementation(libs.guava)
    implementation(libs.jzlib)
    implementation(libs.netty.http)
    implementation(libs.netty.socks)
    implementation(libs.netty.proxy)
    implementation(libs.slf4j)

    testFixturesImplementation(libs.slf4j)
    testFixturesApi(platform(libs.kotlin.platform))
    testFixturesApi(libs.bundles.test)
    testFixturesApi(libs.bundles.kotlin)

    testImplementation(libs.asm)
    testImplementation(libs.bundles.test)
    testRuntimeOnly(libs.bundles.testRuntime)
    testRuntimeOnly(libs.slf4j.simple)
}

tasks.processResources {
    inputs.property("gitHash", cct.gitHash)

    filesMatching("data/computercraft/lua/rom/help/credits.md") {
        expand(mapOf("gitContributors" to cct.gitContributors.map { it.joinToString("\n") }.get()))
    }
}

tasks.test {
    systemProperty("cct.test-files", layout.buildDirectory.dir("tmp/testFiles").getAbsolutePath())
}

val checkChangelog by tasks.registering(cc.tweaked.gradle.CheckChangelog::class) {
    version.set(modVersion)
    whatsNew.set(file("src/main/resources/data/computercraft/lua/rom/help/whatsnew.md"))
    changelog.set(file("src/main/resources/data/computercraft/lua/rom/help/changelog.md"))
}

tasks.check { dependsOn(checkChangelog) }
