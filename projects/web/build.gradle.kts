// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

import cc.tweaked.gradle.getAbsolutePath

plugins {
    id("cc-tweaked.java-convention")
    id("cc-tweaked.node")
    id("cc-tweaked.illuaminate")
}

val modVersion: String by extra

node {
    projectRoot.set(rootProject.projectDir)
}

illuaminate {
    version.set(libs.versions.illuaminate)
}

sourceSets.register("builder")

dependencies {
    implementation(project(":core"))
    implementation(libs.bundles.teavm.api)
    implementation(libs.asm)
    implementation(libs.guava)
    implementation(libs.netty.http)
    implementation(libs.slf4j)
    runtimeOnly(libs.teavm.core) // Contains the TeaVM runtime

    "builderCompileOnly"(libs.bundles.annotations)
    "builderImplementation"(libs.bundles.teavm.tooling)
    "builderImplementation"(libs.asm)
    "builderImplementation"(libs.asm.commons)
}

val compileTeaVM by tasks.registering(JavaExec::class) {
    group = LifecycleBasePlugin.BUILD_GROUP
    description = "Generate our classes and resources files"

    val output = layout.buildDirectory.dir("teaVM")
    val minify = !project.hasProperty("noMinify")

    inputs.property("version", modVersion)
    inputs.property("minify", minify)
    inputs.files(sourceSets.main.get().runtimeClasspath).withPropertyName("inputClasspath")
    outputs.dir(output).withPropertyName("output")

    classpath = sourceSets["builder"].runtimeClasspath
    jvmArguments.addAll(
        provider {
            val main = sourceSets.main.get()
            listOf(
                "-Dcct.input=${main.output.classesDirs.asPath}",
                "-Dcct.version=$modVersion",
                "-Dcct.classpath=${main.runtimeClasspath.asPath}",
                "-Dcct.output=${output.getAbsolutePath()}",
                "-Dcct.minify=$minify",
            )
        },
    )
    mainClass.set("cc.tweaked.web.builder.Builder")
    javaLauncher.set(project.javaToolchains.launcherFor { languageVersion.set(java.toolchain.languageVersion) })
}

val rollup by tasks.registering(cc.tweaked.gradle.NpxExecToDir::class) {
    group = LifecycleBasePlugin.BUILD_GROUP
    description = "Bundles JS into rollup"

    val minify = !project.hasProperty("noMinify")
    inputs.property("minify", minify)

    // Sources
    inputs.files(fileTree("src/frontend")).withPropertyName("sources")
    inputs.files(compileTeaVM)
    // Config files
    inputs.file("tsconfig.json").withPropertyName("Typescript config")
    inputs.file("rollup.config.js").withPropertyName("Rollup config")

    // Output directory. Also defined in illuaminate.sexp and rollup.config.js
    output.set(layout.buildDirectory.dir("rollup"))

    args = listOf("rollup", "--config", "rollup.config.js") + if (minify) emptyList() else listOf("--configDebug")
}

val illuaminateDocs by tasks.registering(cc.tweaked.gradle.IlluaminateExecToDir::class) {
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    description = "Generates docs using Illuaminate"

    // Config files
    inputs.file(rootProject.file("illuaminate.sexp")).withPropertyName("illuaminate config")
    // Sources
    inputs.files(rootProject.fileTree("doc")).withPropertyName("docs")
    inputs.files(project(":core").fileTree("src/main/resources/data/computercraft/lua")).withPropertyName("lua rom")
    inputs.files(project(":common").tasks.named("luaJavadoc"))
    // Assets
    inputs.files(rollup)

    // Output directory. Also defined in illuaminate.sexp.
    output.set(layout.buildDirectory.dir("illuaminate"))

    args = listOf("doc-gen")
    workingDir = rootProject.projectDir
}

val htmlTransform by tasks.registering(cc.tweaked.gradle.NpxExecToDir::class) {
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    description = "Post-processes documentation to statically render some dynamic content."

    val sources = fileTree("src/htmlTransform")

    // Config files
    inputs.file("tsconfig.json").withPropertyName("Typescript config")
    // Sources
    inputs.files(sources).withPropertyName("sources")
    inputs.files(illuaminateDocs)

    // Output directory.
    output.set(layout.buildDirectory.dir(name))

    argumentProviders.add {
        listOf(
            "tsx",
            sources.dir.resolve("index.tsx").absolutePath,
            illuaminateDocs.get().output.getAbsolutePath(),
            sources.dir.resolve("export/index.json").absolutePath,
            output.getAbsolutePath(),
        )
    }
}

val docWebsite by tasks.registering(Copy::class) {
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
    from(project(":common-api").tasks.named("javadoc")) { into("javadoc") }

    into(layout.buildDirectory.dir("site"))
}

tasks.assemble { dependsOn(docWebsite) }
