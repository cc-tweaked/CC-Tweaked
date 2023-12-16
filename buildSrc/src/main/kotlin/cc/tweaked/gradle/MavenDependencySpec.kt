// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package cc.tweaked.gradle

import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.plugins.BasePluginExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.specs.Spec

/**
 * A dependency in a POM file.
 */
data class MavenDependency(val groupId: String?, val artifactId: String?, val version: String?, val scope: String?)

/**
 * A spec specifying which dependencies to include/exclude.
 */
class MavenDependencySpec {
    private val excludeSpecs = mutableListOf<Spec<MavenDependency>>()

    fun exclude(spec: Spec<MavenDependency>) {
        excludeSpecs.add(spec)
    }

    fun exclude(dep: Dependency) {
        exclude {
            // We have to cheat a little for project dependencies, as the project name doesn't match the artifact group.
            val name = when (dep) {
                is ProjectDependency -> dep.dependencyProject.extensions.getByType(BasePluginExtension::class.java).archivesName.get()
                else -> dep.name
            }
            (dep.group.isNullOrEmpty() || dep.group == it.groupId) &&
                (name.isNullOrEmpty() || name == it.artifactId) &&
                (dep.version.isNullOrEmpty() || dep.version == it.version)
        }
    }

    fun exclude(dep: MinimalExternalModuleDependency) {
        exclude {
            dep.module.group == it.groupId && dep.module.name == it.artifactId
        }
    }

    fun isIncluded(dep: MavenDependency) = !excludeSpecs.any { it.isSatisfiedBy(dep) }
}

/**
 * Configure dependencies present in this publication's POM file.
 *
 * While this approach is very ugly, it's the easiest way to handle it!
 */
fun MavenPublication.mavenDependencies(action: MavenDependencySpec.() -> Unit) {
    val spec = MavenDependencySpec()
    action(spec)

    pom.withXml {
        val dependencies = XmlUtil.findChild(asNode(), "dependencies") ?: return@withXml
        dependencies.children().map { it as groovy.util.Node }.forEach {
            val dep = MavenDependency(
                groupId = XmlUtil.findChild(it, "groupId")?.text(),
                artifactId = XmlUtil.findChild(it, "artifactId")?.text(),
                version = XmlUtil.findChild(it, "version")?.text(),
                scope = XmlUtil.findChild(it, "scope")?.text(),
            )

            if (!spec.isIncluded(dep)) it.parent().remove(it)
        }
    }
}
