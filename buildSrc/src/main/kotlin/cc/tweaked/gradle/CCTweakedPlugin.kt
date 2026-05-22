// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package cc.tweaked.gradle

import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.plugins.ide.idea.model.IdeaModel
import org.jetbrains.gradle.ext.IdeaExtPlugin
import org.jetbrains.gradle.ext.runConfigurations
import org.jetbrains.gradle.ext.settings
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

/**
 * Configures projects to match a shared configuration.
 */
class CCTweakedPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val cct = project.extensions.create("cct", CCTweakedExtension::class.java)

        project.plugins.withType(JavaPlugin::class.java) {
            val sourceSets = project.extensions.getByType(JavaPluginExtension::class.java).sourceSets
            cct.sourceDirectories.add(SourceSetReference.internal(sourceSets.getByName("main")))
        }

        project.plugins.withType(IdeaExtPlugin::class.java) { extendIdea(project) }
    }

    /**
     * Extend the [IdeaExtPlugin] plugin's `runConfiguration` container to also support [JUnitExt].
     */
    private fun extendIdea(project: Project) {
        val ideaModel = project.extensions.findByName("idea") as IdeaModel? ?: return
        val ideaProject = ideaModel.project ?: return

        ideaProject.settings.runConfigurations {
            registerFactory(JUnitExt::class.java) { name -> project.objects.newInstance(JUnitExt::class.java, name) }
        }
    }

    companion object {
        /**
         * The version we run with. We use Java 25 here, as our Gradle build requires that.
         */
        val JDK_VERSION = JavaLanguageVersion.of(25)

        /**
         * The Java version we target. Should be the same as what Minecraft uses.
         */
        val JAVA_TARGET = JavaLanguageVersion.of(17)

        val JAVA_VERSION = JavaVersion.toVersion(JAVA_TARGET.asInt())
        val KOTLIN_TARGET = JvmTarget.fromTarget(JAVA_TARGET.toString())
    }
}
