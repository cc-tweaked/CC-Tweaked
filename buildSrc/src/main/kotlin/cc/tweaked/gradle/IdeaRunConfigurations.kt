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
 * @see net.minecraftforge.gradle.common.util.runs.IntellijRunGenerator
 */
internal class IdeaRunConfigurations(project: Project) {
    private val rootProject = project.rootProject

    private val documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
    private val xpath = XPathFactory.newInstance().newXPath()
    private val writer = TransformerFactory.newInstance().newTransformer()

    private val ideaDir = rootProject.file(".idea/")
    private val buildDir: Lazy<String?> = lazy {
        val ideaMisc = ideaDir.resolve("misc.xml")

        try {
            val doc = Files.newBufferedReader(ideaMisc.toPath()).use {
                documentBuilder.parse(InputSource(it))
            }
            val node =
                xpath.evaluate("//component[@name=\"ProjectRootManager\"]/output", doc, XPathConstants.NODE) as Node
            val attr = node.attributes.getNamedItem("url") as Attr
            attr.value.removePrefix("file://")
        } catch (e: Exception) {
            LOGGER.error("Failed to find root directory", e)
            null
        }
    }

    fun patch() = synchronized(LOCK) {
        val runConfigDir = ideaDir.resolve("runConfigurations")
        if (!runConfigDir.isDirectory) return

        Files.list(runConfigDir.toPath()).use {
            for (configuration in it) {
                val filename = configuration.fileName.toString();
                when {
                    filename.endsWith("_fabric.xml") -> patchFabric(configuration)
                    filename.startsWith("forge_") && filename.endsWith(".xml") -> patchForge(configuration)
                    else -> {}
                }
            }
        }
    }

    private fun patchFabric(path: Path) = withXml(path) {
        setXml("//configuration", "folderName") { "Fabric" }
    }

    private fun patchForge(path: Path) = withXml(path) {
        val configId = path.fileName.toString().removePrefix("forge_").removeSuffix(".xml")
        val sourceSet = forgeConfigs[configId]
        if (sourceSet == null) {
            LOGGER.error("[{}] Cannot map run configuration to a known source set", path)
            return@withXml
        }

        setXml("//configuration", "folderName") { "Forge" }
        setXml("//configuration/module", "name") { "${rootProject.name}.forge.$sourceSet" }

        if (buildDir.value == null) return@withXml
        setXml("//configuration/envs/env[@name=\"MOD_CLASSES\"]", "value") { classpath ->
            val classes = classpath!!.split(':')
            val newClasses = mutableListOf<String>()
            fun appendUnique(x: String) {
                if (!newClasses.contains(x)) newClasses.add(x)
            }

            for (entry in classes) {
                if (!entry.contains("/out/")) {
                    appendUnique(entry)
                    continue
                }

                val match = CLASSPATH_ENTRY.matchEntire(entry)
                if (match != null) {
                    val modId = match.groups["modId"]!!.value
                    val proj = match.groups["proj"]!!.value
                    var component = match.groups["component"]!!.value
                    if (component == "production") component = "main"

                    appendUnique(forgeModEntry(modId, proj, component))
                } else {
                    LOGGER.warn("[{}] Unknown classpath entry {}", path, entry)
                    appendUnique(entry)
                }
            }

            // Ensure common code is on the classpath
            for (proj in listOf("common", "common-api")) {
                for (component in listOf("main", "client")) {
                    appendUnique(forgeModEntry("computercraft", proj, component))
                }
            }

            if (newClasses.any { it.startsWith("cctest%%") }) {
                appendUnique(forgeModEntry("cctest", "core", "testFixtures"))
                appendUnique(forgeModEntry("cctest", "common", "testFixtures"))
                appendUnique(forgeModEntry("cctest", "common", "testMod"))
            }

            newClasses.joinToString(":")
        }
    }

    private fun forgeModEntry(mod: String, project: String, component: String) =
        "$mod%%${buildDir.value}/production/${rootProject.name}.$project.$component"

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

        private val CLASSPATH_ENTRY =
            Regex("(?<modId>[a-z]+)%%\\\$PROJECT_DIR\\\$/projects/(?<proj>[a-z-]+)/out/(?<component>\\w+)/(?<type>[a-z]+)\$")

        private val forgeConfigs = mapOf(
            "runClient" to "client",
            "runData" to "main",
            "runGameTestServer" to "testMod",
            "runServer" to "main",
            "runTestClient" to "testMod",
        )
    }
}
