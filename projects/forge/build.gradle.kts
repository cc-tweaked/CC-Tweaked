// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

import cc.tweaked.gradle.*
import net.minecraftforge.gradle.common.util.RunConfig

plugins {
    id("cc-tweaked.forge")
    id("cc-tweaked.gametest")
    alias(libs.plugins.mixinGradle)
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
                "--output", file("src/generated/resources/"),
                "--existing", project(":common").file("src/main/resources/"),
                "--existing", file("src/main/resources/"),
            )
            property("cct.pretty-json", "true")
        }

        fun RunConfig.configureForGameTest() {
            val old = lazyTokens["minecraft_classpath"]
            lazyToken("minecraft_classpath") {
                // We do some terrible hacks here to basically find all things not already on the runtime classpath
                // and add them. /Except/ for our source sets, as those need to load inside the Minecraft classpath.
                val testMod = configurations["testModRuntimeClasspath"].resolve()
                val implementation = configurations.runtimeClasspath.get().resolve()
                val new = (testMod - implementation)
                    .asSequence()
                    .filter { it.isFile && !it.name.endsWith("-test-fixtures.jar") }
                    .map { it.absolutePath }
                    .joinToString(File.pathSeparator)

                val oldVal = old?.get()
                if (oldVal.isNullOrEmpty()) new else oldVal + File.pathSeparator + new
            }

            property("cctest.sources", project(":common").file("src/testMod/resources/data/cctest").absolutePath)

            arg("--mixin.config=computercraft-gametest.mixins.json")

            mods.register("cctest") {
                source(sourceSets["testMod"])
                source(sourceSets["testFixtures"])
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

mixin {
    add(sourceSets.main.get(), "computercraft.refmap.json")
    add(sourceSets.client.get(), "client-computercraft.refmap.json")

    config("computercraft.mixins.json")
    config("computercraft-client.mixins.json")
    config("computercraft-client.forge.mixins.json")
}

configurations {
    minecraftLibrary { extendsFrom(minecraftEmbed.get()) }
}

dependencies {
    annotationProcessor("org.spongepowered:mixin:0.8.5-SQUID:processor")
    clientAnnotationProcessor("org.spongepowered:mixin:0.8.5-SQUID:processor")

    compileOnly(libs.jetbrainsAnnotations)
    annotationProcessorEverywhere(libs.autoService)

    clientCompileOnly(variantOf(libs.emi) { classifier("api") })
    libs.bundles.externalMods.forge.compile.get().map { compileOnly(fg.deobf(it)) }
    libs.bundles.externalMods.forge.runtime.get().map { runtimeOnly(fg.deobf(it)) }

    // Depend on our other projects.
    api(commonClasses(project(":forge-api")))
    api(clientClasses(project(":forge-api")))
    implementation(project(":core"))

    minecraftEmbed(libs.cobalt) {
        jarJar.ranged(this, "[${libs.versions.cobalt.asProvider().get()},${libs.versions.cobalt.next.get()})")
    }
    minecraftEmbed(libs.jzlib) {
        jarJar.ranged(this, "[${libs.versions.jzlib.get()},)")
    }
    minecraftEmbed(libs.netty.http) {
        jarJar.ranged(this, "[${libs.versions.netty.get()},)")
        isTransitive = false
    }
    minecraftEmbed(libs.netty.socks) {
        jarJar.ranged(this, "[${libs.versions.netty.get()},)")
        isTransitive = false
    }
    minecraftEmbed(libs.netty.proxy) {
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

    for (source in cct.sourceDirectories.get()) {
        if (source.classes) from(source.sourceSet.output)
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
                exclude(dependencies.create("cc.tweaked:"))
                exclude(libs.jei.forge.get())
            }
        }
    }
}
