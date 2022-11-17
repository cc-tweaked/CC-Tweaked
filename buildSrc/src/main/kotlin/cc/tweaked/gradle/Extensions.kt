package cc.tweaked.gradle

import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.specs.Spec
import org.gradle.api.tasks.JavaExec
import org.gradle.process.JavaExecSpec

/**
 * Add an annotation processor to all source sets.
 */
fun DependencyHandler.annotationProcessorEverywhere(dep: Any) {
    add("compileOnly", dep)
    add("annotationProcessor", dep)

    add("clientCompileOnly", dep)
    add("clientAnnotationProcessor", dep)

    add("testCompileOnly", dep)
    add("testAnnotationProcessor", dep)
}

/**
 * A version of [JavaExecSpec.copyTo] which copies *all* properties.
 */
fun JavaExec.copyToFull(spec: JavaExec) {
    copyTo(spec)
    spec.classpath = classpath
    spec.mainClass.set(mainClass)
    spec.javaLauncher.set(javaLauncher)
    spec.args = args
}

/**
 * An alternative to [Nothing] with a more descriptive name. Use to enforce calling a function with named arguments:
 *
 * ```kotlin
 * fun f(vararg unused: UseNamedArgs, arg1: Int, arg2: Int) {
 *   // ...
 * }
 * ```
 */
class UseNamedArgs private constructor()
