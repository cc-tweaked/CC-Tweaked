import cc.tweaked.gradle.annotationProcessorEverywhere
import cc.tweaked.gradle.clientClasses
import cc.tweaked.gradle.commonClasses
import net.fabricmc.loom.configuration.ide.RunConfigSettings

plugins {
    id("cc-tweaked.fabric")
    id("cc-tweaked.gametest")
}

val modVersion: String by extra
val mcVersion: String by extra

val allProjects = listOf(":core-api", ":core", ":fabric-api").map { evaluationDependsOn(it) }
cct {
    inlineProject(":common")
    allProjects.forEach { externalSources(it) }
}

dependencies {
    modImplementation(libs.bundles.externalMods.fabric)
    modCompileOnly(libs.bundles.externalMods.fabric.compile) {
        exclude("net.fabricmc", "fabric-loader")
        exclude("net.fabricmc.fabric-api")
    }
    modClientRuntimeOnly(libs.bundles.externalMods.fabric.runtime) {
        exclude("net.fabricmc", "fabric-loader")
        exclude("net.fabricmc.fabric-api")
    }

    include(libs.cobalt)
    include(libs.netty.http) // It might be better to shadowJar this, as we don't use half of it.
    include(libs.forgeConfig)
    include(libs.nightConfig.core)
    include(libs.nightConfig.toml)

    // Pull in our other projects. See comments in MinecraftConfigurations on this nastiness.
    implementation(project(":core"))
    compileOnly(project(":forge-stubs"))
    implementation(commonClasses(project(":fabric-api")))
    clientImplementation(clientClasses(project(":fabric-api")))

    annotationProcessorEverywhere(libs.autoService)

    testModImplementation(testFixtures(project(":core")))
    testModImplementation(testFixtures(project(":fabric")))

    testImplementation(libs.byteBuddyAgent)
    testImplementation(libs.bundles.test)
    testRuntimeOnly(libs.bundles.testRuntime)
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
            server()

            runDir("run/dataGen")
            property("cct.pretty-json")
            property("fabric-api.datagen")
            property("fabric-api.datagen.output-dir", file("src/generated/resources").absolutePath)
            property("fabric-api.datagen.strict-validation")
        }

        fun configureForGameTest(config: RunConfigSettings) = config.run {
            source(sourceSets.testMod.get())

            val testSources = project(":common").file("src/testMod/resources/data/cctest").absolutePath
            property("cctest.sources", testSources)

            // Load cctest last, so it can override resources. This bypasses Fabric's shuffling of mods
            property("fabric.debug.loadLate", "cctest")
        }

        val testClient by registering {
            configName = "Test Client"
            client()
            configureForGameTest(this)

            runDir("run/testClient")
        }

        register("gametest") {
            configName = "Game Test"
            server()
            configureForGameTest(this)

            property("fabric-api.gametest")
            property("fabric-api.gametest.report-file", project.buildDir.resolve("test-results/gametest/gametest.xml").absolutePath)
            runDir("run/gametest")
        }
    }
}

tasks.processResources {
    inputs.property("version", modVersion)

    filesMatching("fabric.mod.json") {
        expand(mapOf("version" to modVersion))
    }
    exclude(".cache")
}

tasks.jar {
    from(allProjects.map { zipTree(it.tasks.jar.get().archiveFile) })
}

val validateMixinNames by tasks.registering(net.fabricmc.loom.task.ValidateMixinNameTask::class) {
    source(sourceSets.main.get().output)
    source(sourceSets.client.get().output)
    source(sourceSets.testMod.get().output)
}

tasks.test { dependsOn(tasks.generateDLIConfig) }

val runGametest = tasks.named<JavaExec>("runGametest")

cct.jacoco(runGametest)

tasks.check { dependsOn(validateMixinNames, runGametest) }
