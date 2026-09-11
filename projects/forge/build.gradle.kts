// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

import cc.tweaked.gradle.*
import cc.tweaked.vanillaextract.configurations.Capabilities.clientClasses
import cc.tweaked.vanillaextract.configurations.Capabilities.commonClasses
import net.ltgt.gradle.errorprone.errorprone
import net.neoforged.moddevgradle.dsl.ModModel
import net.neoforged.moddevgradle.dsl.RunModel

plugins {
    id("cc-tweaked.forge")
    id("cc-tweaked.mod")
    id("cc-tweaked.published-mod")
}

// This is needed for configuring source sets in our Forge runs.
for (project in listOf(":core-api", ":core", ":forge-api", ":common")) {
    evaluationDependsOn(project)
}

neoForge {
    fun ModModel.sourceSet(name: String, project: String) {
        modSourceSets.add(provider { project(project) }.flatMap { it.sourceSets.named(name) })
    }

    fun ModModel.addComputerCraft() {
        sourceSet("main", ":core-api")
        sourceSet("main", ":core")
        for (proj in listOf(":common-api", ":common", ":forge-api", ":forge")) {
            sourceSet("main", proj)
            sourceSet("client", proj)
        }
    }

    val computercraft = mods.register("computercraft") {
        addComputerCraft()
    }

    val computercraftDatagen = mods.register("computercraftDatagen") {
        addComputerCraft()
        sourceSet(sourceSets.datagen.get())
        sourceSet("datagen", ":common")
    }

    val testMod = mods.register("testMod") {
        sourceSet(sourceSets.testMod.get())
        sourceSet("testFixtures", ":common")
        sourceSet("testFixtures", ":core")
        sourceSet("testMod", ":common")
    }

    val exampleMod = mods.register("exampleMod") {
        sourceSet(sourceSets.examples.get())
        sourceSet("examples", ":common")
    }

    val project = project // Silly shadowing to prevent "runs.project" referencing the deprecated member.
    runs {
        configureEach {
            ideName = "${name.capitalise()} (Forge)"
            systemProperty("forge.logging.markers", "REGISTRIES")
            systemProperty("forge.logging.console.level", "debug")
            loadedMods.add(computercraft)

            ideFolderName = "Forge"
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
                "--existing", file("../common/src/${sourceSet.name}/resources/").absolutePath,
                "--existing", file("src/${sourceSet.name}/resources/").absolutePath,
            )
        }

        register("data") {
            configureForData("computercraft", sourceSets.main.get())
            loadedMods = listOf(computercraftDatagen.get())
        }

        fun RunModel.configureForGameTest() {
            systemProperty(
                "cctest.sources",
                file("../common/src/testMod/resources/data/cctest").absolutePath,
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
    // Force a more recent version of ASM, so we're compatible with Java 25.
    configureEach { resolutionStrategy.force(libs.asm) }

    additionalRuntimeClasspath { extendsFrom(jarJar.get()) }

    val testAdditionalRuntimeClasspath = register("testAdditionalRuntimeClasspath") {
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
    val localImplementation = register("localImplementation") {
        isCanBeResolved = false
        isCanBeConsumed = false
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
    compileOnly(libs.create.forge) { isTransitive = false }

    // Depend on our other projects.
    "localImplementation"(commonClasses(project(":common")))
    clientImplementation(clientClasses(project(":common")))
    "localImplementation"(commonClasses(project(":forge-api")))
    clientImplementation(clientClasses(project(":forge-api")))

    jarJar(libs.cobalt)
    jarJar(libs.jzlib)
    // We don't jar-in-jar our additional netty dependencies (see the tasks.jarJar configuration), but still want them
    // on the legacy classpath.
    additionalRuntimeClasspath(libs.netty.http) { isTransitive = false }
    additionalRuntimeClasspath(libs.netty.socks) { isTransitive = false }
    additionalRuntimeClasspath(libs.netty.proxy) { isTransitive = false }

    datagenImplementation(project(":common")) { capabilities { requireFeature("datagen") } }
    examplesImplementation(project(":common")) { capabilities { requireFeature("examples") } }

    testModImplementation(testFixtures(project(":common")))
    testModImplementation(project(":common")) { capabilities { requireFeature("test-mod") } }

    // Ensure our test fixture dependencies are on the classpath
    "testAdditionalRuntimeClasspath"(libs.bundles.kotlin)
    "testAdditionalRuntimeClasspath"(libs.bundles.test)

    "testWithIris"(libs.iris.forge)
    "testWithIris"(libs.sodium.forge)
    testImplementation(testFixtures(project(":common")))
    testRuntimeOnly(libs.bundles.testRuntime)

    embeddedProject(project(":core-api"))
    minecraftEmbeddedProject(project(":common"))
    minecraftEmbeddedProject(project(":common-api"))
    minecraftEmbeddedProject(project(":forge-api"))
}

// Compile tasks

tasks.processResources {
    val modVersion = project.version as String

    inputs.property("modVersion", modVersion)
    inputs.property("neoVersion", libs.versions.neoForge)

    var props = mapOf(
        "neoVersion" to libs.versions.neoForge.get(),
        "file" to mapOf("jarVersion" to modVersion),
    )

    filesMatching("META-INF/neoforge.mods.toml") { expand(props) }
}

tasks.jar {
    // Include core separately, along with the relocated netty classes.
    val dep = dependencies.project(mapOf("path" to ":core", "configuration" to "shadowRuntimeElements"))
    val conf = configurations.detachedConfiguration(dep)
    from(zipTree(conf.elements.map { it.single() })) {
        exclude("META-INF/**")
    }
}

tasks.sourcesJar {
    val conf = configurations.detachedConfiguration(dependencies.project(":core"))
    from(ExternalProjectArtifacts.sources(conf))
}

// Check tasks

tasks.test {
    systemProperty("cct.test-files", layout.buildDirectory.dir("tmp/testFiles").getAbsolutePath())
}

tasks.verifyClassesMatch {
    options.errorprone {
        option("ModLoader", "forge")
    }

    // Forge widens the inner classes of RenderType, so we end up with different InnerClasses attributes. Doesn't cause
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
    copyFromForge("runTestClient")
    tags("client")
}
cct.jacoco(runGametestClient)

val runGametestClientWithIris = tasks.register<ClientJavaExec>("runGametestClientWithIris") {
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
