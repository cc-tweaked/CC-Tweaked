// SPDX-FileCopyrightText: 2026 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

plugins {
    `java-gradle-plugin`
}

dependencies {
    compileOnly(libs.teavm.tooling)
}

gradlePlugin {
    plugins {
        register("cc-tweaked.teavm") {
            id = "cc-tweaked.teavm"
            implementationClass = "cc.tweaked.gradle.TeaVMPlugin"
        }
    }
}
