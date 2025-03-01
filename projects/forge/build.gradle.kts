// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

import cc.tweaked.gradle.*
import net.neoforged.moddevgradle.dsl.RunModel

plugins {
    id("cc-tweaked.forge")
    id("cc-tweaked.mod")
    id("cc-tweaked.mod-publishing")
}

val modVersion: String by extra

val allProjects = listOf(":core-api", ":core", ":forge-api").map { evaluationDependsOn(it) }
cct {
    inlineProject(":common")
    allProjects.forEach { externalSources(it) }
}

neoForge {
    val computercraft by mods.registering {
        cct.sourceDirectories.get().forEach {
            if (it.classes) sourceSet(it.sourceSet)
        }
    }

    val computercraftDatagen by mods.registering {
        cct.sourceDirectories.get().forEach {
            if (it.classes) sourceSet(it.sourceSet)
        }
        sourceSet(sourceSets.datagen.get())
    }

    val testMod by mods.registering {
        sourceSet(sourceSets.testMod.get())
        sourceSet(sourceSets.testFixtures.get())
        sourceSet(project(":core").sourceSets["testFixtures"])
    }

    val exampleMod by mods.registering {
        sourceSet(sourceSets.examples.get())
    }

    runs {
        configureEach {
            ideName = "Forge - ${name.capitalise()}"
            systemProperty("forge.logging.markers", "REGISTRIES")
            systemProperty("forge.logging.console.level", "debug")
            loadedMods.add(computercraft)
        }

        register("client") {
            client()
        }

        register("server") {
            server()
            gameDirectory = file("run/server")
            programArgument("--nogui")
        }

        fun RunModel.configureForData(mod: String, sourceSet: SourceSet) {
            data()
            gameDirectory = file("run/run${name.capitalise()}")
            programArguments.addAll(
                "--mod", mod, "--all",
                "--output",
                layout.buildDirectory.dir(sourceSet.getTaskName("generateResources", null))
                    .getAbsolutePath(),
                "--existing", project.project(":common").file("src/${sourceSet.name}/resources/").absolutePath,
                "--existing", project.file("src/${sourceSet.name}/resources/").absolutePath,
            )
        }

        register("data") {
            configureForData("computercraft", sourceSets.main.get())
            loadedMods = listOf(computercraftDatagen.get())
        }

        fun RunModel.configureForGameTest() {
            systemProperty(
                "cctest.sources",
                project.project(":common").file("src/testMod/resources/data/cctest").absolutePath,
            )

            programArgument("--mixin.config=computercraft-gametest.mixins.json")
            loadedMods.add(testMod)

            jvmArgument("-ea")
        }

        register("testClient") {
            client()
            gameDirectory = file("run/testClient")
            configureForGameTest()

            systemProperty("cctest.tags", "client,common")
        }

        register("gametest") {
            type = "gameTestServer"
            configureForGameTest()

            systemProperty("forge.logging.console.level", "info")
            systemProperty(
                "cctest.gametest-report",
                layout.buildDirectory.dir("test-results/runGametest.xml").getAbsolutePath(),
            )
            gameDirectory = file("run/gametest")
        }

        register("exampleClient") {
            client()
            loadedMods.add(exampleMod.get())
        }

        register("exampleData") {
            configureForData("examplemod", sourceSets.examples.get())
            loadedMods.add(exampleMod.get())
        }
    }
}

configurations {
    additionalRuntimeClasspath { extendsFrom(jarJar.get()) }

    val testAdditionalRuntimeClasspath by registering {
        isCanBeResolved = true
        isCanBeConsumed = false
        // Prevent ending up with multiple versions of libraries on the classpath.
        shouldResolveConsistentlyWith(additionalRuntimeClasspath.get())
    }

    for (testConfig in listOf("testClientAdditionalRuntimeClasspath", "gametestAdditionalRuntimeClasspath")) {
        named(testConfig) { extendsFrom(testAdditionalRuntimeClasspath.get()) }
    }

    register("testWithIris") {
        isCanBeConsumed = false
        isCanBeResolved = true
    }

    // Declare a configuration for projects which are on the compile and runtime classpath, but not treated as
    // dependencies. This is used for our local projects.
    val localImplementation by registering {
        isCanBeResolved = false
        isCanBeConsumed = false
        isVisible = false
    }
    compileClasspath { extendsFrom(localImplementation.get()) }
    runtimeClasspath { extendsFrom(localImplementation.get()) }
}

