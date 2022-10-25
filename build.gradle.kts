import cc.tweaked.gradle.*
import net.darkhax.curseforgegradle.TaskPublishCurseForge

plugins {
    // Build
    alias(libs.plugins.kotlin)
    alias(libs.plugins.forgeGradle)
    alias(libs.plugins.mixinGradle)
    alias(libs.plugins.librarian)
    alias(libs.plugins.shadow)
    // Publishing
    `maven-publish`
    alias(libs.plugins.curseForgeGradle)
    alias(libs.plugins.githubRelease)
    alias(libs.plugins.minotaur)
    // Utility
    alias(libs.plugins.taskTree)

    id("cc-tweaked.illuaminate")
    id("cc-tweaked.node")
    id("cc-tweaked.java-convention")
    id("cc-tweaked")
}

val isStable = true
val modVersion: String by extra
val mcVersion: String by extra

group = "org.squiddev"
version = modVersion
base.archivesName.set("cc-tweaked-$mcVersion")

java.registerFeature("extraMods") { usingSourceSet(sourceSets.main.get()) }

sourceSets {
    main {
        resources.srcDir("src/generated/resources")
    }

    register("testMod")
}

minecraft {
    runs {
        // configureEach would be better, but we need to eagerly configure configs or otherwise the run task doesn't
        // get set up properly.
        all {
            property("forge.logging.markers", "REGISTRIES")
            property("forge.logging.console.level", "debug")

            forceExit = false

            mods.register("computercraft") { source(sourceSets.main.get()) }
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
                "--mod",
                "computercraft",
                "--all",
                "--output",
                file("src/generated/resources/"),
                "--existing",
                file("src/main/resources/"),
            )
            property("cct.pretty-json", "true")
        }

        val testClient by registering {
            workingDirectory(file("run/testClient"))
            parent(client.get())

            mods.register("cctest") { source(sourceSets["testMod"]) }
        }

        val testServer by registering {
            workingDirectory(file("run/testServer"))
            parent(server.get())

            property("cctest.run", "true")
            property("forge.logging.console.level", "info")

            mods.register("cctest") { source(sourceSets["testMod"]) }
        }
    }

    mappings("parchment", "${libs.versions.parchmentMc.get()}-${libs.versions.parchment.get()}-$mcVersion")

    accessTransformer(file("src/main/resources/META-INF/accesstransformer.cfg"))
    accessTransformer(file("src/testMod/resources/META-INF/accesstransformer.cfg"))
}

mixin {
    add(sourceSets.main.get(), "computercraft.mixins.refmap.json")
    config("computercraft.mixins.json")
}

reobf {
    register("shadowJar")
}

configurations {
    val shade by registering { isTransitive = false }
    implementation { extendsFrom(shade.get()) }
    register("cctJavadoc")

    named("testModImplementation") { extendsFrom(implementation.get(), testImplementation.get()) }
}

dependencies {
    minecraft("net.minecraftforge:forge:$mcVersion-${libs.versions.forge.get()}")
    annotationProcessor("org.spongepowered:mixin:0.8.4:processor")

    compileOnly(libs.jetbrainsAnnotations)
    annotationProcessorEverywhere(libs.autoService)

    "extraModsCompileOnly"(fg.deobf("mezz.jei:jei-1.16.5:7.7.0.104:api"))
    "extraModsRuntimeOnly"(fg.deobf("mezz.jei:jei-1.16.5:7.7.0.104"))

    "extraModsCompileOnly"(fg.deobf("com.blamejared.crafttweaker:CraftTweaker-1.16.5:7.1.0.313"))
    "extraModsCompileOnly"(fg.deobf("commoble.morered:morered-1.16.5:2.1.1.0"))

    "shade"(libs.cobalt)

    testImplementation(libs.bundles.test)
    testImplementation(libs.bundles.kotlin)
    testRuntimeOnly(libs.bundles.testRuntime)

    "testModImplementation"(sourceSets.main.get().output)

    "cctJavadoc"(libs.cctJavadoc)
}

illuaminate {
    version.set(libs.versions.illuaminate)
}

// Compile tasks

tasks.javadoc {
    include("dan200/computercraft/api/**/*.java")
    (options as StandardJavadocDocletOptions).links("https://docs.oracle.com/javase/8/docs/api/")
}

val apiJar by tasks.registering(Jar::class) {
    archiveClassifier.set("api")
    from(sourceSets.main.get().output) {
        include("dan200/computercraft/api/**/*")
    }
}

tasks.assemble { dependsOn(apiJar) }

