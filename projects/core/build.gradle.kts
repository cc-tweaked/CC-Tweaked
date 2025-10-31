// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

import cc.tweaked.gradle.CCTweakedPlugin
import cc.tweaked.gradle.getAbsolutePath

plugins {
    `java-library`
    `java-test-fixtures`
    kotlin("jvm")
    alias(libs.plugins.shadow)

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

kotlin.compilerOptions.jvmTarget = CCTweakedPlugin.KOTLIN_TARGET

tasks.processResources {
    inputs.property("gitHash", cct.gitHash)

    var props = mapOf("gitContributors" to cct.gitContributors.get().joinToString("\n"))
    filesMatching("data/computercraft/lua/rom/help/credits.md") { expand(props) }
}

tasks.test {
    systemProperty("cct.test-files", layout.buildDirectory.dir("tmp/testFiles").getAbsolutePath())
}

val checkChangelog by tasks.registering(cc.tweaked.gradle.CheckChangelog::class) {
    version = modVersion
    whatsNew = file("src/main/resources/data/computercraft/lua/rom/help/whatsnew.md")
    changelog = file("src/main/resources/data/computercraft/lua/rom/help/changelog.md")
}

tasks.check { dependsOn(checkChangelog) }

cct.linters(minecraft = false, loader = null)

// We configure the shadow jar to ship netty-codec and all its dependencies, relocating them under the
// dan200.computercraft.core package.
// This is used as part of the Forge build, so that our version of netty-codec is loaded under the GAME layer, and so
// has access to our jar-in-jar'ed jzlib.
tasks.shadowJar {
    minimize()

    dependencies {
        include(dependency(libs.netty.codec.get()))
        include(dependency(libs.netty.http.get()))
        include(dependency(libs.netty.socks.get()))
        include(dependency(libs.netty.proxy.get()))
    }

    for (pkg in listOf("io.netty.handler.codec", "io.netty.handler.proxy")) {
        relocate(pkg, "dan200.computercraft.core.vendor.$pkg")
    }
}
