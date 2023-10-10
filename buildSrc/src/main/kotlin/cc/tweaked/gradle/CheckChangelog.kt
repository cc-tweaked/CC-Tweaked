// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package cc.tweaked.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.language.base.plugins.LifecycleBasePlugin

/**
 * Checks the `changelog.md` and `whatsnew.md` files are well-formed.
 */
@CacheableTask
abstract class CheckChangelog : DefaultTask() {
    init {
        group = LifecycleBasePlugin.VERIFICATION_GROUP
        description = "Verifies the changelog and whatsnew file are consistent."
    }

    @get:Input
    abstract val version: Property<String>

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val changelog: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val whatsNew: RegularFileProperty

    @TaskAction
    fun check() {
        val version = version.get()

        var ok = true

        // Check we're targeting the current version
        var whatsNew = whatsNew.get().asFile.readLines()
        if (whatsNew[0] != "New features in CC: Tweaked $version") {
            ok = false
            logger.error("Expected `whatsnew.md' to target $version.")
        }

        // Check "read more" exists and trim it
        val idx = whatsNew.indexOfFirst { it == "Type \"help changelog\" to see the full version history." }
        if (idx == -1) {
            ok = false
            logger.error("Must mention the changelog in whatsnew.md")
        } else {
            whatsNew = whatsNew.slice(0 until idx)
        }

        // Check whatsnew and changelog match.
        val expectedChangelog = sequenceOf("# ${whatsNew[0]}") + whatsNew.slice(1 until whatsNew.size).asSequence()
        val changelog = changelog.get().asFile.readLines()
        val mismatch = expectedChangelog.zip(changelog.asSequence()).filter { (a, b) -> a != b }.firstOrNull()
        if (mismatch != null) {
            ok = false
            logger.error("whatsnew and changelog are not in sync")
        }

        if (!ok) throw GradleException("Could not check release")
    }
}
