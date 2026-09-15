// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

import cc.tweaked.gradle.CCTweakedJavaVersions
import cc.tweaked.gradle.IlluaminateExec
import cc.tweaked.gradle.MergeTrees
import cc.tweaked.gradle.annotationProcessorEverywhere
import cc.tweaked.vanillaextract.configurations.Capabilities.clientClasses
import cc.tweaked.vanillaextract.configurations.Capabilities.commonClasses
import net.fabricmc.loom.configuration.accesswidener.AccessWidenerJarProcessor

plugins {
    `java-test-fixtures`
    id("cc-tweaked.vanilla")
    id("cc-tweaked.illuaminate")
    id("cc-tweaked.mod")
    id("cc-tweaked.publishing")
}

loom {
    accessWidenerPath = project.file("src/main/resources/computercraft.accesswidener")
    // Loom doesn't support multiple access wideners. We inject an additional processor which applies our
    // Fabric/Forge-provided AWs.
    addMinecraftJarProcessor(
        AccessWidenerJarProcessor::class.java,
        "cct:extra-aw",
        false,
        project.objects.fileProperty().also {
            it.set(project.file("src/main/resources/computercraft-common.accesswidener"))
            it.finalizeValue()
        },
    )
}

configurations {
    register("cctJavadoc")
}

sourceSets.testFixtures {
    compileClasspath += sourceSets.main.get().compileClasspath + sourceSets.client.get().compileClasspath
}

repositories {
    exclusiveContent {
        forRepositories(maven("https://maven.neoforged.net/releases"))
        filter {
            includeModule("org.spongepowered", "mixin")
        }
    }
}

dependencies {
    // Pull in our other projects. See comments in MinecraftConfigurations on this nastiness.
    api(project(":core"))
    api(commonClasses(project(":common-api")))
    clientApi(clientClasses(project(":common-api")))

    compileOnly(libs.mixin)
    compileOnly(libs.mixinExtra)
    compileOnly(libs.bundles.externalMods.common)

    annotationProcessorEverywhere(libs.autoService)
    testFixturesAnnotationProcessor(libs.autoService)

    testFixturesApi(testFixtures(project(":core")))

    testImplementation(testFixtures(project(":core")))
    testImplementation(testFixtures(project()))
    testImplementation(libs.bundles.test)
    testRuntimeOnly(libs.bundles.testRuntime)

    testImplementation(libs.jmh)
    testAnnotationProcessor(libs.jmh.processor)

    testModCompileOnly(libs.mixin)
    testModImplementation(testFixtures(project(":core")))
    testModImplementation(testFixtures(project(":common")))
    testModImplementation(libs.bundles.kotlin)

    "cctJavadoc"(libs.cctJavadoc)
}

illuaminate {
    version = libs.versions.illuaminate
}

val rootProjectDir = rootProject.isolated.projectDirectory
val luaJavadoc = tasks.register<Javadoc>("luaJavadoc") {
    description = "Generates documentation for Java-side Lua functions."
    group = JavaBasePlugin.DOCUMENTATION_GROUP

    source(sourceSets.main.get().java.sourceDirectories)
    source("../core/src/main/java")
    classpath = sourceSets.main.get().compileClasspath + sourceSets.main.get().runtimeClasspath

    destinationDir = layout.buildDirectory.dir("docs/luaJavadoc").get().asFile

    val options = options as StandardJavadocDocletOptions
    options.docletpath = configurations["cctJavadoc"].files.toList()
    options.doclet = "cc.tweaked.javadoc.LuaDoclet"
    options.addStringOption("project-root", rootProjectDir.asFile.absolutePath)
    options.noTimestamp(false)

    javadocTool = javaToolchains.javadocToolFor { languageVersion = CCTweakedJavaVersions.JDK_VERSION }
}

configurations.consumable("luaJavadocElements") { outgoing.artifact(luaJavadoc) }

val lintLua = tasks.register<IlluaminateExec>("lintLua") {
    group = JavaBasePlugin.VERIFICATION_GROUP
    description = "Lint Lua (and Lua docs) with illuaminate"

    // Config files
    inputs.file(rootProjectDir.file("illuaminate.sexp")).withPropertyName("illuaminate.sexp")
    // Sources
    inputs.dir(rootProjectDir.dir("doc")).withPropertyName("docs")
    inputs.dir("../core/src/main/resources/data/computercraft/lua").withPropertyName("lua rom")
    inputs.files(luaJavadoc)

    args = listOf("lint")
    workingDir = rootProjectDir.asFile

    doFirst { if (System.getenv("GITHUB_ACTIONS") != null) println("::add-matcher::.github/matchers/illuaminate.json") }
    doLast { if (System.getenv("GITHUB_ACTIONS") != null) println("::remove-matcher owner=illuaminate::") }
}

fun MergeTrees.configureForDatagen(source: SourceSet, outputFolder: String) {
    output = layout.projectDirectory.dir(outputFolder)

    val taskName = source.getTaskName("generate", "resources")
    for (loader in listOf(":forge", ":fabric")) {
        source {
            val resources = configurations.detachedConfiguration(dependencies.project(loader, "${taskName}Elements"))
            input.from(resources.asFileTree.matching { exclude(".cache") })
            output = project(loader).isolated.projectDirectory.dir(outputFolder)
        }
    }
}

val runData = tasks.register<MergeTrees>("runData") {
    configureForDatagen(sourceSets.main.get(), "src/generated/resources")
}

val runExampleData = tasks.register<MergeTrees>("runExampleData") {
    configureForDatagen(sourceSets.examples.get(), "src/examples/generatedResources")
}

// We can't create accurate module metadata for our additional capabilities, so disable it.
project.tasks.withType(GenerateModuleMetadata::class.java).configureEach {
    isEnabled = false
}
