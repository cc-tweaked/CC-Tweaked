// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package cc.tweaked.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ModuleComponentSelector
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.result.DependencyResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.language.base.plugins.LifecycleBasePlugin

abstract class DependencyCheck : DefaultTask() {
    @get:Input
    protected abstract val dependencies: ListProperty<DependencyResult>

    /**
     * A mapping of module coordinates (`group:module`) to versions, overriding the requested version.
     */
    @get:Input
    protected abstract val overrides: MapProperty<String, String>

    init {
        description = "Check :core's dependencies are consistent with Minecraft's."
        group = LifecycleBasePlugin.VERIFICATION_GROUP

        dependencies.finalizeValueOnRead()
        overrides.finalizeValueOnRead()
    }

    /**
     * Override a module with a different version.
     */
    fun override(module: Provider<MinimalExternalModuleDependency>, version: String) {
        overrides.putAll(project.provider { mutableMapOf(module.get().module.toString() to version) })
    }

    fun override(group: String, module: String, version: String) {
        overrides.put("$group:$module", version)
    }

    /**
     * Add a configuration to check.
     */
    fun configuration(configuration: Provider<Configuration>) {
        // We can't store the Configuration in the cache, so store the resolved dependencies instead.
        dependencies.addAll(configuration.map { it.incoming.resolutionResult.allDependencies })
    }

    @TaskAction
    fun run() {
        var ok = true
        for (configuration in dependencies.get()) {
            if (!check(configuration)) ok = false
        }

        if (!ok) {
            throw GradleException("Mismatched versions in Minecraft dependencies. gradle/libs.versions.toml may need updating.")
        }
    }

    private fun check(dependency: DependencyResult): Boolean {
        if (dependency !is ResolvedDependencyResult) {
            logger.warn("Found unexpected dependency result {}", dependency)
            return false
        }

        // Skip dependencies on non-modules.
        val requested = dependency.requested
        if (requested !is ModuleComponentSelector) return true

        // If this dependency is specified within some project (so is non-transitive), or is pulled in via Minecraft,
        // then check for consistency.
        // It would be nice to be smarter about transitive dependencies, but avoiding false positives is hard.
        val from = dependency.from.id
        if (
            from is ProjectComponentIdentifier ||
            from is ModuleComponentIdentifier && (from.group == "net.minecraft" || from.group == "io.netty")
        ) {
            // If the version is different between the requested and selected version, report an error.
            val selected = dependency.selected.moduleVersion!!.version
            val requestedVersion = overrides.get()["${requested.group}:${requested.module}"] ?: requested.version
            if (requestedVersion != selected) {
                logger.error("Requested dependency {} (via {}) but got version {}", requested, from, selected)
                return false
            }

            return true
        }
        return true
    }
}
