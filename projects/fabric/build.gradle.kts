// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

import cc.tweaked.gradle.*
import cc.tweaked.vanillaextract.configurations.Capabilities.clientClasses
import cc.tweaked.vanillaextract.configurations.Capabilities.commonClasses
import net.fabricmc.loom.configuration.ide.RunConfigSettings
import net.ltgt.gradle.errorprone.errorprone
import java.util.*

plugins {
    id("cc-tweaked.fabric")
    id("cc-tweaked.mod")
    id("cc-tweaked.published-mod")
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
        onCompileClasspath = false
        onRuntimeClasspath = true
        sourceSet = ourSourceSet
        targetConfigurationName = name
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
    val includeRuntimeOnly = register("includeRuntimeOnly") {
        isCanBeConsumed = false
        isCanBeResolved = false
    }
    val includeImplementation = register("includeImplementation") {
        isCanBeConsumed = false
        isCanBeResolved = false
    }

    include { extendsFrom(includeRuntimeOnly.get(), includeImplementation.get()) }
    runtimeOnly { extendsFrom(includeRuntimeOnly.get()) }
    implementation { extendsFrom(includeImplementation.get()) }

    // Declare a configuration for projects which are on the compile and runtime classpath, but not treated as
    // dependencies. This is used for our local projects.
    val localImplementation = register("localImplementation") {
        isCanBeResolved = false
        isCanBeConsumed = false
    }
    compileClasspath { extendsFrom(localImplementation.get()) }
    runtimeClasspath { extendsFrom(localImplementation.get()) }
}

dependencies {
    clientCompileOnly(variantOf(libs.emi) { classifier("api") })
    modCompileOnly(libs.bundles.externalMods.fabric.compile) {
        exclude("net.fabricmc", "fabric-loader")
        exclude("net.fabricmc.fabric-api")
    }

    modClientRuntimeOnly(libs.bundles.externalMods.fabric.runtime) {
        exclude("net.fabricmc", "fabric-loader")
        exclude("net.fabricmc.fabric-api")
    }

    "modTestWithSodium"(libs.sodium.fabric)
    "modTestWithIris"(libs.iris.fabric)
    "modTestWithIris"(libs.sodium.fabric)

    "includeRuntimeOnly"(libs.cobalt)
    "includeRuntimeOnly"(libs.jzlib)
    "includeRuntimeOnly"(libs.netty.http)
    "includeRuntimeOnly"(libs.netty.socks)
    "includeRuntimeOnly"(libs.netty.proxy)

    "includeImplementation"(libs.nightConfig.core)
    "includeImplementation"(libs.nightConfig.toml)

    // Pull in our other projects. See comments in MinecraftConfigurations on this nastiness.
    "localImplementation"(commonClasses(project(":common")))
    clientImplementation(clientClasses(project(":common")))
    "localImplementation"(commonClasses(project(":fabric-api")))
    clientImplementation(clientClasses(project(":fabric-api")))

    annotationProcessorEverywhere(libs.autoService)

    datagenImplementation(project(":common")) { capabilities { requireFeature("datagen") } }
    examplesImplementation(project(":common")) { capabilities { requireFeature("examples") } }

    testModImplementation(project(":common")) { capabilities { requireFeature("test-mod") } }
    testModImplementation(testFixtures(project(":common")))

    testImplementation(testFixtures(project(":common")))
    testRuntimeOnly(libs.bundles.testRuntime)
    testRuntimeOnly(libs.fabric.junit)

    embeddedProject(project(":core"))
    embeddedProject(project(":core-api"))
    minecraftEmbeddedProject(project(":common"))
    minecraftEmbeddedProject(project(":common-api"))
    minecraftEmbeddedProject(project(":fabric-api"))
}

