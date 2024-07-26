// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

import cc.tweaked.gradle.*
import net.fabricmc.loom.configuration.ide.RunConfigSettings
import java.util.*

plugins {
    id("cc-tweaked.fabric")
    id("cc-tweaked.gametest")
    id("cc-tweaked.mod-publishing")
}

val modVersion: String by extra

val allProjects = listOf(":core-api", ":core", ":fabric-api").map { evaluationDependsOn(it) }
cct {
    inlineProject(":common")
    allProjects.forEach { externalSources(it) }
}

fun addRemappedConfiguration(name: String) {
    // There was a regression in Loom 1.1 which means that targetConfigurationName doesn't do anything, and remap
    // configurations just get added to the main source set (https://github.com/FabricMC/fabric-loom/issues/843).
    // To get around that, we create our own source set and register a remap configuration with that. This does
    // introduce a bit of noise, but it's not the end of the world.
    val ourSourceSet = sourceSets.register(name) {
        // Try to make this source set as much of a non-entity as possible.
        listOf(allSource, java, resources, kotlin).forEach { it.setSrcDirs(emptyList<File>()) }
    }
    val capitalName = name.replaceFirstChar { it.titlecase(Locale.ROOT) }
    loom.addRemapConfiguration("mod$capitalName") {
        onCompileClasspath.set(false)
        onRuntimeClasspath.set(true)
        sourceSet.set(ourSourceSet)
        targetConfigurationName.set(name)
    }
    configurations.create(name) {
        isCanBeConsumed = false
        isCanBeResolved = true
        extendsFrom(configurations["${name}RuntimeClasspath"])
    }
}

addRemappedConfiguration("testWithSodium")
addRemappedConfiguration("testWithIris")

configurations {
    // Declare some configurations which are both included (jar-in-jar-ed) and a normal dependency (so they appear in
    // our POM).
    val includeRuntimeOnly by registering {
        isCanBeConsumed = false
        isCanBeResolved = false
    }
    val includeImplementation by registering {
        isCanBeConsumed = false
        isCanBeResolved = false
    }

    include { extendsFrom(includeRuntimeOnly.get(), includeImplementation.get()) }
    runtimeOnly { extendsFrom(includeRuntimeOnly.get()) }
    implementation { extendsFrom(includeImplementation.get()) }
}

dependencies {
    clientCompileOnly(variantOf(libs.emi) { classifier("api") })
    modCompileOnly(libs.bundles.externalMods.fabric.compile) {
        exclude("net.fabricmc", "fabric-loader")
        exclude("net.fabricmc.fabric-api")
    }
    modCompileOnly(libs.create.fabric) { isTransitive = false }

    modClientRuntimeOnly(libs.bundles.externalMods.fabric.runtime) {
        exclude("net.fabricmc", "fabric-loader")
        exclude("net.fabricmc.fabric-api")
    }

    "modTestWithSodium"(libs.sodium)
    "modTestWithIris"(libs.iris)
    "modTestWithIris"(libs.sodium)

    "includeRuntimeOnly"(libs.cobalt)
    "includeRuntimeOnly"(libs.jzlib)
    "includeRuntimeOnly"(libs.netty.http)
    "includeRuntimeOnly"(libs.netty.socks)
    "includeRuntimeOnly"(libs.netty.proxy)

    "includeImplementation"(libs.nightConfig.core)
    "includeImplementation"(libs.nightConfig.toml)

    // Pull in our other projects. See comments in MinecraftConfigurations on this nastiness.
    api(commonClasses(project(":fabric-api"))) { cct.exclude(this) }
    clientApi(clientClasses(project(":fabric-api"))) { cct.exclude(this) }
    implementation(project(":core")) { cct.exclude(this) }

    annotationProcessorEverywhere(libs.autoService)

    testModImplementation(testFixtures(project(":core")))
    testModImplementation(testFixtures(project(":fabric")))

    testImplementation(libs.bundles.test)
    testRuntimeOnly(libs.bundles.testRuntime)
    testRuntimeOnly(libs.fabric.junit)

    testFixturesImplementation(testFixtures(project(":core")))
}

sourceSets.main { resources.srcDir("src/generated/resources") }

