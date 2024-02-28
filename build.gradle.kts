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
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }

    withSourcesJar()
}

val runtimeToolchain = javaToolchains.launcherFor {
    languageVersion.set(JavaLanguageVersion.of(8))
}

repositories {
    mavenCentral()

    exclusiveContent {
        forRepository { maven("https://api.modrinth.com/maven") { name = "Modrinth" } }
        filter { includeGroup("maven.modrinth") }
    }
}

volde {
    runs {
        named("client") {
            programArg("SquidDev")
            property("fml.coreMods.load", "cc.tweaked.patch.CorePlugin")
        }
    }
}

tasks.withType(net.fabricmc.loom.task.RunTask::class.java).configureEach {
    javaLauncher.set(runtimeToolchain)
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

    compileOnly("com.google.code.findbugs:jsr305:3.0.2")
    compileOnly("org.jetbrains:annotations:24.0.1")
    modImplementation("maven.modrinth:computercraft:1.50")
    "shade"("cc.tweaked:cobalt")

    "buildTools"("cc.tweaked.cobalt:build-tools")
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

    // Run under Java 8, so we can check compatibility of methods.
    javaLauncher.set(runtimeToolchain)
    mainClass.set("cc.tweaked.cobalt.build.MainKt")
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
    inputs.property("version", project.version)
    inputs.property("mcVersion", mcVersion)
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
