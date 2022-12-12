import cc.tweaked.gradle.*
import net.darkhax.curseforgegradle.TaskPublishCurseForge
import net.minecraftforge.gradle.common.util.RunConfig

plugins {
    // Build
    id("cc-tweaked.forge")
    id("cc-tweaked.gametest")
    alias(libs.plugins.mixinGradle)
    alias(libs.plugins.shadow)
    // Publishing
    alias(libs.plugins.curseForgeGradle)
    alias(libs.plugins.minotaur)

    id("cc-tweaked.illuaminate")
    id("cc-tweaked.publishing")
    id("cc-tweaked")
}

val isUnstable = project.properties["isUnstable"] == "true"
val modVersion: String by extra
val mcVersion: String by extra

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

            forceExit = false

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

reobf {
    register("shadowJar")
}

configurations {
    register("cctJavadoc")
}

dependencies {
    annotationProcessor("org.spongepowered:mixin:0.8.5-SQUID:processor")
    clientAnnotationProcessor("org.spongepowered:mixin:0.8.5-SQUID:processor")

    compileOnly(libs.jetbrainsAnnotations)
    annotationProcessorEverywhere(libs.autoService)

    libs.bundles.externalMods.forge.compile.get().map { compileOnly(fg.deobf(it)) }
    libs.bundles.externalMods.forge.runtime.get().map { runtimeOnly(fg.deobf(it)) }

    // Depend on our other projects. By using the api configuration, shadow jar will correctly
    // preserve all files from forge-api/core-api.
    api(commonClasses(project(":forge-api")))
    api(clientClasses(project(":forge-api")))
    implementation(project(":core"))

    minecraftLibrary(libs.cobalt)
    minecraftLibrary(libs.netty.http) { isTransitive = false }

    testFixturesApi(libs.bundles.test)
    testFixturesApi(libs.bundles.kotlin)

    testImplementation(testFixtures(project(":core")))
    testImplementation(libs.bundles.test)
    testImplementation(libs.bundles.kotlin)
    testRuntimeOnly(libs.bundles.testRuntime)

    testModImplementation(testFixtures(project(":core")))
    testModImplementation(testFixtures(project(":forge")))

    "cctJavadoc"(libs.cctJavadoc)
}

illuaminate {
    version.set(libs.versions.illuaminate)
}

// Compile tasks

val luaJavadoc by tasks.registering(Javadoc::class) {
    description = "Generates documentation for Java-side Lua functions."
    group = JavaBasePlugin.DOCUMENTATION_GROUP

    source(sourceSets.main.get().java)
    source(project(":core").sourceSets.main.get().java)
    source(project(":common").sourceSets.main.get().java)

    setDestinationDir(buildDir.resolve("docs/luaJavadoc"))
    classpath = sourceSets.main.get().compileClasspath

    val options = options as StandardJavadocDocletOptions
    options.docletpath = configurations["cctJavadoc"].files.toList()
    options.doclet = "cc.tweaked.javadoc.LuaDoclet"
    options.addStringOption("project-root", rootProject.file(".").absolutePath)
    options.noTimestamp(false)

    javadocTool.set(
        javaToolchains.javadocToolFor {
            languageVersion.set(cc.tweaked.gradle.CCTweakedPlugin.JAVA_VERSION)
        },
    )
}

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

tasks.shadowJar {
    finalizedBy("reobfShadowJar")
    archiveClassifier.set("")

    from(sourceSets.client.get().output)

    dependencies {
        include(dependency("cc.tweaked:"))
        include(dependency(libs.cobalt.get()))
        include(dependency(libs.netty.http.get()))
    }
    relocate("org.squiddev.cobalt", "cc.tweaked.internal.cobalt")
    relocate("io.netty.handler.codec.http", "cc.tweaked.internal.netty.codec.http")
    minimize()
}

tasks.assemble { dependsOn("shadowJar") }

// Check tasks

tasks.test {
    systemProperty("cct.test-files", buildDir.resolve("tmp/testFiles").absolutePath)
}

