plugins {
    id("cc-tweaked.publishing")
    id("cc-tweaked.fabric")
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
