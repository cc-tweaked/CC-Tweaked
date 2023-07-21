// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

import cc.tweaked.gradle.CCTweakedExtension
import cc.tweaked.gradle.CCTweakedPlugin
import com.diffplug.gradle.spotless.FormatExtension
import com.diffplug.spotless.LineEnding
import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.errorprone.errorprone
import java.nio.charset.StandardCharsets

plugins {
    `java-library`
    idea
    jacoco
    checkstyle
    id("com.diffplug.spotless")
    id("net.ltgt.errorprone")
}

val modVersion: String by extra
val mcVersion: String by extra

group = "cc.tweaked"
version = modVersion

base.archivesName.convention("cc-tweaked-$mcVersion-${project.name}")

java {
    toolchain {
        languageVersion.set(CCTweakedPlugin.JAVA_VERSION)
    }

    withSourcesJar()
}

repositories {
    mavenCentral()

    val mainMaven = maven("https://squiddev.cc/maven") {
        name = "SquidDev"
        content {
            // Until https://github.com/SpongePowered/Mixin/pull/593 is merged
            includeModule("org.spongepowered", "mixin")
        }
    }

    exclusiveContent {
        forRepositories(mainMaven)

        // Include the ForgeGradle repository if present. This requires that ForgeGradle is already present, which we
        // enforce in our Forge overlay.
        val fg =
            project.extensions.findByType(net.minecraftforge.gradle.userdev.DependencyManagementExtension::class.java)
        if (fg != null) forRepositories(fg.repository)

        filter {
            includeGroup("cc.tweaked")
            includeModule("org.squiddev", "Cobalt")
            // Things we mirror
            includeGroup("dev.architectury")
            includeGroup("dev.emi")
            includeGroup("maven.modrinth")
            includeGroup("me.shedaniel.cloth")
            includeGroup("me.shedaniel")
            includeGroup("mezz.jei")
            includeModule("com.terraformersmc", "modmenu")
        }
    }
}

dependencies {
    val libs = project.extensions.getByType<VersionCatalogsExtension>().named("libs")
    checkstyle(libs.findLibrary("checkstyle").get())

    errorprone(libs.findLibrary("errorProne-core").get())
    errorprone(libs.findLibrary("nullAway").get())
}

// Configure default JavaCompile tasks with our arguments.
sourceSets.all {
    tasks.named(compileJavaTaskName, JavaCompile::class.java) {
        // Processing just gives us "No processor claimed any of these annotations", so skip that!
        options.compilerArgs.addAll(listOf("-Xlint", "-Xlint:-processing"))

        options.errorprone {
            check("InvalidBlockTag", CheckSeverity.OFF) // Broken by @cc.xyz
            check("InvalidParam", CheckSeverity.OFF) // Broken by records.
            check("InlineMeSuggester", CheckSeverity.OFF) // Minecraft uses @Deprecated liberally
            // Too many false positives right now. Maybe we need an indirection for it later on.
            check("ReferenceEquality", CheckSeverity.OFF)
            check("UnusedVariable", CheckSeverity.OFF) // Too many false positives with records.
            check("OperatorPrecedence", CheckSeverity.OFF) // For now.
            check("AlreadyChecked", CheckSeverity.OFF) // Seems to be broken?
            check("NonOverridingEquals", CheckSeverity.OFF) // Peripheral.equals makes this hard to avoid
            check("FutureReturnValueIgnored", CheckSeverity.OFF) // Too many false positives with Netty

            check("NullAway", CheckSeverity.ERROR)
            option("NullAway:AnnotatedPackages", listOf("dan200.computercraft", "net.fabricmc.fabric.api").joinToString(","))
            option("NullAway:ExcludedFieldAnnotations", listOf("org.spongepowered.asm.mixin.Shadow").joinToString(","))
            option("NullAway:CastToNonNullMethod", "dan200.computercraft.core.util.Nullability.assertNonNull")
            option("NullAway:CheckOptionalEmptiness")
            option("NullAway:AcknowledgeRestrictiveAnnotations")
        }
    }
}

tasks.compileTestJava {
    options.errorprone {
        check("NullAway", CheckSeverity.OFF)
    }
}


tasks.withType(JavaCompile::class.java).configureEach {
    options.encoding = "UTF-8"
}

tasks.processResources {
    exclude("**/*.license")
    exclude(".cache")
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
            "Specification-Title" to "computercraft",
            "Specification-Vendor" to "SquidDev",
            "Specification-Version" to "1",
            "Implementation-Title" to "cctweaked-${project.name}",
            "Implementation-Version" to modVersion,
            "Implementation-Vendor" to "SquidDev",
        )
    }
}

tasks.javadoc {
    options {
        val stdOptions = this as StandardJavadocDocletOptions
        stdOptions.addBooleanOption("Xdoclint:all,-missing", true)
        stdOptions.links("https://docs.oracle.com/en/java/javase/17/docs/api/")
    }
}

tasks.test {
    finalizedBy("jacocoTestReport")

    useJUnitPlatform()
    testLogging {
        events("skipped", "failed")
    }
}

tasks.withType(JacocoReport::class.java).configureEach {
    reports.xml.required.set(true)
    reports.html.required.set(true)
}

project.plugins.withType(CCTweakedPlugin::class.java) {
    // Set up jacoco to read from /all/ our source directories.
    val cct = project.extensions.getByType<CCTweakedExtension>()
    project.tasks.named("jacocoTestReport", JacocoReport::class.java) {
        for (ref in cct.sourceSets.get()) sourceDirectories.from(ref.allSource.sourceDirectories)
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

idea.module {
    excludeDirs.addAll(project.files("run", "out", "logs").files)

    // Force Gradle to write to inherit the output directory from the parent, instead of writing to out/xxx/classes.
    // This is required for Loom, and we patch Forge's run configurations to work there.
    // TODO: Submit a patch to Forge to support ProjectRootManager.
    inheritOutputDirs = true
}
