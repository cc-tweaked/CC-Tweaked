// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

import cc.tweaked.gradle.*
import net.neoforged.gradle.dsl.common.extensions.RunnableSourceSet
import net.neoforged.gradle.dsl.common.runs.run.Run

plugins {
    id("cc-tweaked.forge")
    id("cc-tweaked.gametest")
    id("cc-tweaked.mod-publishing")
}

val modVersion: String by extra

val allProjects = listOf(":core-api", ":core", ":forge-api").map { evaluationDependsOn(it) }
cct {
    inlineProject(":common")
    allProjects.forEach { externalSources(it) }
}

sourceSets {
    main {
        resources.srcDir("src/generated/resources")
    }

    testMod { runs { modIdentifier = "cctest" } }
    testFixtures { runs { modIdentifier = "cctest" } }
}

minecraft {
    accessTransformers {
        file("src/main/resources/META-INF/accesstransformer.cfg")
    }
}

runs {
    configureEach {
        systemProperty("forge.logging.markers", "REGISTRIES")
        systemProperty("forge.logging.console.level", "debug")

        cct.sourceDirectories.get().forEach {
            if (it.classes) {
                if (it.sourceSet.extensions.findByType<RunnableSourceSet>() == null) {
                    it.sourceSet.extensions.create<RunnableSourceSet>(RunnableSourceSet.NAME, project)
                }

                modSource(it.sourceSet)
            }
        }

        dependencies {
            runtime(configurations["minecraftLibrary"])
        }
    }

    val client by registering {
        workingDirectory(file("run"))
    }

    val server by registering {
        workingDirectory(file("run/server"))
        programArgument("--nogui")
    }

    val data by registering {
        workingDirectory(file("run"))
        programArguments.addAll(
            "--mod", "computercraft", "--all",
            "--output", file("src/generated/resources/").absolutePath,
            "--existing", project(":common").file("src/main/resources/").absolutePath,
            "--existing", file("src/main/resources/").absolutePath,
        )
    }

    fun Run.configureForGameTest() {
        gameTest()

        systemProperty("cctest.sources", project(":common").file("src/testMod/resources/data/cctest").absolutePath)

        modSource(sourceSets.testMod.get())
        modSource(sourceSets.testFixtures.get())

        jvmArgument("-ea")

        dependencies {
            runtime(configurations["testMinecraftLibrary"])
        }
    }

    val gameTestServer by registering {
        workingDirectory(file("run/testServer"))
        configureForGameTest()
    }

    val gameTestClient by registering {
        configure(runTypes.client)

        workingDirectory(file("run/testClient"))
        configureForGameTest()

        systemProperties("cctest.tags", "client,common")
    }
}

configurations {
    val minecraftLibrary by registering {
        isCanBeResolved = true
        isCanBeConsumed = false
    }
    runtimeOnly { extendsFrom(minecraftLibrary.get()) }

    val testMinecraftLibrary by registering {
        isCanBeResolved = true
        isCanBeConsumed = false
    }
}

