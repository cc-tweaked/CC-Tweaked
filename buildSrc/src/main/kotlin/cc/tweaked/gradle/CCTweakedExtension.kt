package cc.tweaked.gradle

import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.errorprone.errorprone
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Project
import org.gradle.api.attributes.TestSuiteType
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.api.reporting.ReportingExtension
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.configurationcache.extensions.capitalized
import org.gradle.kotlin.dsl.get
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.gradle.language.jvm.tasks.ProcessResources
import org.gradle.testing.jacoco.plugins.JacocoCoverageReport
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import org.gradle.testing.jacoco.tasks.JacocoReport
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.BufferedWriter
import java.io.IOException
import java.io.OutputStreamWriter
import java.util.regex.Pattern

abstract class CCTweakedExtension(
    private val project: Project,
    private val fs: FileSystemOperations,
) {
    /** Get the hash of the latest git commit. */
    val gitHash: Provider<String> = gitProvider(project, "<no git hash>") {
        ProcessHelpers.captureOut("git", "-C", project.projectDir.absolutePath, "rev-parse", "HEAD").trim()
    }

    /** Get the current git branch. */
    val gitBranch: Provider<String> = gitProvider(project, "<no git branch>") {
        ProcessHelpers.captureOut("git", "-C", project.projectDir.absolutePath, "rev-parse", "--abbrev-ref", "HEAD")
            .trim()
    }

    /** Get a list of all contributors to the project. */
    val gitContributors: Provider<List<String>> = gitProvider(project, listOf()) {
        val authors: Set<String> = HashSet(
            ProcessHelpers.captureLines(
                "git", "-C", project.projectDir.absolutePath, "log",
                "--format=tformat:%an <%ae>%n%cn <%ce>%n%(trailers:key=Co-authored-by,valueonly)",
            ),
        )
        val process = ProcessHelpers.startProcess("git", "check-mailmap", "--stdin")
        BufferedWriter(OutputStreamWriter(process.outputStream)).use { writer ->
            for (authorName in authors) {
                var author = authorName

                if (author.isEmpty()) continue
                if (!author.endsWith(">")) author += ">" // Some commits have broken Co-Authored-By lines!
                writer.write(author)
                writer.newLine()
            }
        }
        val contributors: MutableSet<String> = HashSet()
        for (authorLine in ProcessHelpers.captureLines(process)) {
            val matcher = EMAIL.matcher(authorLine)
            matcher.find()
            val name = matcher.group(1)
            if (!IGNORED_USERS.contains(name)) contributors.add(name)
        }

        contributors.sortedWith(String.CASE_INSENSITIVE_ORDER)
    }

    /**
     * References to other sources
     */
    val sourceDirectories: SetProperty<SourceSetReference> =
        project.objects.setProperty(SourceSetReference::class.java)

    /** All source sets referenced by this project. */
    val sourceSets = sourceDirectories.map { x -> x.map { it.sourceSet } }

    init {
        sourceDirectories.finalizeValueOnRead()
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

    fun jacoco(task: NamedDomainObjectProvider<JavaExec>) {
        val classDump = project.buildDir.resolve("jacocoClassDump/${task.name}")
        val reportTaskName = "jacoco${task.name.capitalized()}Report"

        val jacoco = project.extensions.getByType(JacocoPluginExtension::class.java)
        task.configure {
            finalizedBy(reportTaskName)

            doFirst("Clean class dump directory") { fs.delete { delete(classDump) } }

            jacoco.applyTo(this)
            extensions.configure(JacocoTaskExtension::class.java) {
                includes = listOf("dan200.computercraft.*")
                classDumpDir = classDump

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
            val sourceSets = project.extensions.getByType(SourceSetContainer::class.java)
            sourceDirectories.from(sourceSets["main"].allSource.sourceDirectories)
        }

        project.extensions.configure(ReportingExtension::class.java) {
            reports.register("${task.name}CodeCoverageReport", JacocoCoverageReport::class.java) {
                testType.set(TestSuiteType.INTEGRATION_TEST)
            }
        }
    }

    companion object {
        private val EMAIL = Pattern.compile("^([^<]+) <.+>$")
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
                }
            }
        }

        internal val isIdeSync: Boolean
            get() = java.lang.Boolean.parseBoolean(System.getProperty("idea.sync.active", "false"))
    }
}