val luaJavadoc by tasks.registering(Javadoc::class) {
    description = "Generates documentation for Java-side Lua functions."
    group = JavaBasePlugin.DOCUMENTATION_GROUP

    source(sourceSets.main.get().java)
    setDestinationDir(buildDir.resolve("docs/luaJavadoc"))
    classpath = sourceSets.main.get().compileClasspath

    options.docletpath = configurations["cctJavadoc"].files.toList()
    options.doclet = "cc.tweaked.javadoc.LuaDoclet"
    (options as StandardJavadocDocletOptions).noTimestamp(false)

    javadocTool.set(
        javaToolchains.javadocToolFor {
            languageVersion.set(JavaLanguageVersion.of(11))
        },
    )
}

tasks.processResources {
    inputs.property("modVersion", modVersion)
    inputs.property("forgeVersion", libs.versions.forge.get())
    inputs.property("gitHash", cct.gitHash)

    filesMatching("data/computercraft/lua/rom/help/credits.txt") {
        expand(mapOf("gitContributors" to cct.gitContributors.get().joinToString("\n")))
    }

    filesMatching("META-INF/mods.toml") {
        expand(mapOf("forgeVersion" to libs.versions.forge.get(), "file" to mapOf("jarVersion" to modVersion)))
    }
}

tasks.jar {
    isReproducibleFileOrder = true
    isPreserveFileTimestamps = false
    finalizedBy("reobfJar")
    archiveClassifier.set("slim")

    manifest {
        attributes(
            "Specification-Title" to "computercraft",
            "Specification-Vendor" to "SquidDev",
            "Specification-Version" to "1",
            "specificationVersion" to "cctweaked",
            "Implementation-Version" to modVersion,
            "Implementation-Vendor" to "SquidDev",
        )
    }
}

tasks.shadowJar {
    finalizedBy("reobfShadowJar")

    archiveClassifier.set("")
    configurations = listOf(project.configurations["shade"])
    relocate("org.squiddev.cobalt", "cc.tweaked.internal.cobalt")
    minimize()
}

tasks.assemble { dependsOn("shadowJar") }

// Web tasks

val rollup by tasks.registering(NpxExecToDir::class) {
    group = LifecycleBasePlugin.BUILD_GROUP
    description = "Bundles JS into rollup"

    // Sources
    inputs.files(fileTree("src/web")).withPropertyName("sources")
    // Config files
    inputs.file("tsconfig.json").withPropertyName("Typescript config")
    inputs.file("rollup.config.js").withPropertyName("Rollup config")

    // Output directory. Also defined in illuaminate.sexp and rollup.config.js
    output.set(buildDir.resolve("rollup"))

    args = listOf("rollup", "--config", "rollup.config.js")
}

val illuaminateDocs by tasks.registering(IlluaminateExecToDir::class) {
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    description = "Generates docs using Illuaminate"

    // Config files
    inputs.file("illuaminate.sexp").withPropertyName("illuaminate.sexp")
    // Sources
    inputs.files(fileTree("doc")).withPropertyName("docs")
    inputs.files(fileTree("src/main/resources/data/computercraft/lua")).withPropertyName("lua rom")
    inputs.files(luaJavadoc)
    // Additional assets
    inputs.files(rollup)
    inputs.file("src/web/styles.css").withPropertyName("styles")

    // Output directory. Also defined in illuaminate.sexp and transform.tsx
    output.set(buildDir.resolve("illuaminate"))

    args = listOf("doc-gen")
}

val jsxDocs by tasks.registering(NpxExecToDir::class) {
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    description = "Post-processes documentation to statically render some dynamic content."

    // Config files
    inputs.file("tsconfig.json").withPropertyName("Typescript config")
    // Sources
    inputs.files(fileTree("src/web")).withPropertyName("sources")
    inputs.file("src/generated/export/index.json").withPropertyName("export")
    inputs.files(illuaminateDocs)

    // Output directory. Also defined in src/web/transform.tsx
    output.set(buildDir.resolve("jsxDocs"))

    args = listOf("ts-node", "-T", "--esm", "src/web/transform.tsx")
}

val docWebsite by tasks.registering(Copy::class) {
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    description = "Assemble docs and assets together into the documentation website."

    from(jsxDocs)

    from("doc") {
        include("logo.png")
        include("images/**")
    }
    from(rollup) { exclude("index.js") }
    from(illuaminateDocs) { exclude("**/*.html") }
    from("src/generated/export/items") { into("images/items") }

    into(buildDir.resolve("docs/site"))
}

// Check tasks

tasks.test {
    systemProperty("cct.test-files", buildDir.resolve("tmp/test-files").absolutePath)
}