dependencies {
    compileOnly(libs.jetbrainsAnnotations)
    annotationProcessorEverywhere(libs.autoService)

    clientCompileOnly(variantOf(libs.emi) { classifier("api") })
    compileOnly(libs.bundles.externalMods.forge.compile)
    clientRuntimeOnly(libs.bundles.externalMods.forge.runtime)
    compileOnly(variantOf(libs.create.forge) { classifier("slim") }) { isTransitive = false }

    // Depend on our other projects.
    "localImplementation"(project(":core"))
    "localImplementation"(commonClasses(project(":forge-api")))
    clientImplementation(clientClasses(project(":forge-api")))

    jarJar(libs.cobalt)
    jarJar(libs.jzlib)
    // We don't jar-in-jar our additional netty dependencies (see the tasks.jarJar configuration), but still want them
    // on the legacy classpath.
    additionalRuntimeClasspath(libs.netty.http) { isTransitive = false }
    additionalRuntimeClasspath(libs.netty.socks) { isTransitive = false }
    additionalRuntimeClasspath(libs.netty.proxy) { isTransitive = false }

    testFixturesApi(libs.bundles.test)
    testFixturesApi(libs.bundles.kotlin)

    testImplementation(testFixtures(project(":core")))
    testImplementation(libs.bundles.test)
    testRuntimeOnly(libs.bundles.testRuntime)

    testModImplementation(testFixtures(project(":core")))
    testModImplementation(testFixtures(project(":forge")))

    // Ensure our test fixture dependencies are on the classpath
    "testAdditionalRuntimeClasspath"(libs.bundles.kotlin)
    "testAdditionalRuntimeClasspath"(libs.bundles.test)

    testFixturesImplementation(testFixtures(project(":core")))

    "testWithIris"(libs.iris.forge)
    "testWithIris"(libs.sodium.forge)
}

// Compile tasks

tasks.processResources {
    inputs.property("modVersion", modVersion)
    inputs.property("neoVersion", libs.versions.neoForge)

    var props = mapOf(
        "neoVersion" to libs.versions.neoForge.get(),
        "file" to mapOf("jarVersion" to modVersion),
    )

    filesMatching("META-INF/neoforge.mods.toml") { expand(props) }
}

tasks.jar {
    // Include all classes from other projects except core.
    val coreSources = project(":core").sourceSets["main"]
    for (source in cct.sourceDirectories.get()) {
        if (source.classes && source.sourceSet != coreSources) from(source.sourceSet.output)
    }

    // Include core separately, along with the relocated netty classes.
    from(zipTree(project(":core").tasks.named("shadowJar", AbstractArchiveTask::class).map { it.archiveFile })) {
        exclude("META-INF/**")
    }
}

tasks.sourcesJar {
    for (source in cct.sourceDirectories.get()) from(source.sourceSet.allSource)
}

// Check tasks

tasks.test {
    systemProperty("cct.test-files", layout.buildDirectory.dir("tmp/testFiles").getAbsolutePath())
}

val runGametest = tasks.named<JavaExec>("runGametest") {
    usesService(MinecraftRunnerService.get(gradle))
}
cct.jacoco(runGametest)
tasks.check { dependsOn(runGametest) }

val runGametestClient by tasks.registering(ClientJavaExec::class) {
    description = "Runs client-side gametests with no mods"
    copyFromForge("runTestClient")
    tags("client")
}
cct.jacoco(runGametestClient)

val runGametestClientWithIris by tasks.registering(ClientJavaExec::class) {
    description = "Runs client-side gametests with Iris"
    copyFromForge("runGameTestClient")

    tags("iris")
    classpath += configurations["testWithIris"]

    withComplementaryShaders()
}
cct.jacoco(runGametestClientWithIris)

tasks.register("checkClient") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Runs all client-only checks."
    dependsOn(runGametestClient, runGametestClientWithIris)
}

modPublishing {
    output = tasks.jar
}

// TODO: Remove once https://github.com/modrinth/minotaur/pull/72 is merged.
modrinth { loaders = listOf("neoforge") }
