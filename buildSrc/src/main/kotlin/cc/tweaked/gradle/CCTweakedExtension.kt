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
import org.gradle.api.artifacts.Dependency
import org.gradle.api.attributes.TestSuiteType
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.api.reporting.ReportingExtension
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.gradle.language.jvm.tasks.ProcessResources
import org.gradle.process.JavaForkOptions
import org.gradle.testing.jacoco.plugins.JacocoCoverageReport
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import org.gradle.testing.jacoco.tasks.JacocoReport
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.File
import java.io.IOException
import java.net.URI
import java.util.regex.Pattern

abstract class CCTweakedExtension(
    private val project: Project,
    private val fs: FileSystemOperations,
) {
    /** Get the hash of the latest git commit. */
    val gitHash: Provider<String> = gitProvider(project, "<no git hash>") {
        ProcessHelpers.captureOut("git", "-C", project.rootProject.projectDir.absolutePath, "rev-parse", "HEAD").trim()
    }

    /** Get the current git branch. */
    val gitBranch: Provider<String> = gitProvider(project, "<no git branch>") {
        ProcessHelpers.captureOut("git", "-C", project.rootProject.projectDir.absolutePath, "rev-parse", "--abbrev-ref", "HEAD")
            .trim()
    }

    /** Get a list of all contributors to the project. */
    val gitContributors: Provider<List<String>> = gitProvider(project, listOf()) {
        ProcessHelpers.captureLines(
            "git", "-C", project.rootProject.projectDir.absolutePath, "shortlog", "-ns",
            "--group=author", "--group=trailer:co-authored-by", "HEAD",
        )
            .asSequence()
            .map {
                val matcher = COMMIT_COUNTS.matcher(it)
                matcher.find()
                matcher.group(1)
            }
            .filter { !IGNORED_USERS.contains(it) }
            .toList()
            .sortedWith(String.CASE_INSENSITIVE_ORDER)
    }

    /**
     * References to other sources
     */
    val sourceDirectories: SetProperty<SourceSetReference> = project.objects.setProperty(SourceSetReference::class.java)

    /**
     * Dependencies excluded from published artifacts.
     */
    private val excludedDeps: ListProperty<Dependency> = project.objects.listProperty(Dependency::class.java)

    /** All source sets referenced by this project. */
    val sourceSets = sourceDirectories.map { x -> x.map { it.sourceSet } }

    init {
        sourceDirectories.finalizeValueOnRead()
        excludedDeps.finalizeValueOnRead()
        project.afterEvaluate { sourceDirectories.disallowChanges() }
    }

    /**
     * Mark this project as consuming another project. Its [sourceDirectories] are added, allowing easier configuration
     * of run configurations and other tasks which consume sources/classes.
     */
    fun externalSources(project: Project) {
        val otherCct = project.extensions.getByType(CCTweakedExtension::class.java)
        for (sourceSet in otherCct.sourceDirectories.get()) {
            sourceDirectories.add(SourceSetReference(sourceSet.sourceSet, classes = sourceSet.classes, external = true))
        }
    }

    /**
     * Add a dependency on another project such that its sources and compiles are processed with this one.
     *
     * This is used when importing a common library into a loader-specific one, as we want to compile sources using
     * the loader-specific sources.
     */
    fun inlineProject(path: String) {
        val otherProject = project.evaluationDependsOn(path)
        val otherJava = otherProject.extensions.getByType(JavaPluginExtension::class.java)
        val main = otherJava.sourceSets.getByName("main")
        val client = otherJava.sourceSets.getByName("client")
        val testMod = otherJava.sourceSets.findByName("testMod")
        val testFixtures = otherJava.sourceSets.findByName("testFixtures")

        // Pull in sources from the other project.
        extendSourceSet(otherProject, main)
        extendSourceSet(otherProject, client)
        if (testMod != null) extendSourceSet(otherProject, testMod)
        if (testFixtures != null) extendSourceSet(otherProject, testFixtures)

        // The extra source-processing tasks should include these files too.
        project.tasks.named(main.javadocTaskName, Javadoc::class.java) { source(main.allJava, client.allJava) }
        project.tasks.named(main.sourcesJarTaskName, Jar::class.java) { from(main.allSource, client.allSource) }
        sourceDirectories.addAll(SourceSetReference.inline(main), SourceSetReference.inline(client))
    }

    /**
     * Extend a source set with files from another project.
     *
     * This actually extends the original compile tasks, as extending the source sets does not play well with IDEs.
     */
    private fun extendSourceSet(otherProject: Project, sourceSet: SourceSet) {
        project.tasks.named(sourceSet.compileJavaTaskName, JavaCompile::class.java) {
            dependsOn(otherProject.tasks.named(sourceSet.compileJavaTaskName)) // Avoid duplicate compile errors
            source(sourceSet.allJava)
        }

        project.tasks.named(sourceSet.processResourcesTaskName, ProcessResources::class.java) {
            from(sourceSet.resources)
        }

        // Also try to depend on Kotlin if it exists
        val kotlin = otherProject.extensions.findByType(KotlinProjectExtension::class.java)
        if (kotlin != null) {
            val compileKotlin = sourceSet.getCompileTaskName("kotlin")
            project.tasks.named(compileKotlin, KotlinCompile::class.java) {
                dependsOn(otherProject.tasks.named(compileKotlin))
                source(kotlin.sourceSets.getByName(sourceSet.name).kotlin)
            }
        }

        // If we're doing an IDE sync, add a fake dependency to ensure it's on the classpath.
        if (isIdeSync) project.dependencies.add(sourceSet.apiConfigurationName, sourceSet.output)
    }

    fun linters(@Suppress("UNUSED_PARAMETER") vararg unused: UseNamedArgs, minecraft: Boolean, loader: String?) {
        val java = project.extensions.getByType(JavaPluginExtension::class.java)
        val sourceSets = java.sourceSets

        project.dependencies.run { add("errorprone", project(mapOf("path" to ":lints"))) }
        sourceSets.all {
            val name = name
            project.tasks.named(compileJavaTaskName, JavaCompile::class.java) {
                options.errorprone {
                    // Only the main source set should run the side checker
                    check("SideChecker", if (minecraft && name == "main") CheckSeverity.DEFAULT else CheckSeverity.OFF)

                    // The MissingLoaderOverride check superseeds the MissingOverride one, so disable that.
                    if (loader != null) {
                        check("MissingOverride", CheckSeverity.OFF)
                        option("ModLoader", loader)
                    } else {
                        check("LoaderOverride", CheckSeverity.OFF)
                        check("MissingLoaderOverride", CheckSeverity.OFF)
                    }
                }
            }
        }
    }

    fun <T> jacoco(task: NamedDomainObjectProvider<T>) where T : Task, T : JavaForkOptions {
        val classDump = project.layout.buildDirectory.dir("jacocoClassDump/${task.name}")
        val reportTaskName = "jacoco${task.name.capitalise()}Report"

        val jacoco = project.extensions.getByType(JacocoPluginExtension::class.java)
        task.configure {
            finalizedBy(reportTaskName)

            doFirst("Clean class dump directory") { fs.delete { delete(classDump) } }

            jacoco.applyTo(this)
            extensions.configure(JacocoTaskExtension::class.java) {
                includes = listOf("dan200.computercraft.*")
                classDumpDir = classDump.get().asFile

                // Older versions of modlauncher don't include a protection domain (and thus no code
                // source). Jacoco skips such classes by default, so we need to explicitly include them.
                isIncludeNoLocationClasses = true
            }
        }

        project.tasks.register(reportTaskName, JacocoReport::class.java) {
            group = LifecycleBasePlugin.VERIFICATION_GROUP
            description = "Generates code coverage report for the ${task.name} task."

            executionData(task.get())
            classDirectories.from(classDump)

            // Don't want to use sourceSets(...) here as we have a custom class directory.
            for (ref in sourceSets.get()) sourceDirectories.from(ref.allSource.sourceDirectories)
        }

        project.extensions.configure(ReportingExtension::class.java) {
            reports.register("${task.name}CodeCoverageReport", JacocoCoverageReport::class.java) {
                testType.set(TestSuiteType.INTEGRATION_TEST)
            }
        }
    }

    /**
     * Download a file by creating a dummy Ivy repository.
     *
     * This should only be used for one-off downloads. Using a more conventional Ivy or Maven repository is preferred
     * where possible.
     */
    fun downloadFile(label: String, url: String): File {
        val uri = URI(url)
        val path = File(uri.path)

        project.repositories.ivy {
            name = label
            setUrl(URI(uri.scheme, uri.userInfo, uri.host, uri.port, path.parent, null, null))
            patternLayout {
                artifact("[artifact].[ext]")
            }
            metadataSources {
                artifact()
            }
            content {
                includeModule("cc.tweaked.internal", path.nameWithoutExtension)
            }
        }

        return project.configurations.detachedConfiguration(
            project.dependencies.create(
                mapOf(
                    "group" to "cc.tweaked.internal",
                    "name" to path.nameWithoutExtension,
                    "ext" to path.extension,
                ),
            ),
        ).resolve().single()
    }

    /**
     * Exclude a dependency from being published in Maven.
     */
    fun exclude(dep: Dependency) {
        excludedDeps.add(dep)
    }

    /**
     * Configure a [MavenDependencySpec].
     */
    fun configureExcludes(spec: MavenDependencySpec) {
        for (dep in excludedDeps.get()) spec.exclude(dep)
    }

    companion object {
        private val COMMIT_COUNTS = Pattern.compile("""^\s*[0-9]+\s+(.*)$""")
        private val IGNORED_USERS = setOf(
            "GitHub", "Daniel Ratcliffe", "Weblate",
        )

        private fun <T> gitProvider(project: Project, default: T, supplier: () -> T): Provider<T> {
            return project.provider {
                try {
                    supplier()
                } catch (e: IOException) {
                    project.logger.error("Cannot read Git repository: ${e.message}")
                    default
                } catch (e: GradleException) {
                    project.logger.error("Cannot read Git repository: ${e.message}")
                    default
                }
            }
        }

        private val isIdeSync: Boolean
            get() = java.lang.Boolean.parseBoolean(System.getProperty("idea.sync.active", "false"))
    }
}
