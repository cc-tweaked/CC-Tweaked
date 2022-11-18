package cc.tweaked.gradle

import org.codehaus.groovy.runtime.ProcessGroovyMethods
import org.gradle.api.GradleException
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader

internal object ProcessHelpers {
    fun startProcess(vararg command: String): Process {
        // Something randomly passes in "GIT_DIR=" as an environment variable which clobbers everything else. Don't
        // inherit the environment array!
        return Runtime.getRuntime().exec(command, arrayOfNulls(0))
    }

    fun captureOut(vararg command: String): String {
        val process = startProcess(*command)
        val result = ProcessGroovyMethods.getText(process)
        if (process.waitFor() != 0) throw IOException("Command exited with a non-0 status")
        return result
    }

    fun captureLines(vararg command: String): List<String> {
        return captureLines(startProcess(*command))
    }

    fun captureLines(process: Process): List<String> {
        val out = BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
            reader.lines().filter { it.isNotEmpty() }.toList()
        }
        ProcessGroovyMethods.closeStreams(process)
        if (process.waitFor() != 0) throw IOException("Command exited with a non-0 status")
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
