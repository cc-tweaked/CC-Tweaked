// SPDX-FileCopyrightText: 2026 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package cc.tweaked.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileTree
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.nio.file.Files

/**
 * Build a Javascript file containing an archive of all files in a folder.
 */
abstract class JsArchive : DefaultTask() {
    /**
     * The version to include in this file.
     */
    @get:Input
    abstract val version: Property<String>

    /**
     * The subdirectory within [input] to visit.
     *
     * Gradle doesn't have a good way to take the subdirectory of a [org.gradle.api.file.FileTree] or
     * [ConfigurableFileCollection]. It's possible to do some crazy things with a [org.gradle.api.file.CopySpec]
     * (https://docs.gradle.org/current/userguide/working_with_files.html#sec:unpacking_archives_example), but
     * unfortunately that's not accessible to us.
     */
    @get:Input
    abstract val prefix: Property<String>

    /**
     * The files to copy from.
     */
    @get:Internal
    abstract val input: ConfigurableFileCollection

    @get:InputFiles
    val inputFiles: FileTree
        get() = input.asFileTree.matching { include(prefix.get()) }

    /**
     * The output file to write to.
     */
    @get:OutputFile
    abstract val output: RegularFileProperty

    init {
        version.convention(project.provider { project.version as String? })
        prefix.convention("")
    }

    @TaskAction
    fun run() {
        Files.newBufferedWriter(output.get().asFile.toPath()).use { out ->
            out.write("export const version = ")
            writeJsonString(out, version.get())
            out.write(";\n")

            out.write("export const resources = {\n")
            val prefix = this.prefix.get()
            inputFiles.visit {
                if (isDirectory) return@visit
                out.write("  ")
                writeJsonString(out, relativePath.pathString.removePrefix(prefix))
                out.write(": ")
                writeJsonString(out, open().use { InputStreamReader(it, StandardCharsets.UTF_8).readAllAsString() })
                out.write(",\n")
            }
            out.write("};\n")
        }
    }

    private fun writeJsonString(out: Appendable, string: String) {
        out.append('"')
        for (i in string.indices) {
            when (val chr: Char = string[i]) {
                '\n' -> out.append("\\n")
                '\r' -> out.append("\\r")
                '\t' -> out.append("\\t")
                '\b' -> out.append("\\b")
                '\\' -> out.append("\\\\")
                '"' -> out.append("\\\"")
                else -> out.append(chr)
            }
        }
        out.append('"')
    }
}
