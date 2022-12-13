plugins {
    id("cc-tweaked.java-convention")
    id("cc-tweaked.publishing")
    id("cc-tweaked")
}

java {
    withJavadocJar()
}

// Due to the slightly circular nature of our API, add the main API jars to the javadoc classpath.
val docApi by configurations.registering {
    isTransitive = false
}

dependencies {
    compileOnlyApi(libs.jsr305)
    compileOnlyApi(libs.checkerFramework)
    compileOnlyApi(libs.jetbrainsAnnotations)

    "docApi"(project(":common-api"))
}

tasks.javadoc {
    // Depend on the common API when publishing javadoc
    classpath += docApi.get()
}
