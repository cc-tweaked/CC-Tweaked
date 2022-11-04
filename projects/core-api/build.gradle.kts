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
    compileOnly(project(":mc-stubs"))
    compileOnlyApi(libs.jsr305)
    compileOnlyApi(libs.checkerFramework)

    "docApi"(project(":"))
}

tasks.javadoc {
    // Depend on
    classpath += docApi
}
