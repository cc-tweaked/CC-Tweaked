// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.test.core.computer

import dan200.computercraft.api.lua.ILuaAPI
import dan200.computercraft.api.lua.ILuaContext
import dan200.computercraft.api.lua.LuaException
import dan200.computercraft.core.apis.IAPIEnvironment
import dan200.computercraft.test.core.apis.BasicApiEnvironment
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class LuaTaskRunner : AbstractLuaTaskContext() {
    private val eventStream: Channel<Event> = Channel(Channel.UNLIMITED)
    private val apis = mutableListOf<ILuaAPI>()

    val environment: IAPIEnvironment = object : BasicApiEnvironment(BasicEnvironment()) {
        override fun queueEvent(event: String?, vararg args: Any?) = this@LuaTaskRunner.queueEvent(event, args)

        override fun shutdown() {
            super.shutdown()
            eventStream.close()
        }
    }
    override val context =
        ILuaContext { throw LuaException("Cannot queue main thread task") }

    fun <T : ILuaAPI> addApi(api: T): T {
        super.addApi(api)
        apis.add(api)
        api.startup()
        return api
    }

    override fun close() {
        environment.shutdown()
    }

    private class Event(val name: String?, val args: Array<out Any?>)

    companion object {
        fun runTest(timeout: Duration = 5.seconds, fn: suspend LuaTaskRunner.() -> Unit) {
            runBlocking {
                withTimeout(timeout) {
                    LuaTaskRunner().use { fn(it) }
                }
            }
        }
    }
}
