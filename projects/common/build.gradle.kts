import cc.tweaked.gradle.annotationProcessorEverywhere
import cc.tweaked.gradle.clientClasses
import cc.tweaked.gradle.commonClasses

plugins {
    id("cc-tweaked.vanilla")
    id("cc-tweaked.gametest")
}

minecraft {
    accessWideners(
        "src/main/resources/computercraft.accesswidener",
        "src/main/resources/computercraft-common.accesswidener",
    )
}

dependencies {
    // Pull in our other projects. See comments in MinecraftConfigurations on this nastiness.
    implementation(project(":core"))
    implementation(commonClasses(project(":common-api")))
    clientImplementation(clientClasses(project(":common-api")))

    compileOnly(libs.bundles.externalMods.common)
    compileOnly(project(":forge-stubs"))

    compileOnly(libs.mixin)
    annotationProcessorEverywhere(libs.autoService)

    testImplementation(testFixtures(project(":core")))
    testImplementation(libs.bundles.test)
    testImplementation(project(":forge-stubs"))
    testRuntimeOnly(libs.bundles.testRuntime)

    testModImplementation(testFixtures(project(":core")))
    testModImplementation(libs.bundles.kotlin)
}
