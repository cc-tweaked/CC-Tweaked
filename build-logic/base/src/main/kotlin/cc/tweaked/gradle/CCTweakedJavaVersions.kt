// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package cc.tweaked.gradle

import org.gradle.api.JavaVersion
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

object CCTweakedJavaVersions {
    /**
     * The version we run with. We use Java 25 here, as our Gradle build requires that.
     */
    val JDK_VERSION = JavaLanguageVersion.of(25)

    /**
     * The Java version we target. Should be the same as what Minecraft uses.
     */
    val JAVA_TARGET = JavaLanguageVersion.of(25)

    val JAVA_VERSION = JavaVersion.toVersion(JAVA_TARGET.asInt())
    val KOTLIN_TARGET = JvmTarget.fromTarget(JAVA_TARGET.toString())
}
