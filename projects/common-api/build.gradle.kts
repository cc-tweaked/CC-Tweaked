plugins {
    id("cc-tweaked.java-convention")
    id("cc-tweaked.publishing")
    id("cc-tweaked.vanilla")
}

java {
    withJavadocJar()
}

dependencies {
    api(project(":core-api"))
    compileOnly(project(":forge-stubs"))
}

tasks.javadoc {
    include("dan200/computercraft/api/**/*.java")
}
