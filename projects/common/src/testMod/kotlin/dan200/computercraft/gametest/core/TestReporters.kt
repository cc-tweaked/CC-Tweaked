// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.gametest.core

import net.minecraft.gametest.framework.GameTestInfo
import net.minecraft.gametest.framework.TestReporter
import org.opentest4j.reporting.events.api.DocumentWriter
import org.opentest4j.reporting.events.api.NamespaceRegistry
import org.opentest4j.reporting.events.core.CoreFactory
import org.opentest4j.reporting.events.java.JavaFactory
import org.opentest4j.reporting.events.root.Events
import org.opentest4j.reporting.events.root.Finished
import org.opentest4j.reporting.events.root.RootFactory
import org.opentest4j.reporting.schema.Namespace
import java.io.File
import java.time.Instant
import org.opentest4j.reporting.events.core.Result as OtrResult

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
 * A test reporter that writes [Open Test Reporting](https://github.com/ota4j-team/open-test-reporting/) test reports.
 *
 * @param name The name of this test suite.
 * @param destination The destination file to write to.
 */
class OtrTestReporter(name: String, destination: File) : TestReporter {
    private val writer: DocumentWriter<Events>
    private var hasErrors = false

    init {
        val namespaceRegistry = NamespaceRegistry.builder(Namespace.REPORTING_CORE)
            .add("e", Namespace.REPORTING_EVENTS)
            .add("java", Namespace.REPORTING_JAVA)
            .build()

        writer = Events.createDocumentWriter(namespaceRegistry, destination.toPath())

        writer.append(RootFactory.started(SUITE_ID, Instant.now(), name))
    }

    private fun report(testInfo: GameTestInfo, configure: (Finished) -> Unit) {
        val finished = Instant.now()
        val started = finished.minusMillis(testInfo.runTime)
        val name = testInfo.id().toShortString()
        writer.append(RootFactory.started(name, started, name)) {
            it.withParentId(SUITE_ID)
        }
        writer.append(RootFactory.finished(name, finished), configure)
    }

    override fun onTestFailed(testInfo: GameTestInfo) {
        report(testInfo) { report ->
            if (testInfo.isRequired) hasErrors = true

            val status = if (testInfo.isRequired) OtrResult.Status.FAILED else OtrResult.Status.ABORTED
            report.append(CoreFactory.result(status)) { result ->
                val error = testInfo.error
                if (error != null) {
                    result.append(CoreFactory.reason(error.message ?: "unknown"))
                    result.append(JavaFactory.throwable(error))
                }
            }
        }
    }

    override fun onTestSuccess(testInfo: GameTestInfo) {
        report(testInfo) { it.append(CoreFactory.result(OtrResult.Status.SUCCESSFUL)) }
    }

    override fun finish() {
        writer.append(RootFactory.finished(SUITE_ID, Instant.now())) {
            it.append(CoreFactory.result(if (hasErrors) OtrResult.Status.FAILED else OtrResult.Status.SUCCESSFUL))
        }
        writer.close()
    }

    companion object {
        private const val SUITE_ID = "_gametests_"
    }
}
