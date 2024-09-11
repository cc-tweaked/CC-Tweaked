// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

import cc.tweaked.gradle.*
import net.minecraftforge.gradle.common.util.RunConfig

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
}

minecraft {
    runs {
        // configureEach would be better, but we need to eagerly configure configs or otherwise the run task doesn't
        // get set up properly.
        all {
            property("forge.logging.markers", "REGISTRIES")
            property("forge.logging.console.level", "debug")

            mods.register("computercraft") {
                cct.sourceDirectories.get().forEach {
                    if (it.classes) sources(it.sourceSet)
                }
            }
        }

        val client by registering {
            workingDirectory(file("run"))
        }

        val server by registering {
            workingDirectory(file("run/server"))
            arg("--nogui")
        }

        val data by registering {
            workingDirectory(file("run"))
            args(
                "--mod", "computercraft", "--all",
                "--output", layout.buildDirectory.dir("generatedResources").getAbsolutePath(),
                "--existing", project(":common").file("src/main/resources/"),
                "--existing", file("src/main/resources/"),
            )
        }

        fun RunConfig.configureForGameTest() {
            val old = lazyTokens["minecraft_classpath"]
            lazyToken("minecraft_classpath") {
                // Add all files in testMinecraftLibrary to the classpath.
                val allFiles = mutableSetOf<String>()

                val oldVal = old?.get()
                if (!oldVal.isNullOrEmpty()) allFiles.addAll(oldVal.split(File.pathSeparatorChar))

                for (file in configurations["testMinecraftLibrary"].resolve()) allFiles.add(file.absolutePath)

                allFiles.joinToString(File.pathSeparator)
            }

            property("cctest.sources", project(":common").file("src/testMod/resources/data/cctest").absolutePath)

            arg("--mixin.config=computercraft-gametest.mixins.json")

            mods.register("cctest") {
                source(sourceSets["testMod"])
                source(sourceSets["testFixtures"])
                source(project(":core").sourceSets["testFixtures"])
            }
        }

        val testClient by registering {
            workingDirectory(file("run/testClient"))
            parent(client.get())
            configureForGameTest()

            property("cctest.tags", "client,common")
        }

        val gameTestServer by registering {
            workingDirectory(file("run/testServer"))
            configureForGameTest()

            property("forge.logging.console.level", "info")
            jvmArg("-ea")
        }
    }
}

configurations {
    minecraftLibrary { extendsFrom(minecraftEmbed.get()) }

    // Move minecraftLibrary/minecraftEmbed out of implementation, and into runtimeOnly.
    implementation { setExtendsFrom(extendsFrom - setOf(minecraftLibrary.get(), minecraftEmbed.get())) }
    runtimeOnly { extendsFrom(minecraftLibrary.get(), minecraftEmbed.get()) }

    val testMinecraftLibrary by registering {
        isCanBeResolved = true
        isCanBeConsumed = false
        // Prevent ending up with multiple versions of libraries on the classpath.
        shouldResolveConsistentlyWith(minecraftLibrary.get())
    }
}

dependencies {
    compileOnly(libs.jetbrainsAnnotations)
    annotationProcessorEverywhere(libs.autoService)

    clientCompileOnly(variantOf(libs.emi) { classifier("api") })
    libs.bundles.externalMods.forge.compile.get().map { compileOnly(fg.deobf(it)) }
    libs.bundles.externalMods.forge.runtime.get().map { runtimeOnly(fg.deobf(it)) }

    // fg.debof only accepts a closure to configure the dependency, so doesn't work with Kotlin. We create and configure
    // the dep first, and then pass it off to ForgeGradle.
    (create(variantOf(libs.create.forge) { classifier("slim") }.get()) as ExternalModuleDependency)
        .apply { isTransitive = false }.let { compileOnly(fg.deobf(it)) }

    // Depend on our other projects.
    api(commonClasses(project(":forge-api"))) { cct.exclude(this) }
    clientApi(clientClasses(project(":forge-api"))) { cct.exclude(this) }
    implementation(project(":core")) { cct.exclude(this) }

    minecraftEmbed(libs.cobalt) {
        val version = libs.versions.cobalt.get()
        jarJar.ranged(this, "[$version,${getNextVersion(version)})")
    }
    minecraftEmbed(libs.jzlib) {
        jarJar.ranged(this, "[${libs.versions.jzlib.get()},)")
    }
    // We don't jar-in-jar our additional netty dependencies (see the tasks.jarJar configuration), but still want them
    // on the legacy classpath.
    minecraftLibrary(libs.netty.http) { isTransitive = false }
    minecraftLibrary(libs.netty.socks) { isTransitive = false }
    minecraftLibrary(libs.netty.proxy) { isTransitive = false }

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
    inputs.property("forgeVersion", libs.versions.forge.get())

    filesMatching("META-INF/mods.toml") {
        expand(mapOf("forgeVersion" to libs.versions.forge.get(), "file" to mapOf("jarVersion" to modVersion)))
    }
}

tasks.jar {
    finalizedBy("reobfJar")
    archiveClassifier.set("slim")

    for (source in cct.sourceDirectories.get()) {
        if (source.classes && source.external) from(source.sourceSet.output)
    }
}

tasks.sourcesJar {
    for (source in cct.sourceDirectories.get()) from(source.sourceSet.allSource)
}

tasks.jarJar {
    finalizedBy("reobfJarJar")
    archiveClassifier.set("")
    duplicatesStrategy = DuplicatesStrategy.FAIL

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

tasks.assemble { dependsOn("jarJar") }

// Check tasks

tasks.test {
    systemProperty("cct.test-files", layout.buildDirectory.dir("tmp/testFiles").getAbsolutePath())
}

val runGametest by tasks.registering(JavaExec::class) {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Runs tests on a temporary Minecraft instance."
    dependsOn("cleanRunGametest")
    usesService(MinecraftRunnerService.get(gradle))

    setRunConfig(minecraft.runs["gameTestServer"])

    systemProperty("cctest.gametest-report", layout.buildDirectory.dir("test-results/$name.xml").getAbsolutePath())
}
cct.jacoco(runGametest)
tasks.check { dependsOn(runGametest) }

val runGametestClient by tasks.registering(ClientJavaExec::class) {
    description = "Runs client-side gametests with no mods"
    setRunConfig(minecraft.runs["testClient"])
    tags("client")
}
cct.jacoco(runGametestClient)

tasks.register("checkClient") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Runs all client-only checks."
    dependsOn(runGametestClient)
}

// Upload tasks

modPublishing {
    output.set(tasks.jarJar)
}

// Don't publish the slim jar
for (cfg in listOf(configurations.apiElements, configurations.runtimeElements)) {
    cfg.configure { artifacts.removeIf { it.classifier == "slim" } }
}

publishing {
    publications {
        named("maven", MavenPublication::class) {
            fg.component(this)
            // jarJar.component is broken (https://github.com/MinecraftForge/ForgeGradle/issues/914), so declare the
            // artifact explicitly.
            artifact(tasks.jarJar)

            mavenDependencies {
                cct.configureExcludes(this)
                exclude(libs.jei.forge.get())
            }
        }
    }
}
