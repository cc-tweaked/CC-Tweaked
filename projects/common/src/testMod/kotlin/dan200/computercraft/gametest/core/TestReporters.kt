// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.gametest.core

import net.minecraft.gametest.framework.GameTestInfo
import net.minecraft.gametest.framework.JUnitLikeTestReporter
import net.minecraft.gametest.framework.TestReporter
import java.io.File
import java.io.IOException
import java.nio.file.Files
import javax.xml.transform.TransformerException

/**
 * A test reporter which delegates to a list of other reporters.
 */
class MultiTestReporter(private val reporters: List<TestReporter>) : TestReporter {
    constructor(vararg reporters: TestReporter) : this(listOf(*reporters))

    override fun onTestFailed(test: GameTestInfo) {
        for (reporter in reporters) reporter.onTestFailed(test)
    }

    override fun onTestSuccess(test: GameTestInfo) {
        for (reporter in reporters) reporter.onTestSuccess(test)
    }

    override fun finish() {
        for (reporter in reporters) reporter.finish()
    }
}

/**
 * Reports tests to a JUnit XML file. This is equivalent to [JUnitLikeTestReporter], except it ensures the destination
 * directory exists.
 */
class JunitTestReporter constructor(destination: File) : JUnitLikeTestReporter(destination) {
    override fun save(file: File) {
        try {
            Files.createDirectories(file.toPath().parent)
        } catch (e: IOException) {
            throw TransformerException("Failed to create parent directory", e)
        }
        super.save(file)
    }
}
