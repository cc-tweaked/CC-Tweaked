// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

import com.diffplug.gradle.spotless.FormatExtension
import com.diffplug.spotless.LineEnding
import java.nio.charset.StandardCharsets

plugins {
    alias(libs.plugins.voldeloom)
    alias(libs.plugins.spotless)
}

val modVersion: String by extra
val mcVersion: String by extra

group = "cc.tweaked"
version = modVersion

base.archivesName.convention("cc-tweaked-$mcVersion")

java {
    // Last version able to set a --release as low as 6
    toolchain.languageVersion.set(JavaLanguageVersion.of(11))

    withSourcesJar()
}

tasks.compileJava { options.release.set(8) }

repositories {
    mavenCentral()

    exclusiveContent {
        forRepository { maven("https://api.modrinth.com/maven") { name = "Modrinth" } }
        filter { includeGroup("maven.modrinth") }
    }
}

volde {
    forgeCapabilities {
        setSrgsAsFallback(true)
    }

    runs {
        named("client") {
            programArg("SquidDev")
            property("fml.coreMods.load", "cc.tweaked.patch.CorePlugin")
        }
    }
}

configurations {
    val shade by registering
    compileOnly { extendsFrom(shade.get()) }
}

val buildTools by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}

dependencies {
    minecraft("com.mojang:minecraft:$mcVersion")
    forge("net.minecraftforge:forge:${libs.versions.forge.get()}:universal@zip")

    mappings(
        volde.layered {
            importBaseZip("net.minecraftforge:forge:${libs.versions.forge.get()}:src@zip")
            removeClasses(listOf("bar", "bas"))
        },
    )

    modImplementation("maven.modrinth:computercraft:1.50")
    "shade"("org.squiddev:Cobalt")

    "buildTools"(project(":build-tools"))
}

// Point compileJava to emit to classes/uninstrumentedJava/main, and then add a task to instrument these classes,
// saving them back to the the original class directory. This is held together with so much string :(.
val mainSource = sourceSets.main.get()
val javaClassesDir = mainSource.java.classesDirectory.get()
val untransformedClasses = project.layout.buildDirectory.dir("classes/uninstrumentedJava/main")

val instrumentJava = tasks.register(mainSource.getTaskName("Instrument", "Java"), JavaExec::class) {
    dependsOn(tasks.compileJava)
    inputs.dir(untransformedClasses).withPropertyName("inputDir")
    outputs.dir(javaClassesDir).withPropertyName("outputDir")

    javaLauncher.set(
        javaToolchains.launcherFor {
            languageVersion.set(JavaLanguageVersion.of(17))
        },
    )
    mainClass.set("cc.tweaked.build.MainKt")
    classpath = buildTools

    args = listOf(
        untransformedClasses.get().asFile.absolutePath,
        javaClassesDir.asFile.absolutePath,
    )

    doFirst { project.delete(javaClassesDir) }
}

mainSource.compiledBy(instrumentJava)
tasks.compileJava {
    destinationDirectory.set(untransformedClasses)
    finalizedBy(instrumentJava)
}

tasks.withType(AbstractArchiveTask::class.java).configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
    dirMode = Integer.valueOf("755", 8)
    fileMode = Integer.valueOf("664", 8)
}

tasks.jar {
    manifest {
        attributes(
            "FMLCorePlugin" to "cc.tweaked.patch.CorePlugin",
            "FMLCorePluginContainsFMLMod" to "true",
        )
    }

    from(configurations["shade"].map { if (it.isDirectory) it else zipTree(it) })
}

tasks.processResources {
    filesMatching("mcmod.info") {
        expand("version" to project.version, "mcVersion" to mcVersion)
    }
}

spotless {
    encoding = StandardCharsets.UTF_8
    lineEndings = LineEnding.UNIX

    fun FormatExtension.defaults() {
        endWithNewline()
        trimTrailingWhitespace()
        indentWithSpaces(4)
    }

    java {
        defaults()
        removeUnusedImports()
    }

    val ktlintConfig = mapOf(
        "ktlint_standard_no-wildcard-imports" to "disabled",
        "ij_kotlin_allow_trailing_comma" to "true",
        "ij_kotlin_allow_trailing_comma_on_call_site" to "true",
    )

    kotlinGradle {
        defaults()
        ktlint().editorConfigOverride(ktlintConfig)
    }

    kotlin {
        defaults()
        ktlint().editorConfigOverride(ktlintConfig)
    }
}
