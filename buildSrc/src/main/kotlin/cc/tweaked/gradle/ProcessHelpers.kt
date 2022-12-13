package cc.tweaked.gradle

import org.codehaus.groovy.runtime.ProcessGroovyMethods
import org.gradle.api.GradleException
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

internal object ProcessHelpers {
    fun startProcess(vararg command: String): Process {
        // Something randomly passes in "GIT_DIR=" as an environment variable which clobbers everything else. Don't
        // inherit the environment array!
        return ProcessBuilder()
            .command(*command)
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .also { it.environment().clear() }
            .start()
    }

    fun captureOut(vararg command: String): String {
        val process = startProcess(*command)
        process.outputStream.close()

        val result = ProcessGroovyMethods.getText(process)
        process.waitForOrThrow("Failed to run command")
        return result
    }

    fun captureLines(vararg command: String): List<String> {
        val process = startProcess(*command)
        process.outputStream.close()

        val out = BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
            reader.lines().filter { it.isNotEmpty() }.toList()
        }
        ProcessGroovyMethods.closeStreams(process)
        process.waitForOrThrow("Failed to run command")
        return out
    }

    fun onPath(name: String): Boolean {
        val path = System.getenv("PATH") ?: return false
        return path.splitToSequence(File.pathSeparator).any { File(it, name).exists() }
    }
}

internal fun Process.waitForOrThrow(message: String) {
    val ret = waitFor()
    if (ret != 0) throw GradleException("$message (exited with $ret)")
}
