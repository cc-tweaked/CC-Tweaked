// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

import cc.tweaked.gradle.getAbsolutePath

plugins {
    `java-library`
    `java-test-fixtures`
    kotlin("jvm")

    id("cc-tweaked.java-convention")
    id("cc-tweaked.publishing")
}

dependencies {
    api(project(":core-api"))
    implementation(libs.cobalt)
    implementation(libs.fastutil)
    implementation(libs.guava)
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

    var props = mapOf("gitContributors" to cct.gitContributors.get().joinToString("\n"))
    filesMatching("data/computercraft/lua/rom/help/credits.md") { expand(props) }
}

tasks.test {
    systemProperty("cct.test-files", layout.buildDirectory.dir("tmp/testFiles").getAbsolutePath())
}

val checkChangelog = tasks.register<cc.tweaked.gradle.CheckChangelog>("checkChangelog") {
    version = project.version as String
    whatsNew = file("src/main/resources/data/computercraft/lua/rom/help/whatsnew.md")
    changelog = file("src/main/resources/data/computercraft/lua/rom/help/changelog.md")
}

tasks.check { dependsOn(checkChangelog) }

cct.linters(minecraft = false)
