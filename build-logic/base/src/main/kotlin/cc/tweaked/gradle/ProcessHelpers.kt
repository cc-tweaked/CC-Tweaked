// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package cc.tweaked.gradle

import org.gradle.api.GradleException
import java.io.File

internal object ProcessHelpers {
    fun onPath(name: String): Boolean {
        val path = System.getenv("PATH") ?: return false
        return path.splitToSequence(File.pathSeparator).any { File(it, name).exists() }
    }

    /**
     * Search for an executable on the `PATH` if required.
     *
     * [Process]/[ProcessBuilder] does not handle all executable file extensions on Windows (such as `.com). When on
     * Windows, this function searches `PATH` and `PATHEXT` for an executable matching [name].
     */
    fun getExecutable(name: String): String {
        if (!System.getProperty("os.name").lowercase().contains("windows")) return name

        val path = (System.getenv("PATH") ?: return name).split(File.pathSeparator)
        val pathExt = (System.getenv("PATHEXT") ?: return name).split(File.pathSeparator)

        for (pathEntry in path) {
            for (ext in pathExt) {
                val resolved = File(pathEntry, name + ext)
                if (resolved.exists()) return resolved.getAbsolutePath()
            }
        }

        return name
    }
}

internal fun Process.waitForOrThrow(message: String) {
    val ret = waitFor()
    if (ret != 0) throw GradleException("$message (exited with $ret)")
}
