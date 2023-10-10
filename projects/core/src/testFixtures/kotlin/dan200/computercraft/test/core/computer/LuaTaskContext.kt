// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.test.core.computer

import dan200.computercraft.api.lua.ILuaAPI
import dan200.computercraft.api.lua.ILuaContext
import dan200.computercraft.api.lua.MethodResult
import dan200.computercraft.api.lua.ObjectArguments
import dan200.computercraft.core.apis.OSAPI
import dan200.computercraft.core.apis.PeripheralAPI
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration

/**
 * The context for tasks which consume Lua objects.
 *
 * This provides helpers for converting CC's callback-based code into a more direct style based on Kotlin coroutines.
 */
interface LuaTaskContext {
    /** The current Lua context, to be passed to method calls. */
    val context: ILuaContext

    /** Get a registered API. */
    fun <T : ILuaAPI> getApi(api: Class<T>): T

    /** Pull a Lua event */
    suspend fun pullEvent(event: String? = null): Array<out Any?>

    suspend fun pullEventOrTimeout(timeout: Duration, event: String? = null): Array<out Any?>? =
        withTimeoutOrNull(timeout) { pullEvent(event) }

    /** Resolve a [MethodResult] until completion, returning the resulting values. */
    suspend fun MethodResult.await(): Array<out Any?>? {
        var result = this
        while (true) {
            val callback = result.callback
            val values = result.result

            if (callback == null) return values

            val filter = if (values == null) null else values[0] as String?
            result = callback.resume(pullEvent(filter))
        }
    }

    /** Call a peripheral method. */
    suspend fun LuaTaskContext.callPeripheral(name: String, method: String, vararg args: Any?): Array<out Any?>? =
        getApi<PeripheralAPI>().call(context, ObjectArguments(name, method, *args)).await()

    /**
     * Sleep for the given duration. This uses the internal computer clock, so won't be accurate.
     */
    suspend fun LuaTaskContext.sleep(duration: Duration) {
        val timer = getApi<OSAPI>().startTimer(duration.inWholeMilliseconds / 1000.0)
        while (true) {
            val event = pullEvent("timer")
            if (event[0] == "timer" && event[1] is Number && (event[1] as Number).toInt() == timer) {
                return
            }
        }
    }
}

/** Get a registered API. */
inline fun <reified T : ILuaAPI> LuaTaskContext.getApi(): T = getApi(T::class.java)

abstract class AbstractLuaTaskContext : LuaTaskContext, AutoCloseable {
    private val pullEvents = mutableListOf<PullEvent>()
    private val apis = mutableMapOf<Class<out ILuaAPI>, ILuaAPI>()

    protected fun addApi(api: ILuaAPI) {
        apis[api.javaClass] = api
    }

    protected val hasEventListeners
        get() = pullEvents.isNotEmpty()

    protected fun queueEvent(eventName: String?, arguments: Array<out Any?>?) {
        val fullEvent: Array<out Any?> = when {
            eventName == null && arguments == null -> arrayOf()
            eventName != null && arguments == null -> arrayOf(eventName)
            eventName == null && arguments != null -> arguments
            else -> arrayOf(eventName, *arguments!!)
        }
        for (i in pullEvents.size - 1 downTo 0) {
            val puller = pullEvents[i]
            if (puller.name == null || puller.name == eventName || eventName == "terminate") {
                pullEvents.removeAt(i)
                puller.cont.resumeWith(Result.success(fullEvent))
            }
        }
    }

    override fun close() {
        for (pullEvent in pullEvents) pullEvent.cont.cancel()
        pullEvents.clear()
    }

    final override fun <T : ILuaAPI> getApi(api: Class<T>): T =
        api.cast(apis[api] ?: throw IllegalStateException("No API of type ${api.name}"))

    final override suspend fun pullEvent(event: String?): Array<out Any?> =
        suspendCancellableCoroutine { cont -> pullEvents.add(PullEvent(event, cont)) }

    private class PullEvent(val name: String?, val cont: CancellableContinuation<Array<out Any?>>)
}