loom {
    accessWidenerPath.set(project(":common").file("src/main/resources/computercraft.accesswidener"))
    mixin.defaultRefmapName.set("computercraft.refmap.json")

    mods {
        register("computercraft") {
            // Configure sources when running via the IDE. Note these don't add build dependencies (hence why it's safe
            // to use common), only change how the launch.cfg file is generated.
            cct.sourceDirectories.get().forEach { sourceSet(it.sourceSet) }

            // Running via Gradle
            dependency(dependencies.project(":core").apply { isTransitive = false })
        }

        register("cctest") {
            sourceSet(sourceSets.testMod.get())
            sourceSet(project(":common").sourceSets.testMod.get())
        }
    }

    runs {
        configureEach {
            ideConfigGenerated(true)
        }

        named("client") {
            configName = "Client"
        }

        named("server") {
            configName = "Server"
            runDir("run/server")
        }

        register("data") {
            configName = "Datagen"
            client()

            runDir("run/dataGen")
            property("fabric-api.datagen")
            property("fabric-api.datagen.output-dir", layout.buildDirectory.dir("generatedResources").getAbsolutePath())
            property("fabric-api.datagen.strict-validation")
        }

        fun configureForGameTest(config: RunConfigSettings) = config.run {
            source(sourceSets.testMod.get())

            val testSources = project(":common").file("src/testMod/resources/data/cctest").absolutePath
            property("cctest.sources", testSources)

            // Load cctest last, so it can override resources. This bypasses Fabric's shuffling of mods
            property("fabric.debug.loadLate", "cctest")

            vmArg("-ea")
        }

        val testClient by registering {
            configName = "Test Client"
            client()
            configureForGameTest(this)

            runDir("run/testClient")
            property("cctest.tags", "client,common")
        }

        register("gametest") {
            configName = "Game Test"
            server()
            configureForGameTest(this)

            property("fabric-api.gametest")
            property(
                "fabric-api.gametest.report-file",
                layout.buildDirectory.dir("test-results/runGametest.xml")
                    .getAbsolutePath(),
            )
            runDir("run/gametest")
        }
    }
}

tasks.processResources {
    inputs.property("version", modVersion)

    filesMatching("fabric.mod.json") {
        expand(mapOf("version" to modVersion))
    }
}

tasks.jar {
    for (source in cct.sourceDirectories.get()) {
        if (source.classes && source.external) from(source.sourceSet.output)
    }
}

tasks.sourcesJar {
    for (source in cct.sourceDirectories.get()) from(source.sourceSet.allSource)
}

val validateMixinNames by tasks.registering(net.fabricmc.loom.task.ValidateMixinNameTask::class) {
    source(sourceSets.main.get().output)
    source(sourceSets.client.get().output)
    source(sourceSets.testMod.get().output)
}
tasks.check { dependsOn(validateMixinNames) }

tasks.test { dependsOn(tasks.generateDLIConfig) }

val runGametest = tasks.named<JavaExec>("runGametest") {
    usesService(MinecraftRunnerService.get(gradle))
}
cct.jacoco(runGametest)
tasks.check { dependsOn(runGametest) }

val runGametestClient by tasks.registering(ClientJavaExec::class) {
    description = "Runs client-side gametests with no mods"
    copyFrom("runTestClient")

    tags("client")
}
cct.jacoco(runGametestClient)

val runGametestClientWithSodium by tasks.registering(ClientJavaExec::class) {
    description = "Runs client-side gametests with Sodium"
    copyFrom("runTestClient")

    tags("sodium")
    classpath += configurations["testWithSodium"]
}
cct.jacoco(runGametestClientWithSodium)

val runGametestClientWithIris by tasks.registering(ClientJavaExec::class) {
    description = "Runs client-side gametests with Iris"
    copyFrom("runTestClient")

    tags("iris")
    classpath += configurations["testWithIris"]

    withFileFrom(workingDir.resolve("shaderpacks/ComplementaryShaders_v4.6.zip")) {
        cct.downloadFile("Complementary Shaders", "https://edge.forgecdn.net/files/3951/170/ComplementaryShaders_v4.6.zip")
    }
    withFileContents(workingDir.resolve("config/iris.properties")) {
        """
        enableShaders=true
        shaderPack=ComplementaryShaders_v4.6.zip
        """.trimIndent()
    }
}
cct.jacoco(runGametestClientWithIris)

tasks.register("checkClient") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Runs all client-only checks."
    dependsOn(runGametestClient, runGametestClientWithSodium, runGametestClientWithIris)
}

modPublishing {
    output.set(tasks.remapJar)
}

tasks.withType(GenerateModuleMetadata::class).configureEach { isEnabled = false }
publishing {
    publications {
        named("maven", MavenPublication::class) {
            mavenDependencies {
                cct.configureExcludes(this)
            }
        }
    }
}

modrinth {
    required.project("fabric-api")
}
