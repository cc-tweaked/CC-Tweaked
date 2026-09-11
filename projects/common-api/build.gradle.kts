// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

plugins {
    id("cc-tweaked.java-convention")
    id("cc-tweaked.publishing")
    id("cc-tweaked.vanilla")
}

val mcVersion = project.extensions.getByType<VersionCatalogsExtension>().named("libs")
    .findVersion("minecraft").get().toString()

java {
    withJavadocJar()
}

val snippetSourcesDependencies = configurations.dependencyScope("snippetSourcesDependencies")
val snippetSources = configurations.resolvable("snippetSources") {
    extendsFrom(snippetSourcesDependencies)
    attributes { attribute(DocsType.DOCS_TYPE_ATTRIBUTE, objects.named(DocsType.SAMPLES)) }
}

dependencies {
    api(project(":core-api"))

    "snippetSourcesDependencies"(project(":common"))
    "snippetSourcesDependencies"(project(":fabric"))
    "snippetSourcesDependencies"(project(":forge"))
}

val javadocOverview = tasks.register<Copy>("javadocOverview") {
    from("src/overview.html")
    into(layout.buildDirectory.dir(name))

    expand(
        mapOf(
            "mcVersion" to mcVersion,
            "modVersion" to version,
        ),
    )
}

tasks.javadoc {
    title = "CC: Tweaked $version for Minecraft $mcVersion"
    include("dan200/computercraft/api/**/*.java")

    options {
        (this as StandardJavadocDocletOptions)

        inputs.files(javadocOverview)
        overview(javadocOverview.get().destinationDir.resolve("overview.html").absolutePath)

        groups = mapOf(
            "Common" to listOf(
                "dan200.computercraft.api",
                "dan200.computercraft.api.lua",
                "dan200.computercraft.api.peripheral",
            ),
            "Upgrades" to listOf(
                "dan200.computercraft.api.client.turtle",
                "dan200.computercraft.api.pocket",
                "dan200.computercraft.api.turtle",
                "dan200.computercraft.api.upgrades",
            ),
        )

        addBooleanOption("-allow-script-in-comments", true)
        bottom(
            """
            <script src="https://cdn.jsdelivr.net/npm/prismjs@v1.29.0/components/prism-core.min.js"></script>
            <script src="https://cdn.jsdelivr.net/npm/prismjs@v1.29.0/plugins/autoloader/prism-autoloader.min.js"></script>
            <link href=" https://cdn.jsdelivr.net/npm/prismjs@1.29.0/themes/prism.min.css " rel="stylesheet">
            """.trimIndent(),
        )

        val snippetSources = snippetSources.get().files.toList()
        inputs.files(snippetSources)
        addPathOption("-snippet-path").value = snippetSources
    }

    // Include the core-api in our javadoc export. This is wrong, but it means we can export a single javadoc dump.
    source(project(":core-api").sourceSets.main.map { it.allJava })

    options {
        this as StandardJavadocDocletOptions
        addBooleanOption("-allow-script-in-comments", true)
        bottom(
            """
            <script src="https://cdn.jsdelivr.net/npm/prismjs@v1.29.0/components/prism-core.min.js"></script>
            <script src="https://cdn.jsdelivr.net/npm/prismjs@v1.29.0/plugins/autoloader/prism-autoloader.min.js"></script>
            <link href=" https://cdn.jsdelivr.net/npm/prismjs@1.29.0/themes/prism.min.css " rel="stylesheet">
            """.trimIndent(),
        )
    }
}
