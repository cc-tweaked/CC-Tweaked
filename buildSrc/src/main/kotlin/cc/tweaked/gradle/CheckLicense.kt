package cc.tweaked.gradle

import com.diffplug.spotless.FormatterFunc
import com.diffplug.spotless.FormatterStep
import com.diffplug.spotless.generic.LicenseHeaderStep
import java.io.File
import java.io.Serializable
import java.nio.charset.StandardCharsets

/**
 * Similar to [LicenseHeaderStep], but supports multiple licenses.
 */
object LicenseHeader {
    /**
     * The current year to use in templates. Intentionally not dynamic to avoid failing the build.
     */
    private const val YEAR = 2022

    private val COMMENT = Regex("""^/\*(.*?)\*/\n?""", RegexOption.DOT_MATCHES_ALL)

    fun create(api: File, main: File): FormatterStep = FormatterStep.createLazy(
        "license",
        { Licenses(getTemplateText(api), getTemplateText(main)) },
        { state -> FormatterFunc.NeedsFile { contents, file -> formatFile(state, contents, file) } },
    )

    private fun getTemplateText(file: File): String =
        file.readText(StandardCharsets.UTF_8).trim().replace("\${year}", "$YEAR")

    private fun formatFile(licenses: Licenses, contents: String, file: File): String {
        val license = getLicense(contents)
        val expectedLicense = getExpectedLicense(licenses, file.parentFile)

        return when {
            license == null -> setLicense(expectedLicense, contents)
            license.second != expectedLicense -> setLicense(expectedLicense, contents, license.first)
            else -> contents
        }
    }

    private fun getExpectedLicense(licenses: Licenses, file: File): String {
        var file: File? = file
        while (file != null) {
            if (file.name == "api" && file.parentFile?.name == "computercraft") return licenses.api
            file = file.parentFile
        }
        return licenses.main
    }

    private fun getLicense(contents: String): Pair<Int, String>? {
        val match = COMMENT.find(contents) ?: return null
        val license = match.groups[1]!!.value
            .trim().lineSequence()
            .map { it.trimStart(' ', '*') }
            .joinToString("\n")
        return Pair(match.range.last + 1, license)
    }

    private fun setLicense(license: String, contents: String, start: Int = 0): String {
        val out = StringBuilder()
        out.append("/*\n")
        for (line in license.lineSequence()) out.append(" * ").append(line).append("\n")
        out.append(" */\n")
        out.append(contents, start, contents.length)
        return out.toString()
    }

    private data class Licenses(val api: String, val main: String) : Serializable {
        companion object {
            private const val serialVersionUID: Long = 7741106448372435662L
        }
    }
}
