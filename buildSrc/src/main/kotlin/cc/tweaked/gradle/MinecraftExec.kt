// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package cc.tweaked.gradle

import net.minecraftforge.gradle.common.util.RunConfig
import org.gradle.api.GradleException
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.invocation.Gradle
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.getByName
import org.gradle.language.base.plugins.LifecycleBasePlugin
import java.io.File
import java.nio.file.Files
import java.util.concurrent.TimeUnit
import java.util.function.Supplier
import javax.inject.Inject
import kotlin.random.Random

/**
 * A [JavaExec] task for client-tests. This sets some common setup, and uses [MinecraftRunnerService] to ensure only one
 * test runs at once.
 */
abstract class ClientJavaExec : JavaExec() {
    private val clientRunner: Provider<MinecraftRunnerService> = MinecraftRunnerService.get(project.gradle)

    init {
        group = LifecycleBasePlugin.VERIFICATION_GROUP
        usesService(clientRunner)
    }

    @get:Input
    val renderdoc get() = project.hasProperty("renderdoc")

    /**
     * When [false], tests will not be run automatically, allowing the user to debug rendering.
     */
    @get:Input
    val clientDebug get() = renderdoc || project.hasProperty("clientDebug")

    /**
     * When [false], tests will not run under a framebuffer.
     */
    @get:Input
    val useFramebuffer get() = !clientDebug && !project.hasProperty("clientNoFramebuffer")

    /**
     * The path test results are written to.
     */
    @get:OutputFile
    val testResults = project.layout.buildDirectory.file("test-results/$name.xml")

    private fun setTestProperties() {
        if (!clientDebug) systemProperty("cctest.client", "")
        if (renderdoc) environment("LD_PRELOAD", "/usr/lib/librenderdoc.so")
        systemProperty("cctest.gametest-report", testResults.get().asFile.absoluteFile)
        workingDir(project.layout.buildDirectory.dir("gametest/$name"))
    }

    init {
        setTestProperties()
    }

    /**
     * Set this task to run a given [RunConfig].
     */
    fun setRunConfig(config: RunConfig) {
        (this as JavaExec).setRunConfig(config)
        setTestProperties() // setRunConfig may clobber some properties, ensure everything is set.
    }

    /**
     * Copy configuration from a task with the given name.
     */
    fun copyFrom(path: String) = copyFrom(project.tasks.getByName(path, JavaExec::class))

    /**
     * Copy configuration from an existing [JavaExec] task.
     */
    fun copyFrom(task: JavaExec) {
        for (dep in task.dependsOn) dependsOn(dep)
        task.copyToFull(this)
        setTestProperties() // copyToFull may clobber some properties, ensure everything is set.
    }

    /**
     * Only run tests with the given tags.
     */
    fun tags(vararg tags: String) {
        systemProperty("cctest.tags", tags.joinToString(","))
    }

    /**
     * Write a file with the given contents before starting Minecraft. This may be useful for writing config files.
     */
    fun withFileContents(path: Any, contents: Supplier<String>) {
        val file = project.file(path).toPath()
        doFirst {
            Files.createDirectories(file.parent)
            Files.writeString(file, contents.get())
        }
    }

    /**
     * Copy a file to the provided path before starting Minecraft. This copy only occurs if the file does not already
     * exist.
     */
    fun withFileFrom(path: Any, source: Supplier<File>) {
        val file = project.file(path).toPath()
        doFirst {
            Files.createDirectories(file.parent)
            if (!Files.exists(file)) Files.copy(source.get().toPath(), file)
        }
    }

    @TaskAction
    override fun exec() {
        Files.createDirectories(workingDir.toPath())
        fsOperations.delete { delete(workingDir.resolve("screenshots")) }

        if (useFramebuffer) {
            clientRunner.get().wrapClient(this) { super.exec() }
        } else {
            super.exec()
        }
    }

    @get:Inject
    protected abstract val fsOperations: FileSystemOperations
}

/**
 * A service for [JavaExec] tasks which start Minecraft.
 *
 * Tasks may run `usesService(MinecraftRunnerService.get(gradle))` to ensure that only one Minecraft-related task runs
 * at once.
 */
abstract class MinecraftRunnerService : BuildService<BuildServiceParameters.None> {
    private val hasXvfb = lazy {
        System.getProperty("os.name", "").equals("linux", ignoreCase = true) && ProcessHelpers.onPath("xvfb-run")
    }

    internal fun wrapClient(exec: JavaExec, run: () -> Unit) = when {
        hasXvfb.value -> runXvfb(exec, run)
        else -> run()
    }

    /**
     * Run a program under Xvfb, preventing it spawning a window.
     */
    private fun runXvfb(exec: JavaExec, run: () -> Unit) {
        fun ProcessBuilder.startVerbose(): Process {
            exec.logger.info("Running ${this.command()}")
            return start()
        }

        CloseScope().use { scope ->
            val dir = Files.createTempDirectory("cctweaked").toAbsolutePath()
            scope.add { fsOperations.delete { delete(dir) } }

            val authFile = Files.createTempFile(dir, "Xauthority", "").toAbsolutePath()

            val cookie = StringBuilder().also {
                for (i in 0..31) it.append("0123456789abcdef"[Random.nextInt(16)])
            }.toString()

            val xvfb =
                ProcessBuilder("Xvfb", "-displayfd", "1", "-screen", "0", "640x480x24", "-nolisten", "tcp").also {
                    it.inheritIO()
                    it.environment()["XAUTHORITY"] = authFile.toString()
                    it.redirectOutput(ProcessBuilder.Redirect.PIPE)
                }.startVerbose()
            scope.add { xvfb.destroyForcibly().waitFor() }

            val server = xvfb.inputReader().use { it.readLine().trim() }
            exec.logger.info("Running at :$server (XAUTHORITY=$authFile.toA")

            ProcessBuilder("xauth", "add", ":$server", ".", cookie).also {
                it.inheritIO()
                it.environment()["XAUTHORITY"] = authFile.toString()
            }.startVerbose().waitForOrThrow("Failed to setup XAuthority file")

            scope.add {
                ProcessBuilder("xauth", "remove", ":$server").also {
                    it.inheritIO()
                    it.environment()["XAUTHORITY"] = authFile.toString()
                }.startVerbose().waitFor()
            }

            // Wait a few seconds for Xvfb to start. Ugly, but identical to xvfb-run.
            if (xvfb.waitFor(3, TimeUnit.SECONDS)) {
                throw GradleException("Xvfb unexpectedly exited (with status code ${xvfb.exitValue()})")
            }

            exec.environment("XAUTHORITY", authFile.toString())
            exec.environment("DISPLAY", ":$server")

            run()
        }
    }

    @get:Inject
    protected abstract val fsOperations: FileSystemOperations

    companion object {
        fun get(gradle: Gradle): Provider<MinecraftRunnerService> =
            gradle.sharedServices.registerIfAbsent("cc.tweaked.gradle.ClientJavaExec", MinecraftRunnerService::class.java) {
                maxParallelUsages.set(1)
            }
    }
}