dependencies {
    compileOnly(libs.jetbrainsAnnotations)
    annotationProcessorEverywhere(libs.autoService)

    clientCompileOnly(variantOf(libs.emi) { classifier("api") })
    compileOnly(libs.bundles.externalMods.forge.compile)
    runtimeOnly(libs.bundles.externalMods.forge.runtime) { cct.exclude(this) }

    // Depend on our other projects.
    api(commonClasses(project(":forge-api"))) { cct.exclude(this) }
    clientApi(clientClasses(project(":forge-api"))) { cct.exclude(this) }
    implementation(project(":core")) { cct.exclude(this) }

    "minecraftLibrary"(libs.cobalt) {
        val version = libs.versions.cobalt.get()
        jarJar.ranged(this, "[$version,${getNextVersion(version)})")
    }
    "minecraftLibrary"(libs.jzlib) {
        jarJar.ranged(this, "[${libs.versions.jzlib.get()},)")
    }
    "minecraftLibrary"(libs.netty.http) {
        jarJar.ranged(this, "[${libs.versions.netty.get()},)")
        isTransitive = false
    }
    "minecraftLibrary"(libs.netty.socks) {
        jarJar.ranged(this, "[${libs.versions.netty.get()},)")
        isTransitive = false
    }
    "minecraftLibrary"(libs.netty.proxy) {
        jarJar.ranged(this, "[${libs.versions.netty.get()},)")
        isTransitive = false
    }

    testFixturesApi(libs.bundles.test)
    testFixturesApi(libs.bundles.kotlin)

    testImplementation(testFixtures(project(":core")))
    testImplementation(libs.bundles.test)
    testRuntimeOnly(libs.bundles.testRuntime)

    testModImplementation(testFixtures(project(":core")))
    testModImplementation(testFixtures(project(":forge")))

    // Ensure our test fixture dependencies are on the classpath
    "testMinecraftLibrary"(libs.bundles.kotlin)
    "testMinecraftLibrary"(libs.bundles.test)

    testFixturesImplementation(testFixtures(project(":core")))
}

// Compile tasks

tasks.processResources {
    inputs.property("modVersion", modVersion)
    inputs.property("neoVersion", libs.versions.neoForge.get())

    filesMatching("META-INF/mods.toml") {
        expand(mapOf("neoVersion" to libs.versions.neoForge.get(), "file" to mapOf("jarVersion" to modVersion)))
    }
}

tasks.jar {
    archiveClassifier.set("slim")

    for (source in cct.sourceDirectories.get()) {
        if (source.classes && source.external) from(source.sourceSet.output)
    }
}

tasks.sourcesJar {
    for (source in cct.sourceDirectories.get()) from(source.sourceSet.allSource)
}

tasks.jarJar {
    archiveClassifier.set("")
    configuration(project.configurations["minecraftLibrary"])

    for (source in cct.sourceDirectories.get()) {
        if (source.classes) from(source.sourceSet.output)
    }
}

tasks.assemble { dependsOn("jarJar") }

// Check tasks

tasks.test {
    systemProperty("cct.test-files", layout.buildDirectory.dir("tmp/testFiles").getAbsolutePath())
}

tasks.checkDependencyConsistency {
    // Forge pulls in slf4j 2.0.9 instead of 2.0.7, so we need to override that.
    override(libs.slf4j.asProvider(), "2.0.9")
}

val runGametest by tasks.registering(JavaExec::class) {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Runs tests on a temporary Minecraft instance."
    dependsOn("cleanRunGametest")
    usesService(MinecraftRunnerService.get(gradle))

    setRunConfig(runs["gameTestServer"])

    systemProperty("forge.logging.console.level", "info")
    systemProperty("cctest.gametest-report", layout.buildDirectory.dir("test-results/$name.xml").getAbsolutePath())
}
cct.jacoco(runGametest)
tasks.check { dependsOn(runGametest) }

/*val runGametestClient by tasks.registering(ClientJavaExec::class) {
    description = "Runs client-side gametests with no mods"
    setRunConfig(runs["testClient"])
    tags("client")
}
cct.jacoco(runGametestClient)

tasks.register("checkClient") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Runs all client-only checks."
    dependsOn(runGametestClient)
}*/

// Upload tasks

modPublishing {
    output.set(tasks.jarJar)
}

// Don't publish the slim jar
for (cfg in listOf(configurations.apiElements, configurations.runtimeElements)) {
    cfg.configure { artifacts.removeIf { it.classifier == "slim" } }
}

tasks.withType(GenerateModuleMetadata::class).configureEach { isEnabled = false }
publishing {
    publications {
        named("maven", MavenPublication::class) {
            jarJar.component(this)

            mavenDependencies {
                cct.configureExcludes(this)
            }
        }
    }
}