val lintLua by tasks.registering(IlluaminateExec::class) {
    group = JavaBasePlugin.VERIFICATION_GROUP
    description = "Lint Lua (and Lua docs) with illuaminate"

    // Config files
    inputs.file(rootProject.file("illuaminate.sexp")).withPropertyName("illuaminate.sexp")
    // Sources
    inputs.files(rootProject.fileTree("doc")).withPropertyName("docs")
    inputs.files(project(":core").fileTree("src/main/resources/data/computercraft/lua")).withPropertyName("lua rom")
    inputs.files(luaJavadoc)

    args = listOf("lint")
    workingDir = rootProject.projectDir

    doFirst { if (System.getenv("GITHUB_ACTIONS") != null) println("::add-matcher::.github/matchers/illuaminate.json") }
    doLast { if (System.getenv("GITHUB_ACTIONS") != null) println("::remove-matcher owner=illuaminate::") }
}

val runGametest by tasks.registering(JavaExec::class) {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Runs tests on a temporary Minecraft instance."
    dependsOn("cleanRunGametest")
    usesService(MinecraftRunnerService.get(gradle))

    // Copy from runGameTestServer. We do it in this slightly odd way as runGameTestServer
    // isn't created until the task is configured (which is no good for us).
    val exec = tasks.getByName<JavaExec>("runGameTestServer")
    dependsOn(exec.dependsOn)
    exec.copyToFull(this)

    systemProperty("cctest.gametest-report", project.buildDir.resolve("test-results/$name.xml").absolutePath)
}
cct.jacoco(runGametest)
tasks.check { dependsOn(runGametest) }

val runGametestClient by tasks.registering(ClientJavaExec::class) {
    description = "Runs client-side gametests with no mods"
    copyFrom("runTestClient")
    tags("client")
}
cct.jacoco(runGametestClient)

tasks.register("checkClient") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Runs all client-only checks."
    dependsOn(runGametestClient)
}

// Upload tasks

val publishCurseForge by tasks.registering(TaskPublishCurseForge::class) {
    group = PublishingPlugin.PUBLISH_TASK_GROUP
    description = "Upload artifacts to CurseForge"

    apiToken = findProperty("curseForgeApiKey") ?: ""
    enabled = apiToken != ""

    val mainFile = upload("282001", tasks.shadowJar.get().archiveFile)
    dependsOn(tasks.shadowJar) // Ughr.
    mainFile.changelog =
        "Release notes can be found on the [GitHub repository](https://github.com/cc-tweaked/CC-Tweaked/releases/tag/v$mcVersion-$modVersion)."
    mainFile.changelogType = "markdown"
    mainFile.releaseType = if (isUnstable) "alpha" else "release"
    mainFile.gameVersions.add(mcVersion)
}

tasks.publish { dependsOn(publishCurseForge) }

modrinth {
    token.set(findProperty("modrinthApiKey") as String? ?: "")
    projectId.set("gu7yAYhd")
    versionNumber.set("$mcVersion-$modVersion")
    versionName.set(modVersion)
    versionType.set(if (isUnstable) "alpha" else "release")
    uploadFile.set(tasks.shadowJar as Any)
    gameVersions.add(mcVersion)
    changelog.set("Release notes can be found on the [GitHub repository](https://github.com/cc-tweaked/CC-Tweaked/releases/tag/v$mcVersion-$modVersion).")

    syncBodyFrom.set(provider { file("doc/mod-page.md").readText() })
}

tasks.publish { dependsOn(tasks.modrinth) }

// Don't publish the slim jar
for (cfg in listOf(configurations.apiElements, configurations.runtimeElements)) {
    cfg.configure { artifacts.removeIf { it.classifier == "slim" } }
}

publishing {
    publications {
        named("maven", MavenPublication::class) {
            fg.component(this)
            mavenDependencies {
                exclude(dependencies.create("cc.tweaked:"))
                exclude(libs.jei.forge.get())
            }
        }
    }
}
