// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package cc.tweaked.gradle

import org.jetbrains.gradle.ext.JUnit
import javax.inject.Inject

/**
 * A version of [JUnit] with a functional [className].
 *
 * See [#92](https://github.com/JetBrains/gradle-idea-ext-plugin/issues/92).
 */
open class JUnitExt @Inject constructor(nameParam: String) : JUnit(nameParam) {
    override fun toMap(): MutableMap<String, *> {
        val map = HashMap(super.toMap())
        // Should be "class" instead of "className".
        // See https://github.com/JetBrains/intellij-community/blob/9ba394021dc73a3926f13d6d6cdf434f9ee7046d/plugins/junit/src/com/intellij/execution/junit/JUnitRunConfigurationImporter.kt#L39
        map["class"] = className
        return map
    }
}
