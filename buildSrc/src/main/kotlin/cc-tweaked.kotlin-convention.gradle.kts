// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

import cc.tweaked.gradle.CCTweakedPlugin
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
}

kotlin {
    jvmToolchain {
        languageVersion.set(CCTweakedPlugin.JAVA_VERSION)
    }
}

tasks.withType(KotlinCompile::class.java).configureEach {
    // So technically we shouldn't need to do this as the toolchain sets it above. However, the option only appears
    // to be set when the task executes, so doesn't get picked up by IDEs.
    kotlinOptions.jvmTarget = when {
        CCTweakedPlugin.JAVA_VERSION.asInt() > 8 -> CCTweakedPlugin.JAVA_VERSION.toString()
        else -> "1.${CCTweakedPlugin.JAVA_VERSION.asInt()}"
    }
}
