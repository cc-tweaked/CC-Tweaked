package cc.tweaked.gradle

import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.tasks.JavaExec

fun DependencyHandler.annotationProcessorEverywhere(dep: Any) {
    add("compileOnly", dep)
    add("annotationProcessor", dep)

    add("testCompileOnly", dep)
    add("testAnnotationProcessor", dep)
}

fun JavaExec.copyToFull(spec: JavaExec) {
    copyTo(spec)
    spec.classpath = classpath
    spec.mainClass.set(mainClass)
    spec.javaLauncher.set(javaLauncher)
    spec.args = args
}
