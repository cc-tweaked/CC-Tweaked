// SPDX-FileCopyrightText: 2026 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

import cc.tweaked.gradle.CCTweakedExtension
import cc.tweaked.gradle.JUnitExt
import com.diffplug.gradle.spotless.FormatExtension
import com.diffplug.spotless.LineEnding
import org.gradle.plugins.ide.idea.model.IdeaModel
import org.jetbrains.gradle.ext.IdeaExtPlugin
import org.jetbrains.gradle.ext.runConfigurations
import org.jetbrains.gradle.ext.settings
import java.nio.charset.StandardCharsets

plugins {
    idea
    checkstyle
    id("com.diffplug.spotless")
}

val cct = project.extensions.create("cct", CCTweakedExtension::class.java)

// Extend the [IdeaExtPlugin] plugin's `runConfiguration` container to also support [JUnitExt].
project.plugins.withType(IdeaExtPlugin::class.java) {
    val ideaModel = project.extensions.findByName("idea") as IdeaModel? ?: return@withType
    val ideaProject = ideaModel.project ?: return@withType

    ideaProject.settings.runConfigurations {
        registerFactory(JUnitExt::class.java) { name -> project.objects.newInstance(JUnitExt::class.java, name) }
    }
}

val libs = project.extensions.getByType<VersionCatalogsExtension>().named("libs")

group = "cc.tweaked"
base.archivesName.set("cc-tweaked-${libs.findVersion("minecraft").get()}-${project.name}")

repositories {
    mavenCentral()

    exclusiveContent {
        forRepositories(maven("https://maven.squiddev.cc/mirror"))
        filter {
            includeGroup("cc.tweaked")
            // Things we mirror
            includeGroup("com.simibubi.create")
            includeGroup("net.commoble.morered")
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
    checkstyle(libs.findLibrary("checkstyle").get())
}

tasks.register("checkstyle") {
    description = "Run Checkstyle on all sources"
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    dependsOn(tasks.withType(Checkstyle::class.java))
}

spotless {
    encoding = StandardCharsets.UTF_8
    lineEndings = LineEnding.UNIX

    fun FormatExtension.defaults() {
        endWithNewline()
        trimTrailingWhitespace()
        leadingTabsToSpaces(4)
    }

    kotlinGradle {
        defaults()
        ktlint()
    }

    // Only configure Java/Kotlin if we have our source sets available.
    project.plugins.withType(JvmEcosystemPlugin::class.java) {
        java {
            defaults()
            importOrder("", "javax|java", "\\#")
            removeUnusedImports()
        }
        kotlin {
            defaults()
            ktlint()
        }
    }
}

idea.module {
    excludeDirs.addAll(project.files("run", "out", "logs").files)

    // Force Gradle to write to inherit the output directory from the parent, instead of writing to out/xxx/classes.
    // This is required for Loom.
    inheritOutputDirs = true
}