loom {
    accessWidenerPath = file("../common/src/main/resources/computercraft.accesswidener")

    mods {
        register("computercraft") {
            // Configure sources when running via the IDE. Note these don't add build dependencies (hence why it's safe
            // to use common), only change how the launch.cfg file is generated.
            sourceSet("main", ":core-api")
            sourceSet("main", ":core")
            for (proj in listOf(":common-api", ":common", ":fabric-api", ":fabric")) {
                sourceSet("main", proj)
                sourceSet("client", proj)
            }

            // Running via Gradle
            dependency(dependencies.project(":core").apply { isTransitive = false })
        }

        register("cctest") {
            sourceSet(sourceSets.testMod.get())
            sourceSet("testMod", ":common")

            // Loom's sourceSet doesn't add jars to the list, so also include the :common test mod jar.
            val dep = dependencies.project(":common")
            dep.capabilities { requireFeature("test-mod") }
            dep.isTransitive = false
            modFiles.from(configurations.detachedConfiguration(dep))
        }

        register("examplemod") {
            sourceSet(sourceSets.examples.get())
        }
    }

    runs {
        configureEach {
            generateRunConfig = true
            preferGradleTask = false
            ideConfigFolder = "Fabric"

            systemProperties.put("fabric-tag-conventions-v2.missingTagTranslationWarning", "VERBOSE")
        }

        named("client") {
            displayName = "Client"
        }

        named("server") {
            displayName = "Server"
            runDirectory = layout.projectDirectory.dir("run/server")
        }

        fun registerForData(name: String, display: String, source: SourceSet, modSource: NamedDomainObjectProvider<SourceSet>) {
            val outputName = source.getTaskName("generate", "resources")
            val output = layout.buildDirectory.dir(outputName)
            register(name) {
                displayName = display

                client()
                runDirectory = layout.buildDirectory.dir("run${name.capitalise()}")
                systemProperties.put("fabric-api.datagen", "true")
                systemProperties.put("fabric-api.datagen.output-dir", output.getAbsolutePath())
                systemProperties.put("fabric-api.datagen.strict-validation", "true")

                sourceSet = modSource.name
            }

            configurations.consumable("${outputName}Elements") {
                outgoing.artifact(output) { builtBy(tasks.named("run${name.capitalise()}")) }
            }
        }

        registerForData("data", "Datagen", sourceSets.main.get(), sourceSets.datagen)

        fun RunConfigSettings.configureForGameTest() {
            sourceSet = sourceSets.testMod.name

            val testSources = file("../common/src/testMod/resources/data/cctest").absolutePath
            systemProperties.put("cctest.sources", testSources)

            // Load cctest last, so it can override resources. This bypasses Fabric's shuffling of mods
            systemProperties.put("fabric.debug.loadLate", "cctest")

            jvmArguments.add("-ea")
        }

        val testClient = register("testClient") {
            displayName = "Test Client"
            client()
            configureForGameTest()

            runDirectory = layout.projectDirectory.dir("run/testClient")
            systemProperties.put("cctest.tags", "client,common")
        }

        register("gametest") {
            displayName = "Game Test"
            server()
            configureForGameTest()

            systemProperties.put("fabric-api.gametest", "true")
            systemProperties.put(
                "cctest.gametest-report",
                layout.buildDirectory.dir("test-results/runGametest.xml").getAbsolutePath(),
            )
            runDirectory = layout.projectDirectory.dir("run/gametest")
        }

        register("exampleClient") {
            client()
            displayName = "Example Mod Client"
            sourceSet = sourceSets.examples.name
        }

        registerForData("exampleData", "Example Mod Datagen", sourceSets.examples.get(), sourceSets.examples)
    }
}

tasks.processResources {
    val modVersion = project.version as String

    inputs.property("modVersion", modVersion)

    var props = mapOf("version" to modVersion)

    filesMatching("fabric.mod.json") { expand(props) }
}

val validateMixinNames = tasks.register<net.fabricmc.loom.task.ValidateMixinNameTask>("validateMixinNames") {
    source(sourceSets.main.get().output)
    source(sourceSets.client.get().output)
    source(sourceSets.testMod.get().output)
}
tasks.check { dependsOn(validateMixinNames) }

tasks.test { dependsOn(tasks.generateDLIConfig) }

tasks.verifyClassesMatch {
    options.errorprone {
        option("ModLoader", "fabric")
    }

    // Fabric widens the inner classes of RenderType, so we end up with different InnerClasses attributes. Doesn't cause
    // issues.
    ignoredFiles.add("dan200/computercraft/client/render/RenderTypes\$Types.class")
}

val runGametest = tasks.named<JavaExec>("runGametest") {
    usesService(MinecraftRunnerService.get(gradle))
}
cct.jacoco(runGametest)
tasks.check { dependsOn(runGametest) }

val runGametestClient = tasks.register<ClientJavaExec>("runGametestClient") {
    description = "Runs client-side gametests with no mods"
    copyFrom("runTestClient")

    tags("client")
}
cct.jacoco(runGametestClient)

val runGametestClientWithSodium = tasks.register<ClientJavaExec>("runGametestClientWithSodium") {
    description = "Runs client-side gametests with Sodium"
    copyFrom("runTestClient")

    tags("sodium")
    classpath += configurations["testWithSodium"]
}
cct.jacoco(runGametestClientWithSodium)

val runGametestClientWithIris = tasks.register<ClientJavaExec>("runGametestClientWithIris") {
    description = "Runs client-side gametests with Iris"
    copyFrom("runTestClient")

    tags("iris")
    classpath += configurations["testWithIris"]

    withComplementaryShaders()
}
cct.jacoco(runGametestClientWithIris)

tasks.register("checkClient") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Runs all client-only checks."
    dependsOn(runGametestClient, runGametestClientWithSodium, runGametestClientWithIris)
}

modPublishing {
    output = tasks.remapJar
}

modrinth {
    required.project("fabric-api")
}
