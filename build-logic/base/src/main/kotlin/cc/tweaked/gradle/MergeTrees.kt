// SPDX-FileCopyrightText: 2024 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package cc.tweaked.gradle

import cc.tweaked.vanillaextract.core.util.MoreFiles
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.*
import javax.inject.Inject

/**
 * Merge common files across multiple directories into one destination directory.
 *
 * This is intended for merging the generated resources from the Forge and Fabric projects. Files common between the two
 * are written to the global [output] directory, while distinct files are written to the per-source
 * [MergeTrees.Source.output] directory.
 */
abstract class MergeTrees : DefaultTask() {
    /**
     * A source directory to read from.
     */
    interface Source {
        /**
         * The folder(s) containing all input files.
         */
        @get:InputFiles
        @get:PathSensitive(PathSensitivity.RELATIVE)
        val input: ConfigurableFileCollection

        /**
         * The folder to write files unique to this folder to.
         */
        @get:OutputDirectory
        val output: DirectoryProperty
    }

    /**
     * The list of sources.
     */
    @get:Nested
    abstract val sources: ListProperty<Source>

    /**
     * Add and configure a new source.
     */
    fun source(configure: Action<Source>) {
        val instance = objectFactory.newInstance(Source::class.java)
        configure.execute(instance)
        instance.output.disallowChanges()
        sources.add(instance)
    }

    /**
     * The directory to write common files to.
     */
    @get:OutputDirectory
    abstract val output: DirectoryProperty

    @get:Inject
    protected abstract val objectFactory: ObjectFactory

    @get:Inject
    protected abstract val fsOperations: FileSystemOperations

    @TaskAction
    fun run() {
        val sources = this.sources.get()
        if (sources.isEmpty()) throw GradleException("Cannot have an empty list of sources")

        val files = mutableMapOf<String, SharedFile>()
        for (source in sources) {
            source.input.asFileTree.visit {
                if (isDirectory) return@visit

                val path = path
                val hash = MoreFiles.computeSha1(file.toPath())

                val existing = files[path]
                if (existing == null) {
                    files[path] = SharedFile(hash, 1)
                } else if (existing.hash == hash) {
                    existing.found++
                }
            }
        }

        val sharedFiles =
            files.entries.asSequence().filter { (_, v) -> v.found == sources.size }.map { (k, _) -> k }.toList()

        // Copy shared files to the common directory
        fsOperations.sync {
            from(sources[0].input.asFileTree)
            into(output)
            include(sharedFiles)
        }

        // And all other files to their per-source directory
        for (source in sources) {
            fsOperations.sync {
                from(source.input.asFileTree)
                into(source.output)
                exclude(sharedFiles)
            }
        }
    }

    class SharedFile(val hash: String, var found: Int)
}
