import cc.tweaked.gradle.CCTweakedPlugin
import cc.tweaked.gradle.LicenseHeader
import com.diffplug.gradle.spotless.FormatExtension
import com.diffplug.spotless.LineEnding
import java.nio.charset.StandardCharsets

plugins {
    `java-library`
    jacoco
    checkstyle
    id("com.diffplug.spotless")
}

val modVersion: String by extra
val mcVersion: String by extra

group = "cc.tweaked"
version = modVersion

base.archivesName.convention(
    // TODO: Remove this (and the one below) once we've no longer got a root project!
    if (project.path == rootProject.path) "cc-tweaked-$mcVersion" else "cc-tweaked-$mcVersion-${project.name}",
)

java {
    toolchain {
        languageVersion.set(CCTweakedPlugin.JAVA_VERSION)
    }

    withSourcesJar()
}

repositories {
    mavenCentral()
    maven("https://squiddev.cc/maven") {
        name = "SquidDev"
        content {
            includeGroup("org.squiddev")
            includeGroup("cc.tweaked")
            // Things we mirror
            includeGroup("com.blamejared.crafttweaker")
            includeGroup("commoble.morered")
            includeGroup("maven.modrinth")
            includeGroup("mezz.jei")
        }
    }
}

dependencies {
    val libs = project.extensions.getByType<VersionCatalogsExtension>().named("libs")
    checkstyle(libs.findLibrary("checkstyle").get())
}

// Configure default JavaCompile tasks with our arguments.
sourceSets.all {
    tasks.named(compileJavaTaskName, JavaCompile::class.java) {
        // Processing just gives us "No processor claimed any of these annotations", so skip that!
        options.compilerArgs.addAll(listOf("-Xlint", "-Xlint:-processing"))
    }
}

tasks.withType(JavaCompile::class.java).configureEach {
    options.encoding = "UTF-8"
}

tasks.jar {
    isReproducibleFileOrder = true
    isPreserveFileTimestamps = false
    archiveClassifier.set("slim")

    manifest {
        attributes(
            "Specification-Title" to "computercraft",
            "Specification-Vendor" to "SquidDev",
            "Specification-Version" to "1",
            "Implementation-Title" to (if (project.path == rootProject.path) "cctweaked" else "cctweaked-${project.name}"),
            "Implementation-Version" to modVersion,
            "Implementation-Vendor" to "SquidDev",
        )
    }
}

tasks.javadoc {
    options {
        val stdOptions = this as StandardJavadocDocletOptions
        stdOptions.addBooleanOption("Xdoclint:all,-missing", true)
        stdOptions.links("https://docs.oracle.com/en/java/javase/17/docs/api/")
    }
}

tasks.test {
    finalizedBy("jacocoTestReport")

    useJUnitPlatform()
    testLogging {
        events("skipped", "failed")
    }
}

tasks.withType(JacocoReport::class.java).configureEach {
    reports.xml.required.set(true)
    reports.html.required.set(true)
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
        api = rootProject.file("config/license/api.txt"),
        main = rootProject.file("config/license/main.txt"),
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
