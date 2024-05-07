// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

plugins {
    id("cc-tweaked.forge")
    id("cc-tweaked.publishing")
}

java {
    withJavadocJar()
}

cct.inlineProject(":common-api")

dependencies {
    api(project(":core-api"))

    // FIXME: This should be implementation (and in the common Forge config)
    // but NeoGradle does weird things and we end up with two Forge deps on the
    // classpath - https://github.com/neoforged/NeoGradle/issues/162.
    compileOnly("net.neoforged:neoforge:${libs.versions.neoForge.get()}")
}

tasks.javadoc {
    include("dan200/computercraft/api/**/*.java")
}
