// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

import com.diffplug.gradle.spotless.FormatExtension
import com.diffplug.spotless.LineEnding
import net.fabricmc.loom.LoomGradleExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import java.nio.charset.StandardCharsets

plugins {
    alias(libs.plugins.voldeloom)
    alias(libs.plugins.spotless)
    alias(libs.plugins.kotlin)
    id("com.gradleup.shadow") version "8.3.5"
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

    registerFeature("build") {
        usingSourceSet(sourceSets.create("build"))
        disablePublication()
    }
}

val runtimeToolchain = javaToolchains.launcherFor {
    languageVersion.set(JavaLanguageVersion.of(8))
}

repositories {
    mavenCentral()

    exclusiveContent {
        forRepository { maven("https://maven.squiddev.cc") }
        filter { includeGroup("cc.tweaked") }
    }

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

dependencies {
    minecraft("com.mojang:minecraft:$mcVersion")
    forge("net.minecraftforge:forge:${libs.versions.forge.get()}:universal@zip")

    mappings(
        volde.layered {
            importBaseZip("net.minecraftforge:forge:${libs.versions.forge.get()}:src@zip")
            removeClasses(listOf("bar", "bas"))
        },
    )

    compileOnly(libs.errorProne.annotations)
    compileOnly(libs.jetbrainsAnnotations)
    compileOnly(libs.jspecify)

    modImplementation("maven.modrinth:computercraft:1.50@zip")
    "shade"("cc.tweaked:cobalt:0.9.9")
    "shade"(libs.bundles.shadow)

    "buildImplementation"(libs.slf4j.api)
    "buildImplementation"(libs.bundles.asm)
    "buildImplementation"(libs.kotlin)
    "buildRuntimeOnly"(libs.slf4j.simple)
}

tasks.named("compileBuildJava", JavaCompile::class.java) { options.release = 8 }
tasks.named("compileBuildKotlin", KotlinJvmCompile::class.java).configure {
    compilerOptions.jvmTarget = JvmTarget.JVM_1_8
}

val instrumentJarPath = layout.buildDirectory.file("$name.jar")
val instrumentJar = tasks.register("instrumentJar", JavaExec::class) {
    inputs.file(tasks.shadowJar.flatMap { it.archiveFile }).withPropertyName("inputDir")
    outputs.file(instrumentJarPath).withPropertyName("outputDir")

    // Run under Java 8, so we can check compatibility of methods.
    javaLauncher.set(runtimeToolchain)
    mainClass.set("cc.tweaked.build.MainKt")
    classpath = sourceSets["build"].runtimeClasspath

    argumentProviders.add {
        listOf(
            tasks.shadowJar.get().archiveFile.get().asFile.absolutePath,
            instrumentJarPath.get().asFile.absolutePath,
        )
    }
}

tasks.withType(AbstractArchiveTask::class.java).configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
    dirPermissions { }
    filePermissions {}
}

// Override remapJarForRelease, and then manually configure all tasks to have the right classifiers.
tasks.remapJarForRelease {
    dependsOn(instrumentJar)
    archiveClassifier = ""
    input = instrumentJarPath
}

tasks.jar { archiveClassifier = "dev-slim" }

tasks.shadowJar {
    archiveClassifier = "dev"
    manifest {
        attributes(
            "FMLCorePlugin" to "cc.tweaked.patch.CorePlugin",
            "FMLCorePluginContainsFMLMod" to "true",
        )
    }

    configurations = listOf(project.configurations["shade"])
    relocate("it.unimi.dsi.fastutil", "cc.tweaked.vendor.fastutil")
    relocate("io.netty", "cc.tweaked.vendor.netty")
    relocate("org.slf4j", "cc.tweaked.vendor.slf4j")
    minimize()

    exclude("META-INF/maven/", "META-INF/native-image/", "META-INF/versions/")
}

project.afterEvaluate {
    // Remove tasks.jar from the runtime classpath and add shadowJar instead.
    val field = LoomGradleExtension::class.java.getDeclaredField("unmappedModsBuilt")
    field.isAccessible = true
    (field.get(volde) as MutableList<*>).clear()
    volde.addUnmappedMod(instrumentJarPath.get().asFile.toPath())
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
