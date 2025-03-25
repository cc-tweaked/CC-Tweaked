// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package cc.tweaked.gradle

import org.gradle.api.Project
import org.gradle.api.logging.Logging
import org.w3c.dom.Attr
import org.w3c.dom.Document
import org.w3c.dom.Node
import org.xml.sax.InputSource
import java.nio.file.Files
import java.nio.file.Path
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory

/**
 * Patches up run configurations from ForgeGradle and Loom.
 *
 * Would be good to PR some (or all) of these changes upstream at some point.
 *
 * @see net.fabricmc.loom.configuration.ide.idea.IdeaSyncTask
 */
internal class IdeaRunConfigurations(project: Project) {
    private val rootProject = project.rootProject

    private val documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
    private val xpath = XPathFactory.newInstance().newXPath()
    private val writer = TransformerFactory.newInstance().newTransformer()

    private val ideaDir = rootProject.file(".idea/")

    fun patch() = synchronized(LOCK) {
        val runConfigDir = ideaDir.resolve("runConfigurations")
        if (!runConfigDir.isDirectory) return

        Files.list(runConfigDir.toPath()).use {
            for (configuration in it) {
                val filename = configuration.fileName.toString()
                when {
                    filename.endsWith("_fabric.xml") -> patchFabric(configuration)
                    else -> {}
                }
            }
        }
    }

    private fun patchFabric(path: Path) = withXml(path) {
        setXml("//configuration", "folderName") { "Fabric" }
    }

    private fun LocatedDocument.setXml(xpath: String, attribute: String, value: (String?) -> String) {
        val node = this@IdeaRunConfigurations.xpath.evaluate(xpath, document, XPathConstants.NODE) as Node?
        if (node == null) {
            LOGGER.error("[{}] Cannot find {}", path.fileName, xpath)
            return
        }

        val attr = node.attributes.getNamedItem(attribute) as Attr? ?: document.createAttribute(attribute)
        val oldValue = attr.value
        attr.value = value(attr.value)
        node.attributes.setNamedItem(attr)

        if (oldValue != attr.value) {
            LOGGER.info("[{}] Setting {}@{}:\n  Old: {}\n  New: {}", path.fileName, xpath, attribute, oldValue, attr.value)
        }
    }

    private fun withXml(path: Path, run: LocatedDocument.() -> Unit) {
        val doc = Files.newBufferedReader(path).use { documentBuilder.parse(InputSource(it)) }
        run(LocatedDocument(path, doc))
        Files.newBufferedWriter(path).use { writer.transform(DOMSource(doc), StreamResult(it)) }
    }

    private class LocatedDocument(val path: Path, val document: Document)

    companion object {
        private val LOGGER = Logging.getLogger(IdeaRunConfigurations::class.java)
        private val LOCK = Any()
    }
}
