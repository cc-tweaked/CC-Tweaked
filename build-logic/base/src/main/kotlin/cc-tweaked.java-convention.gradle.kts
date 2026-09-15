// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

import cc.tweaked.gradle.CCTweakedExtension
import cc.tweaked.gradle.CCTweakedJavaVersions
import cc.tweaked.gradle.JUnitExt
import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.errorprone.errorprone
import org.gradle.plugins.ide.idea.model.IdeaModel
import org.jetbrains.gradle.ext.IdeaExtPlugin
import org.jetbrains.gradle.ext.runConfigurations
import org.jetbrains.gradle.ext.settings
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinBasePlugin

plugins {
    `java-library`
    jacoco
    id("cc-tweaked.base-convention")
    id("net.ltgt.errorprone")
    // Required for cross-project dependencies in Fabric
    id("net.fabricmc.fabric-loom-companion")
}

val cct = project.extensions.getByType<CCTweakedExtension>()

// Extend the [IdeaExtPlugin] plugin's `runConfiguration` container to also support [JUnitExt].
project.plugins.withType(IdeaExtPlugin::class.java) {
    val ideaModel = project.extensions.findByName("idea") as IdeaModel? ?: return@withType
    val ideaProject = ideaModel.project ?: return@withType

    ideaProject.settings.runConfigurations {
        registerFactory(JUnitExt::class.java) { name -> project.objects.newInstance(JUnitExt::class.java, name) }
    }
}

val libs = project.extensions.getByType<VersionCatalogsExtension>().named("libs")

java {
    toolchain { languageVersion = CCTweakedJavaVersions.JDK_VERSION }
    sourceCompatibility = CCTweakedJavaVersions.JAVA_VERSION
    targetCompatibility = CCTweakedJavaVersions.JAVA_VERSION

    withSourcesJar()
}

dependencies {
    errorprone(libs.findLibrary("errorProne-core").get())
    errorprone(libs.findLibrary("nullAway").get())
}

// Explicitly set the Kotlin target version to match our release/target version, not the JVM toolchain version.
project.plugins.withType(KotlinBasePlugin::class.java) {
    val kotlin = project.extensions.getByType<KotlinJvmProjectExtension>()
    kotlin.compilerOptions.jvmTarget = CCTweakedJavaVersions.KOTLIN_TARGET
}

// Configure JavaCompile tasks with our default settings.
tasks.withType(JavaCompile::class.java).configureEach {
    options.encoding = "UTF-8"

    // Explicitly set release, as that limits the APIs we can use to the right version of Java.
    options.release.set(CCTweakedJavaVersions.JAVA_TARGET.asInt())

    options.compilerArgs.addAll(
        listOf(
            "-Xlint",
            // Processing just gives us "No processor claimed any of these annotations", so skip that!
            "-Xlint:-processing",
            // We violate this pattern too often for it to be a helpful warning. Something to improve one day!
            "-Xlint:-this-escape",
        ),
    )

    options.errorprone {
        check("MissingOverride", CheckSeverity.ERROR)
        check("InvalidBlockTag", CheckSeverity.OFF) // Broken by @cc.xyz
        check("InlineMeSuggester", CheckSeverity.OFF) // Minecraft uses @Deprecated liberally
        // Too many false positives right now. Maybe we need an indirection for it later on.
        check("AssignmentExpression", CheckSeverity.OFF) // I'm a bad person.
        check("ReferenceEquality", CheckSeverity.OFF)
        check("EnumOrdinal", CheckSeverity.OFF) // For now. We could replace most of these with EnumMap.
        check("OperatorPrecedence", CheckSeverity.OFF) // For now.
        check("NonOverridingEquals", CheckSeverity.OFF) // Peripheral.equals makes this hard to avoid
        // The lint isn't wrong, but we don't control the number of threads used by MC, so thread priority is a reasonable
        // option.
        check("ThreadPriorityCheck", CheckSeverity.OFF)
        check("FutureReturnValueIgnored", CheckSeverity.OFF) // Too many false positives with Netty
        option("UnusedMethod:ExemptingMethodAnnotations", "dan200.computercraft.api.lua.LuaFunction")

        check("NullAway", CheckSeverity.ERROR)
        option(
            "NullAway:AnnotatedPackages",
            listOf("dan200.computercraft", "cc.tweaked", "net.fabricmc.fabric.api").joinToString(","),
        )
        option("NullAway:ExcludedFieldAnnotations", listOf("org.spongepowered.asm.mixin.Shadow").joinToString(","))
        option("NullAway:CastToNonNullMethod", "dan200.computercraft.core.util.Nullability.assertNonNull")
        option("NullAway:CheckOptionalEmptiness")
        option("NullAway:AcknowledgeRestrictiveAnnotations")

        excludedPaths.set(".*/jmh_generated/.*")
    }
}

tasks.compileTestJava {
    options.errorprone {
        check("NullAway", CheckSeverity.OFF)
    }
}

tasks.processResources {
    exclude("**/*.license")
    exclude(".cache")
}

tasks.jar {
    manifest {
        attributes(
            "Specification-Title" to "computercraft",
            "Specification-Vendor" to "SquidDev",
            "Specification-Version" to "1",
            "Implementation-Title" to "cctweaked-${project.name}",
            "Implementation-Version" to project.version as String,
            "Implementation-Vendor" to "SquidDev",
        )
    }

    from(cct.embeddedProjectClassesAndResources)
}

tasks.named<Jar>("sourcesJar") {
    from(cct.embeddedProjectSources)
}

tasks.javadoc {
    options {
        val stdOptions = this as StandardJavadocDocletOptions
        stdOptions.addBooleanOption("Xdoclint:all,-missing", true)
        stdOptions.links("https://docs.oracle.com/en/java/javase/${CCTweakedJavaVersions.JAVA_TARGET.asInt()}/docs/api/")
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
    reports.xml.required = true
    reports.html.required = true

    sourceDirectories.from(cct.embeddedProjectSources)
    classDirectories.from(cct.embeddedProjectClasses)
}
