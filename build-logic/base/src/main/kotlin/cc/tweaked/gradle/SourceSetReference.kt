// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package cc.tweaked.gradle

import org.gradle.api.tasks.SourceSet

data class SourceSetReference(
    val sourceSet: SourceSet,
    val classes: Boolean,
    val external: Boolean,
) {
    companion object {
        /** A source set in the current project. */
        fun internal(sourceSet: SourceSet) = SourceSetReference(sourceSet, classes = true, external = false)

        /** A source set from another project. */
        fun external(sourceSet: SourceSet) = SourceSetReference(sourceSet, classes = true, external = true)

        /** A source set which is inlined into the current project. */
        fun inline(sourceSet: SourceSet) = SourceSetReference(sourceSet, classes = false, external = false)
    }
}
