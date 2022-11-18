import cc.tweaked.gradle.mavenDependencies

plugins {
    id("cc-tweaked.fabric")
    id("cc-tweaked.publishing")
}

val mcVersion: String by extra

java {
    withJavadocJar()
}

cct.inlineProject(":common-api")

dependencies {
    api(project(":core-api"))
    compileOnly(project(":forge-stubs"))
}

tasks.jar {
    manifest {
        attributes["Fabric-Loom-Remap"] = "true"
    }

    from(project(":core-api").sourceSets.main.get().output) // TODO(1.19.3): Remove when we've fixed GenericSource
    from("src/main/modJson") // TODO: Remove once Loom 1.1 is out.
}

// TODO(1.19.3): Remove when GenericSource no longer uses ResourceLocation. This forces us to bundle the core API with
//  the Fabric API, in order to remap those classes.

tasks.sourcesJar {
    from(project(":core-api").sourceSets.main.get().allJava)
}

tasks.withType(GenerateModuleMetadata::class).configureEach { isEnabled = false }
publishing {
    publications {
        named("maven", MavenPublication::class) {
            mavenDependencies {
                exclude(dependencies.create("cc.tweaked:"))
            }
        }
    }
}
