import cc.tweaked.gradle.LicenseHeader
import com.diffplug.gradle.spotless.FormatExtension
import com.diffplug.spotless.LineEnding
import java.nio.charset.StandardCharsets

plugins {
    java
    jacoco
    id("com.diffplug.spotless")
}

spotless {
    encoding = StandardCharsets.UTF_8
    lineEndings = LineEnding.UNIX

    fun FormatExtension.defaults() {
        endWithNewline()
        trimTrailingWhitespace()
        indentWithSpaces(4)
    }

    val licenser = LicenseHeader.create(
        api = file("config/license/api.txt"),
        main = file("config/license/main.txt"),
    )

    java {
        defaults()
        addStep(licenser)
        removeUnusedImports()
    }

    val ktlintConfig = mapOf(
        "disabled_rules" to "no-wildcard-imports",
        "ij_kotlin_allow_trailing_comma" to "true",
        "ij_kotlin_allow_trailing_comma_on_call_site" to "true",
    )

    kotlinGradle {
        defaults()
        ktlint().editorConfigOverride(ktlintConfig)
    }

    kotlin {
        defaults()
        ktlint().editorConfigOverride(ktlintConfig)
    }
}
