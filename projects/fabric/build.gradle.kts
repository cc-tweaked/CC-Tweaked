// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

import cc.tweaked.gradle.*
import net.fabricmc.loom.configuration.ide.RunConfigSettings

plugins {
    id("cc-tweaked.fabric")
    id("cc-tweaked.mod")
    id("cc-tweaked.mod-publishing")
}

val modVersion: String by extra

val allProjects = listOf(":core-api", ":core", ":fabric-api").map { evaluationDependsOn(it) }
cct {
    inlineProject(":common")
    allProjects.forEach { externalSources(it) }
}

sourceSets {
    main { java.exclude("dan200/computercraft/shared/integration/rei") }
    client { java.exclude("dan200/computercraft/client/integration/rei") }
}

fun addRemappedConfiguration(name: String) {
    configurations.create(name) {
        isCanBeConsumed = false
        isCanBeResolved = true
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

    // Declare a configuration for projects which are on the compile and runtime classpath, but not treated as
    // dependencies. This is used for our local projects.
    val localImplementation by registering {
        isCanBeResolved = false
        isCanBeConsumed = false
    }
    compileClasspath { extendsFrom(localImplementation.get()) }
    runtimeClasspath { extendsFrom(localImplementation.get()) }
}

dependencies {
    compileOnly(libs.bundles.externalMods.fabric.compile) {
        exclude("net.fabricmc", "fabric-loader")
        exclude("net.fabricmc.fabric-api")
    }
    // FIXME: A lie, but Fabric Create uses the wrong mappings
    compileOnly(libs.create.forge) { isTransitive = false }

    clientRuntimeOnly(libs.bundles.externalMods.fabric.runtime) {
        exclude("net.fabricmc", "fabric-loader")
        exclude("net.fabricmc.fabric-api")
    }

    "testWithSodium"(libs.sodium.fabric)
    "testWithIris"(libs.iris.fabric)
    "testWithIris"(libs.sodium.fabric)

    "includeRuntimeOnly"(libs.cobalt)
    "includeRuntimeOnly"(libs.netty.socks)
    "includeRuntimeOnly"(libs.netty.proxy)

    "includeImplementation"(libs.nightConfig.core)
    "includeImplementation"(libs.nightConfig.toml)

    // Pull in our other projects. See comments in MinecraftConfigurations on this nastiness.
    "localImplementation"(project(":core"))
    "localImplementation"(commonClasses(project(":fabric-api")))
    clientImplementation(clientClasses(project(":fabric-api")))

    annotationProcessorEverywhere(libs.autoService)

    testModImplementation(testFixtures(project(":core")))
    testModImplementation(testFixtures(project(":fabric")))

    testImplementation(libs.bundles.test)
    testRuntimeOnly(libs.bundles.testRuntime)
    testRuntimeOnly(libs.fabric.junit)

    testFixturesImplementation(testFixtures(project(":core")))
}

loom {
    accessWidenerPath = project(":common").file("src/main/resources/computercraft.accesswidener")

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

        register("examplemod") {
            sourceSet(sourceSets.examples.get())
        }
    }

    runs {
        configureEach {
            ideConfigGenerated(true)
            ideConfigFolder = "Fabric"

            property("fabric-tag-conventions-v2.missingTagTranslationWarning", "VERBOSE")
        }

        named("client") {
            configName = "Client"
        }

        named("server") {
            configName = "Server"
            runDir("run/server")
        }

        fun RunConfigSettings.configureForData(sourceSet: SourceSet) {
            client()
            runDir("run/run${name.capitalise()}")
            property("fabric-api.datagen")
            property(
                "fabric-api.datagen.output-dir",
                layout.buildDirectory.dir(sourceSet.getTaskName("generateResources", null)).getAbsolutePath(),
            )
            property("fabric-api.datagen.strict-validation")
        }

        register("data") {
            configName = "Datagen"
            configureForData(sourceSets.main.get())
            source(sourceSets.datagen.get())
        }

        fun RunConfigSettings.configureForGameTest() {
            source(sourceSets.testMod.get())

            val testSources = project(":common").file("src/testMod/resources").absolutePath
            property("cctest.sources", testSources)

            // Load cctest last, so it can override resources. This bypasses Fabric's shuffling of mods
            property("fabric.debug.loadLate", "cctest")

            vmArg("-ea")
        }

        val testClient by registering {
            configName = "Test Client"
            client()
            configureForGameTest()

            runDir("run/testClient")
            property("cctest.tags", "client,common")
        }

        register("gametest") {
            configName = "Game Test"
            server()
            configureForGameTest()

            property("fabric-api.gametest")
            property(
                "fabric-api.gametest.report-file",
                layout.buildDirectory.dir("test-results/runGametest.xml").getAbsolutePath(),
            )
            runDir("run/gametest")
        }

        register("exampleClient") {
            client()
            configName = "Example Mod Client"
            source(sourceSets.examples.get())
        }

        register("exampleData") {
            configName = "Example Mod Datagen"
            configureForData(sourceSets.examples.get())
            source(sourceSets.examples.get())
        }
    }
}

tasks.processResources {
    inputs.property("modVersion", modVersion)

    var props = mapOf("version" to modVersion)

    filesMatching("fabric.mod.json") { expand(props) }
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

    withComplementaryShaders()
}
cct.jacoco(runGametestClientWithIris)

tasks.register("checkClient") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Runs all client-only checks."
    dependsOn(runGametestClient, runGametestClientWithSodium, runGametestClientWithIris)
}

modPublishing {
    output = tasks.jar
}

modrinth {
    required.project("fabric-api")
}
