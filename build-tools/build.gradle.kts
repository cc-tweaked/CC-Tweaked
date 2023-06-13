// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

plugins {
	application
	alias(libs.plugins.kotlin)
}

java {
	toolchain {
		languageVersion.set(JavaLanguageVersion.of(17))
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation(libs.bundles.asm)
	implementation(libs.bundles.kotlin)
}

tasks.jar {
	manifest.attributes("Main-Class" to "cc.tweaked.build.MainKt")
}
