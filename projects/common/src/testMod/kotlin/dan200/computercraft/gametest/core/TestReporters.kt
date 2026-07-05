// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.gametest.core

import com.google.common.base.Stopwatch
import net.minecraft.gametest.framework.GameTestInfo
import net.minecraft.gametest.framework.JUnitLikeTestReporter
import net.minecraft.gametest.framework.TestReporter
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.*
import java.nio.file.Files.createDirectories
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.TransformerException
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

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
 * directory exists and includes the stack trace in the error.
 */
open class JunitTestReporter(private val destination: File) : TestReporter {
    private val document: Document
    private val testSuite: Element
    private val stopwatch: Stopwatch = Stopwatch.createStarted()

    init {
        document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument()

        testSuite = document.createElement("testsuite")
        testSuite.setAttribute("timestamp", DateTimeFormatter.ISO_INSTANT.format(Instant.now()))

        val container = document.createElement("testsuite")
        container.appendChild(testSuite)
        document.appendChild(container)
    }

    private fun createTestCase(testInfo: GameTestInfo): Element {
        val testCase = document.createElement("testcase")
        testCase.setAttribute("name", testInfo.id().toString())
        testCase.setAttribute("classname", testInfo.structure.toString())
        testCase.setAttribute("time", (testInfo.runTime.toDouble() / 1000.0).toString())
        testSuite.appendChild(testCase)
        return testCase
    }

    override fun onTestFailed(testInfo: GameTestInfo) {
        val error = testInfo.error!!
        val result: Element
        if (testInfo.isRequired) {
            result = document.createElement("failure")
            result.setAttribute("message", error.message)
            result.setAttribute("type", error.javaClass.name)

            val writer = StringWriter()
            error.printStackTrace(PrintWriter(writer))
            result.textContent = writer.toString()
        } else {
            result = document.createElement("skipped")
            result.setAttribute("message", error.message)
        }

        createTestCase(testInfo).appendChild(result)
    }

    override fun onTestSuccess(testInfo: GameTestInfo) {
        createTestCase(testInfo)
    }

    override fun finish() {
        stopwatch.stop()
        testSuite.setAttribute(
            "time",
            (stopwatch.elapsed(TimeUnit.MILLISECONDS).toDouble() / 1000.0).toString(),
        )

        try {
            try {
                createDirectories(destination.toPath().parent)
            } catch (e: IOException) {
                throw UncheckedIOException("Failed to create parent directory", e)
            }

            TransformerFactory.newInstance().newTransformer().transform(DOMSource(document), StreamResult(destination))
        } catch (transformerException: TransformerException) {
            throw Error("Couldn't save test report", transformerException)
        }
    }
}
