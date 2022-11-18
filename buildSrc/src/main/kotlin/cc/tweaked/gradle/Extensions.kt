package cc.tweaked.gradle

import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.tasks.JavaExec
import org.gradle.process.BaseExecSpec
import org.gradle.process.JavaExecSpec
import org.gradle.process.ProcessForkOptions

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

    // Additional Java options
    spec.jvmArgs = jvmArgs // Fabric overrides getJvmArgs so copyTo doesn't do the right thing.
    spec.args = args
    spec.argumentProviders.addAll(argumentProviders)
    spec.mainClass.set(mainClass)
    spec.classpath = classpath
    spec.javaLauncher.set(javaLauncher)
    if (executable != null) spec.setExecutable(executable!!)

    // Additional ExecSpec options
    copyToExec(spec)
}

/**
 * Copy additional [BaseExecSpec] options which aren't handled by [ProcessForkOptions.copyTo].
 */
fun BaseExecSpec.copyToExec(spec: BaseExecSpec) {
    spec.isIgnoreExitValue = isIgnoreExitValue
    if (standardInput != null) spec.standardInput = standardInput
    if (standardOutput != null) spec.standardOutput = standardOutput
    if (errorOutput != null) spec.errorOutput = errorOutput
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

/**
 * An [AutoCloseable] implementation which can be used to combine other [AutoCloseable] instances.
 *
 * Values which implement [AutoCloseable] can be dynamically registered with [CloseScope.add]. When the scope is closed,
 * each value is closed in the opposite order.
 *
 * This is largely intended for cases where it's not appropriate to nest [AutoCloseable.use], for instance when nested
 * would be too deep.
 */
class CloseScope : AutoCloseable {
    private val toClose = ArrayDeque<AutoCloseable>()

    /**
     * Add a value to be closed when this scope is closed.
     */
    public fun add(value: AutoCloseable) {
        toClose.addLast(value)
    }

    override fun close() {
        close(null)
    }

    @PublishedApi
    internal fun close(baseException: Throwable?) {
        var exception = baseException

        while (true) {
            var toClose = toClose.removeLastOrNull() ?: break
            try {
                toClose.close()
            } catch (e: Throwable) {
                if (exception == null) {
                    exception = e
                } else {
                    exception.addSuppressed(e)
                }
            }
        }

        if (exception != null) throw exception
    }

    inline fun <R> use(block: (CloseScope) -> R): R {
        var exception: Throwable? = null
        try {
            return block(this)
        } catch (e: Throwable) {
            exception = e
            throw e
        } finally {
            close(exception)
        }
    }
}
