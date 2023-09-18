// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

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
    inputs.files(fileTree("src")).withPropertyName("sources")
    // Config files
    inputs.file("tsconfig.json").withPropertyName("Typescript config")
    inputs.file("rollup.config.js").withPropertyName("Rollup config")

    // Output directory. Also defined in illuaminate.sexp and rollup.config.js
    output.set(buildDir.resolve("rollup"))

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
    inputs.file("src/styles.css").withPropertyName("styles")

    // Output directory. Also defined in illuaminate.sexp and transform.tsx
    output.set(buildDir.resolve("illuaminate"))

    args = listOf("doc-gen")
    workingDir = rootProject.projectDir
}

val jsxDocs by tasks.registering(cc.tweaked.gradle.NpxExecToDir::class) {
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    description = "Post-processes documentation to statically render some dynamic content."

    // Config files
    inputs.file("tsconfig.json").withPropertyName("Typescript config")
    // Sources
    inputs.files(fileTree("src")).withPropertyName("sources")
    inputs.file(file("src/export/index.json")).withPropertyName("export")
    inputs.files(illuaminateDocs)

    // Output directory. Also defined in src/transform.tsx
    output.set(buildDir.resolve("jsxDocs"))

    args = listOf("tsx", "src/transform.tsx")
}

val docWebsite by tasks.registering(Copy::class) {
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    description = "Assemble docs and assets together into the documentation website."
    duplicatesStrategy = DuplicatesStrategy.FAIL

    from(jsxDocs)

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
    from(file("src/export/items")) { into("images/items") }

    into(buildDir.resolve("site"))
}

tasks.assemble { dependsOn(docWebsite) }
