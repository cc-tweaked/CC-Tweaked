// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

plugins {
    id("cc-tweaked.java-convention")
    application
}

val lwjglNatives = Unit.run {
    val name = System.getProperty("os.name")!!
    val arch = System.getProperty("os.arch")
    when {
        arrayOf("Linux", "FreeBSD", "SunOS", "Unit").any { name.startsWith(it) } -> when {
            arrayOf("arm", "aarch64").any { arch.startsWith(it) } -> "natives-linux${if (arch.contains("64") || arch.startsWith("armv8")) "-arm64" else "-arm32"}"
            else -> "natives-linux"
        }

        arrayOf("Mac OS X", "Darwin").any { name.startsWith(it) } ->
            "natives-macos${if (arch.startsWith("aarch64")) "-arm64" else ""}"

        arrayOf("Windows").any { name.startsWith(it) } -> when {
            arch.contains("64") -> "natives-windows${if (arch.startsWith("aarch64")) "-arm64" else ""}"
            else -> "natives-windows-x86"
        }

        else -> throw GradleException("Unrecognized or unsupported platform.")
    }
}

dependencies {
    implementation(project(":core"))
    implementation(libs.commonsCli)
    implementation(libs.slf4j)
    runtimeOnly(libs.slf4j.simple)

    implementation(platform(libs.lwjgl.bom))
    implementation(libs.lwjgl.core)
    implementation(libs.lwjgl.glfw)
    implementation(libs.lwjgl.opengl)
    runtimeOnly(variantOf(libs.lwjgl.core) { classifier(lwjglNatives) })
    runtimeOnly(variantOf(libs.lwjgl.glfw) { classifier(lwjglNatives) })
    runtimeOnly(variantOf(libs.lwjgl.opengl) { classifier(lwjglNatives) })
}

application {
    mainClass.set("cc.tweaked.standalone.Main")
}

tasks.named("run", JavaExec::class.java) {
    workingDir = rootProject.projectDir
    args = listOf("-r", project(":core").layout.projectDirectory.dir("src/main/resources").asFile.absolutePath)
}
