package dan200.computercraft.ingame.mod

import dan200.computercraft.ingame.api.TestContext
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.minecraft.test.TestCollection
import net.minecraft.test.TestTrackerHolder
import java.lang.reflect.Method
import java.util.*
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.function.Consumer
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.Continuation
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.full.callSuspend
import kotlin.reflect.jvm.kotlinFunction

internal class TestRunner(private val name: String, private val method: Method) : Consumer<TestTrackerHolder> {
    override fun accept(t: TestTrackerHolder) {
        GlobalScope.launch(MainThread + CoroutineName(name)) {
            val testContext = TestContext(t)
            try {
                val instance = method.declaringClass.newInstance()
                val function = method.kotlinFunction;
                if (function == null) {
                    method.invoke(instance, testContext)
                } else {
                    function.callSuspend(instance, testContext)
                }
                testContext.ok()
            } catch (e: Exception) {
                testContext.fail(e)
            }
        }
    }
}

/**
 * A coroutine scope which runs everything on the main thread.
 */
internal object MainThread : AbstractCoroutineContextElement(ContinuationInterceptor), ContinuationInterceptor {
    private val queue: Queue<() -> Unit> = ConcurrentLinkedDeque()

    fun tick() {
        while (true) {
            val q = queue.poll() ?: break;
            q.invoke()
        }
    }

    override fun <T> interceptContinuation(continuation: Continuation<T>): Continuation<T> = MainThreadInterception(continuation)

    private class MainThreadInterception<T>(val cont: Continuation<T>) : Continuation<T> {
        override val context: CoroutineContext get() = cont.context

        override fun resumeWith(result: Result<T>) {
            queue.add { cont.resumeWith(result) }
        }
    }
}
