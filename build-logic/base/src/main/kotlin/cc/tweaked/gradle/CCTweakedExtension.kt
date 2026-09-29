// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package cc.tweaked.gradle

import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.errorprone.errorprone
import org.gradle.api.GradleException
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.gradle.process.JavaForkOptions
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import org.gradle.testing.jacoco.tasks.JacocoReport
import java.io.IOException
import java.util.regex.Pattern

abstract class CCTweakedExtension(private val project: Project) {
    /** Get the hash of the latest git commit. */
    val gitHash: Provider<String> =
        gitProvider("<no git commit>", listOf("rev-parse", "HEAD")) { it.trim() }

    /** Get the current git branch. */
    val gitBranch: Provider<String> =
        gitProvider("<no git branch>", listOf("rev-parse", "--abbrev-ref", "HEAD")) { it.trim() }

    /** Get a list of all contributors to the project. */
    val gitContributors: Provider<List<String>> =
        gitProvider(listOf(), listOf("shortlog", "-ns", "--group=author", "--group=trailer:co-authored-by", "HEAD")) { input ->
            input.lineSequence()
                .filter { it.isNotEmpty() }
                .map {
                    val matcher = COMMIT_COUNTS.matcher(it)
                    matcher.find()
                    matcher.group(1)
                }
                .filter { !IGNORED_USERS.contains(it) }
                .toList()
                .sortedWith(String.CASE_INSENSITIVE_ORDER)
        }

    private val embeddedProjects = project.configurations.dependencyScope("embeddedProject")
    private val embeddedProjectsResolved = project.configurations.resolvable("embeddedProjectResolved") {
        extendsFrom(embeddedProjects)
        isTransitive = false
        ExternalProjectArtifacts.configure(this)
    }

    val embeddedProjectClasses: Provider<FileCollection> =
        embeddedProjectsResolved.map(ExternalProjectArtifacts::classes)
    val embeddedProjectClassesAndResources: Provider<FileCollection> =
        embeddedProjectsResolved.map(ExternalProjectArtifacts::classesAndResources)
    val embeddedProjectSources: Provider<FileCollection> =
        embeddedProjectsResolved.map(ExternalProjectArtifacts::sources)

    /**
     * Enable our custom linters on this project.
     */
    fun linters(@Suppress("UNUSED_PARAMETER") vararg unused: UseNamedArgs, minecraft: Boolean) {
        val java = project.extensions.getByType(JavaPluginExtension::class.java)

        project.dependencies.run { add("errorprone", project(":lints")) }

        project.tasks.withType(JavaCompile::class.java).configureEach {
            options.errorprone {
                // Only the main source set should run the side checker
                check("SideChecker", if (minecraft && name == "compileJava") CheckSeverity.DEFAULT else CheckSeverity.OFF)

                // If we have a custom loader, then we disable MissingOverride and enable the other two override checks.
                check("LoaderOverride", CheckSeverity.OFF)
                check("MissingLoaderOverride", CheckSeverity.OFF)
            }
        }
    }

    fun <T> jacoco(task: NamedDomainObjectProvider<T>) where T : Task, T : JavaForkOptions {
        val reportTaskName = "jacoco${task.name.capitalise()}Report"

        val jacoco = project.extensions.getByType(JacocoPluginExtension::class.java)
        task.configure {
            finalizedBy(reportTaskName)
            jacoco.applyTo(this)

            extensions.configure(JacocoTaskExtension::class.java) {
                includes = listOf("dan200.computercraft.*")
                excludes = listOf(
                    "dan200.computercraft.mixin.*", // Exclude mixins, as they're not executed at runtime.
                    "dan200.computercraft.shared.Capabilities$*", // Exclude capability tokens, as Forge rewrites them.
                )
            }
        }

        project.tasks.register(reportTaskName, JacocoReport::class.java) {
            group = LifecycleBasePlugin.VERIFICATION_GROUP
            description = "Generates code coverage report for the ${task.name} task."

            executionData(task.get())
        }
    }

    private fun <T : Any> gitProvider(default: T, command: List<String>, process: (String) -> T): Provider<T> {
        val baseResult = project.providers.exec {
            commandLine = listOf("git", "-C", project.rootDir.absolutePath) + command
        }

        return project.provider {
            val res = try {
                baseResult.standardOutput.asText.get()
            } catch (e: IOException) {
                project.logger.error("Cannot read Git repository: ${e.message}", e)
                return@provider default
            } catch (e: GradleException) {
                project.logger.error("Cannot read Git repository: ${e.message}", e)
                return@provider default
            }
            process(res)
        }
    }

    companion object {
        private val COMMIT_COUNTS = Pattern.compile("""^\s*[0-9]+\s+(.*)$""")
        private val IGNORED_USERS = setOf(
            "GitHub", "Daniel Ratcliffe", "NotSquidDev", "Weblate",
        )
    }
}
