// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package cc.tweaked.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.plugins.ide.idea.model.IdeaModel
import org.jetbrains.gradle.ext.IdeaExtPlugin
import org.jetbrains.gradle.ext.runConfigurations
import org.jetbrains.gradle.ext.settings

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
        val JAVA_VERSION = JavaLanguageVersion.of(17)
    }
}
