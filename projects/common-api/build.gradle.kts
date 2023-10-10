// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

plugins {
    id("cc-tweaked.java-convention")
    id("cc-tweaked.publishing")
    id("cc-tweaked.vanilla")
}

java {
    withJavadocJar()
}

dependencies {
    api(project(":core-api"))
}

tasks.javadoc {
    include("dan200/computercraft/api/**/*.java")

    // Include the core-api in our javadoc export. This is wrong, but it means we can export a single javadoc dump.
    source(project(":core-api").sourceSets.main.map { it.allJava })
}