val lintLua by tasks.registering(IlluaminateExec::class) {
    group = JavaBasePlugin.VERIFICATION_GROUP
    description = "Lint Lua (and Lua docs) with illuaminate"

    // Config files
    inputs.file("illuaminate.sexp").withPropertyName("illuaminate.sexp")
    // Sources
    inputs.files(fileTree("doc")).withPropertyName("docs")
    inputs.files(fileTree("src/main/resources/data/computercraft/lua")).withPropertyName("lua rom")
    inputs.files(luaJavadoc)

    args = listOf("lint")

    doFirst { if (System.getenv("GITHUB_ACTIONS") != null) println("::add-matcher::.github/matchers/illuaminate.json") }
    doLast { if (System.getenv("GITHUB_ACTIONS") != null) println("::remove-matcher owner=illuaminate::") }
}

val setupRunGametest by tasks.registering(Copy::class) {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Sets up the environment for the test server."

    from("src/testMod/server-files") {
        include("eula.txt")
        include("server.properties")
    }
    into("run/testServer")
}

val runGametest by tasks.registering(JavaExec::class) {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Runs tests on a temporary Minecraft instance."
    dependsOn(setupRunGametest, "cleanRunGametest")

    // Copy from runTestServer. We do it in this slightly odd way as runTestServer
    // isn't created until the task is configured (which is no good for us).
    val exec = tasks.getByName<JavaExec>("runTestServer")
    dependsOn(exec.dependsOn)
    exec.copyToFull(this)
}

cct.jacoco(runGametest)

tasks.check { dependsOn(runGametest) }

// Upload tasks

val checkChangelog by tasks.registering(CheckChangelog::class) {
    version.set(modVersion)
    whatsNew.set(file("src/main/resources/data/computercraft/lua/rom/help/whatsnew.md"))
    changelog.set(file("src/main/resources/data/computercraft/lua/rom/help/changelog.md"))
}

tasks.check { dependsOn(checkChangelog) }

val publishCurseForge by tasks.registering(TaskPublishCurseForge::class) {
    group = PublishingPlugin.PUBLISH_TASK_GROUP
    description = "Upload artifacts to CurseForge"

    apiToken = findProperty("curseForgeApiKey") ?: ""
    enabled = apiToken != ""

    val mainFile = upload("282001", tasks.shadowJar)
    dependsOn(tasks.shadowJar) // Ughr.
    mainFile.changelog = "Release notes can be found on the [GitHub repository](https://github.com/cc-tweaked/CC-Tweaked/releases/tag/v$mcVersion-$modVersion)."
    mainFile.changelogType = "markdown"
    mainFile.releaseType = if (isStable) "release" else "alpha"
    mainFile.gameVersions.add(mcVersion)
}

tasks.publish { dependsOn(publishCurseForge) }

modrinth {
    token.set(findProperty("modrinthApiKey") as String? ?: "")
    projectId.set("gu7yAYhd")
    versionNumber.set("$mcVersion-$modVersion")
    versionName.set(modVersion)
    versionType.set(if (isStable) "release" else "alpha")
    uploadFile.set(tasks.shadowJar as Any)
    gameVersions.add(mcVersion)
    changelog.set("Release notes can be found on the [GitHub repository](https://github.com/cc-tweaked/CC-Tweaked/releases/tag/v$mcVersion-$modVersion).")

    syncBodyFrom.set(provider { file("doc/mod-page.md").readText() })
}

tasks.publish { dependsOn(tasks.modrinth) }

githubRelease {
    token(findProperty("githubApiKey") as String? ?: "")
    owner.set("cc-tweaked")
    repo.set("CC-Tweaked")
    targetCommitish.set(cct.gitBranch)

    tagName.set("v$mcVersion-$modVersion")
    releaseName.set("[$mcVersion] $modVersion")
    body.set(
        provider {
            "## " + file("src/main/resources/data/computercraft/lua/rom/help/whatsnew.md")
                .readLines()
                .takeWhile { it != "Type \"help changelog\" to see the full version history." }
                .joinToString("\n").trim()
        },
    )
    prerelease.set(!isStable)
}

tasks.publish { dependsOn(tasks.githubRelease) }

publishing {
    publications {
        register<MavenPublication>("maven") {
            artifactId = base.archivesName.get()
            from(components["java"])
            artifact(apiJar)
            fg.component(this)

            pom {
                name.set("CC: Tweaked")
                description.set("CC: Tweaked is a fork of ComputerCraft, adding programmable computers, turtles and more to Minecraft.")
                url.set("https://github.com/cc-tweaked/CC-Tweaked")

                scm {
                    url.set("https://github.com/cc-tweaked/CC-Tweaked.git")
                }

                issueManagement {
                    system.set("github")
                    url.set("https://github.com/cc-tweaked/CC-Tweaked/issues")
                }

                licenses {
                    license {
                        name.set("ComputerCraft Public License, Version 1.0")
                        url.set("https://github.com/cc-tweaked/CC-Tweaked/blob/HEAD/LICENSE")
                    }
                }
            }
        }
    }

    repositories {
        maven("https://squiddev.cc/maven") {
            name = "SquidDev"
        }
    }
}
