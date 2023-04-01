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
}

tasks.javadoc {
    include("dan200/computercraft/api/**/*.java")
}

publishing {
    publications {
        named("maven", MavenPublication::class) {
            fg.component(this)
        }
    }
}
