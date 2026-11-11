// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

import cc.tweaked.gradle.CCTweakedJavaVersions
import cc.tweaked.gradle.ExternalProjectArtifacts
import cc.tweaked.gradle.getAbsolutePath

plugins {
    id("cc-tweaked.java-convention")
    id("cc-tweaked.node")
    id("cc-tweaked.illuaminate")
    id("cc-tweaked.teavm")
}

val rootProjectDir = rootProject.isolated.projectDirectory

node {
    projectRoot = rootProjectDir
}

illuaminate {
    version = libs.versions.illuaminate
}

repositories {
    exclusiveContent {
        forRepositories(maven("https://teavm.org/maven/repository"))
        filter {
            includeGroup("org.teavm")
        }
    }
}

dependencies {
    compileOnly(libs.autoService)
    annotationProcessor(libs.autoService)

    implementation(project(":core"))
    implementation(libs.bundles.teavm.impl)
    implementation(libs.guava)
    implementation(libs.netty.http)
    implementation(libs.slf4j)
    runtimeOnly(libs.bundles.teavm.runtime)

    teavm(libs.teavm.tooling)
}

// Force us to use Java 25 for the main classes, so we can use the Classfile API.
tasks.compileJava { options.release = CCTweakedJavaVersions.JDK_VERSION.asInt() }

val bundleResources = tasks.register<cc.tweaked.gradle.JsArchive>("bundleResources") {
    prefix = "data/computercraft/lua/"
    input.from(configurations.runtimeClasspath.map(ExternalProjectArtifacts::resources))
    output = layout.buildDirectory.file("teaVM/resources.js")
}

val compileTeaVMJs = tasks.register<cc.tweaked.gradle.CompileTeaVM>("compileTeaVMJs") {
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "cc.tweaked.web.Main"
    minify = !project.hasProperty("noMinify")
    output = layout.buildDirectory.file("teaVM/classes.js")
}

val compileTeaVMWasm = tasks.register<cc.tweaked.gradle.CompileTeaVM>("compileTeaVMWasm") {
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "cc.tweaked.web.Main"
    minify = !project.hasProperty("noMinify")
    output = layout.buildDirectory.file("teaVM/classes.wasm")
}

val extractWasmModule = tasks.register<Copy>("extractWasmModule") {
    group = LifecycleBasePlugin.BUILD_GROUP
    description = "Extract our WASM runtime from the TeaVM jar"

    from(
        configurations.teavmResolvable.map { files ->
            files.map {
                (if (it.isFile) zipTree(it) else fileTree(it))
                    // We filter the tree now, then extract map to individual files, to ensure the Copy task writes the
                    // files to the root, not to the org/teavm/... folder.
                    .matching { include("org/teavm/backend/wasm/wasm-gc-module-runtime.js") }
                    .elements
            }
        },
    )

    rename { "wasm-gc-runtime.js" }
    into(layout.buildDirectory.dir("teaVM"))
}

val rollup = tasks.register<cc.tweaked.gradle.NpxExecToDir>("rollup") {
    group = LifecycleBasePlugin.BUILD_GROUP
    description = "Bundles JS into rollup"

    val minify = !project.hasProperty("noMinify")
    inputs.property("minify", minify)

    // Sources
    inputs.files(fileTree("src/frontend")).withPropertyName("sources")
    inputs.files(bundleResources, compileTeaVMJs, compileTeaVMWasm, extractWasmModule)
    // Config files
    inputs.file("tsconfig.json").withPropertyName("Typescript config")
    inputs.file("rollup.config.js").withPropertyName("Rollup config")

    // Output directory. Also defined in illuaminate.sexp and rollup.config.js
    output = layout.buildDirectory.dir("rollup")

    args = listOf("rollup", "--config", "rollup.config.js") + if (minify) emptyList() else listOf("--configDebug")
}

val illuaminateDocs = tasks.register<cc.tweaked.gradle.IlluaminateExecToDir>("illuaminateDocs") {
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    description = "Generates docs using Illuaminate"

    // Config files
    inputs.file(rootProjectDir.file("illuaminate.sexp")).withPropertyName("illuaminate config")
    // Sources
    inputs.dir(rootProjectDir.dir("doc")).withPropertyName("docs")
    inputs.files(fileTree("../core/src/main/resources/data/computercraft/lua")).withPropertyName("lua rom")
    inputs.files(configurations.detachedConfiguration(dependencies.project(":common", "luaJavadocElements")))
        .withPropertyName("luaJavadoc")
    // Assets
    inputs.files(rollup)

    // Output directory. Also defined in illuaminate.sexp.
    output = layout.buildDirectory.dir("illuaminate")

    args("doc-gen")
    workingDir = rootProject.projectDir
}

val htmlTransform = tasks.register<cc.tweaked.gradle.NpxExecToDir>("htmlTransform") {
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    description = "Post-processes documentation to statically render some dynamic content."

    val sources = fileTree("src/htmlTransform")

    // Config files
    inputs.file("tsconfig.json").withPropertyName("Typescript config")
    // Sources
    inputs.files(sources).withPropertyName("sources")
    inputs.files(illuaminateDocs)

    // Output directory.
    output = layout.buildDirectory.dir(name)

    args(
        "tsx",
        sources.dir.resolve("index.tsx").absolutePath,
        illuaminateDocs.get().output.getAbsolutePath(),
        sources.dir.resolve("export/index.json").absolutePath,
        output.getAbsolutePath(),
    )
}

val docWebsite = tasks.register<Copy>("docWebsite") {
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    description = "Assemble docs and assets together into the documentation website."
    duplicatesStrategy = DuplicatesStrategy.FAIL

    from(htmlTransform)

    // index.js is provided by illuaminate, but rollup outputs some other chunks
    from(rollup) { exclude("index.js") }
    // Grab illuaminate's assets. HTML files are provided by jsxDocs
    from(illuaminateDocs) { exclude("**/*.html") }
    // And item/block images from the data export
    from(file("src/htmlTransform/export/items")) { into("images/items") }
    // Add the common-api (and core-api) javadoc
    val javadoc = configurations.detachedConfiguration(dependencies.project(":common-api", "javadocElements"))
    javadoc.attributes.attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, ArtifactTypeDefinition.DIRECTORY_TYPE)
    from(javadoc) { into("javadoc") }

    into(layout.buildDirectory.dir("site"))
}

tasks.assemble { dependsOn(docWebsite) }

cct.linters(minecraft = false)
