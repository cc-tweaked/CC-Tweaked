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
