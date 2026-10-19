package cc.tweaked.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileTree
import org.gradle.api.problems.Problem
import org.gradle.api.problems.ProblemGroup
import org.gradle.api.problems.ProblemId
import org.gradle.api.problems.Problems
import org.gradle.api.tasks.*
import java.io.File
import java.time.LocalDate
import javax.inject.Inject

abstract class AbstractEnsurePackageInfo : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sources: ConfigurableFileCollection

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val additionalSources: ConfigurableFileCollection

    @get:Inject
    protected abstract val problems: Problems

    protected fun gatherPackages(): Map<String, Set<File>> {
        val reporter = problems.reporter

        val existingPackages = mutableMapOf<String, File>()
        forEachFileInPackage(additionalSources.asFileTree) { _, packageName, packageInfo ->
            if (packageInfo.exists()) existingPackages.putIfAbsent(packageName, packageInfo.parentFile)
        }
        logger.info("Found existing packages: ${existingPackages.keys}")

        val packages = mutableMapOf<String, MutableSet<File>>()
        forEachFileInPackage(sources.asFileTree) { file, packageName, packageInfo ->
            val existing = existingPackages[packageName]
            if (existing != null) {
                reporter.report(MULTIPLE_DIRS) {
                    contextualLabel("Package '$packageName' is defined in multiple source sets")
                    fileLocation(file.path)
                    fileLocation(existing.path)
                }
            } else {
                packages.computeIfAbsent(packageName) { mutableSetOf() }.add(packageInfo)
            }
        }

        return packages
    }

    private fun forEachFileInPackage(sources: FileTree, consumer: (file: File, packageName: String, packageInfo: File) -> Unit) {
        sources.visit {
            if (isDirectory) return@visit

            if (!relativePath.lastName.endsWith(".java")) return@visit

            val packagePath = relativePath.parent ?: return@visit
            val packageName = packagePath.pathString.replace('/', '.')
            val packageInfo = file.parentFile.resolve("package-info.java")
            consumer(file, packageName, packageInfo)
        }
    }

    protected fun makePackageInfo(packageName: String): String {
        return """
            // SPDX-FileCopyrightText: ${LocalDate.now().year} The CC: Tweaked Developers
            //
            // SPDX-License-Identifier: MPL-2.0

            @NullMarked
            package $packageName;

            import org.jspecify.annotations.NullMarked;
        """.trimIndent() + "\n"
    }

    companion object {
        private val GROUP = ProblemGroup.create("cc.tweaked.ensure-package-infos", "Ensure package-info.java")

        @JvmStatic
        protected val NO_PACKAGE_INFO = ProblemId.create("no-package-info", "Missing package-info.java", GROUP)

        @JvmStatic
        protected val MULTIPLE_DIRS =
            ProblemId.create("multiple-dirs", "Package is defined in multiple source sets", GROUP)
    }
}

/**
 * Ensure all packages in a source set have a `package-info.java` file.
 */
abstract class EnsurePackageInfo : AbstractEnsurePackageInfo() {
    @TaskAction
    fun run() {
        val reporter = problems.reporter
        val packages = gatherPackages()
        val problems = mutableListOf<Problem>()
        for ((packageName, packageInfos) in packages) {
            if (packageInfos.any { it.exists() }) continue

            val location = when (val packageInfo = packageInfos.singleOrNull()) {
                null -> "one of " + packageInfos.joinToString(", ") { "'$it'" }
                else -> {
                    packageInfo.writeText(makePackageInfo(packageName))
                    "'$packageInfo'"
                }
            }
            val problem = reporter.create(NO_PACKAGE_INFO) {
                contextualLabel("Package '$packageName' is missing a package-info.java file at $location")
            }
            problems.add(problem)
        }

        if (problems.isNotEmpty()) throw reporter.throwing(GradleException("Missing package-infos"), problems)
    }
}

/**
 * Generate missing `package-info.java` files into a separate directory.
 */
abstract class GeneratePackageInfo : AbstractEnsurePackageInfo() {
    @get:OutputDirectory
    abstract val output: DirectoryProperty

    @TaskAction
    fun run() {
        val packages = gatherPackages()
        for ((packageName, packageInfos) in packages) {
            if (packageInfos.any { it.exists() }) continue

            val packageFile = output.get().dir(packageName.replace('.', '/')).file("package-info.java").asFile
            packageFile.parentFile.mkdirs()
            packageFile.writeText(makePackageInfo(packageName))
        }
    }
}
