// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

import cc.tweaked.gradle.getAbsolutePath

plugins {
    `lifecycle-base`
    id("cc-tweaked.node")
    id("cc-tweaked.illuaminate")
}

node {
    projectRoot.set(rootProject.projectDir)
}

illuaminate {
    version.set(libs.versions.illuaminate)
}

val rollup by tasks.registering(cc.tweaked.gradle.NpxExecToDir::class) {
    group = LifecycleBasePlugin.BUILD_GROUP
    description = "Bundles JS into rollup"

    // Sources
    inputs.files(fileTree("src/frontend")).withPropertyName("sources")
    // Config files
    inputs.file("tsconfig.json").withPropertyName("Typescript config")
    inputs.file("rollup.config.js").withPropertyName("Rollup config")

    // Output directory. Also defined in illuaminate.sexp and rollup.config.js
    output.set(layout.buildDirectory.dir("rollup"))

    args = listOf("rollup", "--config", "rollup.config.js")
}

val illuaminateDocs by tasks.registering(cc.tweaked.gradle.IlluaminateExecToDir::class) {
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    description = "Generates docs using Illuaminate"

    // Config files
    inputs.file(rootProject.file("illuaminate.sexp")).withPropertyName("illuaminate config")
    // Sources
    inputs.files(rootProject.fileTree("doc")).withPropertyName("docs")
    inputs.files(project(":core").fileTree("src/main/resources/data/computercraft/lua")).withPropertyName("lua rom")
    inputs.files(project(":forge").tasks.named("luaJavadoc"))
    // Additional assets
    inputs.files(rollup)
    inputs.file("src/frontend/styles.css").withPropertyName("styles")

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
            "tsx", sources.dir.resolve("index.tsx").absolutePath,
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

    // Pick up assets from the /docs folder
    from(rootProject.file("doc")) {
        include("logo.png")
        include("images/**")
    }
    // index.js is provided by illuaminate, but rollup outputs some other chunks
    from(rollup) { exclude("index.js") }
    // Grab illuaminate's assets. HTML files are provided by jsxDocs
    from(illuaminateDocs) { exclude("**/*.html") }
    // And item/block images from the data export
    from(file("src/htmlTransform/export/items")) { into("images/items") }

    into(layout.buildDirectory.dir("site"))
}

tasks.assemble { dependsOn(docWebsite) }
