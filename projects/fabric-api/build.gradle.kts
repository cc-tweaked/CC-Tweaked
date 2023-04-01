// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

plugins {
    id("cc-tweaked.fabric")
    id("cc-tweaked.publishing")
}

java {
    withJavadocJar()
}

cct.inlineProject(":common-api")

dependencies {
    api(project(":core-api"))
}

tasks.jar {
    manifest {
        attributes["Fabric-Loom-Remap"] = "true"
    }
}
