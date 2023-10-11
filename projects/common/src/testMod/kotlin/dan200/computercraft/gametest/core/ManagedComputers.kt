// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.gametest.core

import dan200.computercraft.core.apis.OSAPI
import dan200.computercraft.core.lua.CobaltLuaMachine
import dan200.computercraft.core.lua.ILuaMachine
import dan200.computercraft.core.lua.MachineEnvironment
import dan200.computercraft.gametest.api.thenOnComputer
import dan200.computercraft.mixin.gametest.GameTestInfoAccessor
import dan200.computercraft.shared.computer.core.ServerContext
import dan200.computercraft.test.core.computer.KotlinLuaMachine
import dan200.computercraft.test.core.computer.LuaTaskContext
import net.minecraft.gametest.framework.GameTestAssertException
import net.minecraft.gametest.framework.GameTestAssertPosException
import net.minecraft.gametest.framework.GameTestInfo
import net.minecraft.gametest.framework.GameTestSequence
import org.slf4j.LoggerFactory
import java.io.InputStream
import java.util.*
import java.util.concurrent.CancellationException
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicReference

/**
 * Provides a custom [ILuaMachine] which allows computers to run Kotlin or Lua code, depending on their ID.
 *
 * This allows writing game tests which consume Lua APIs, without having the overhead of starting a new computer for
 * each test.
 *
 * @see GameTestSequence.thenOnComputer
 */
object ManagedComputers : ILuaMachine.Factory {
    private val LOGGER = LoggerFactory.getLogger(ManagedComputers::class.java)
    private val computers: MutableMap<String, Queue<suspend LuaTaskContext.() -> Unit>> = mutableMapOf()

    internal fun enqueue(test: GameTestInfo, label: String, task: suspend LuaTaskContext.() -> Unit): Monitor {
        val monitor = Monitor(test, label)
        computers.computeIfAbsent(label) { ConcurrentLinkedDeque() }.add {
            try {
                LOGGER.info("Running $label")
                task()
                monitor.result.set(Result.success(Unit))
            } catch (e: Throwable) {
                if (e !is AssertionError && e !is CancellationException) LOGGER.error("Computer $label failed", e)
                monitor.result.set(Result.failure(e))
                throw e
            } finally {
                LOGGER.info("Finished $label")
            }
        }

        ServerContext.get(test.level.server).registry().computers
            .firstOrNull { it.label == label }?.queueEvent("test_wakeup")

        return monitor
    }

    override fun create(environment: MachineEnvironment, bios: InputStream): ILuaMachine {
        val os = environment.apis.asSequence().filterIsInstance(OSAPI::class.java).first()
        val id = os.computerID
        val label = os.computerLabel
        return when {
            id != 1 -> CobaltLuaMachine(environment, bios)
            label != null && label[0] != null -> KotlinMachine(environment, label[0] as String)
            else -> {
                LOGGER.error("Kotlin Lua machine must have a label")
                CobaltLuaMachine(environment, bios)
            }
        }
    }

    private class KotlinMachine(environment: MachineEnvironment, private val label: String) :
        KotlinLuaMachine(environment) {
        override fun getTask(): (suspend KotlinLuaMachine.() -> Unit)? = computers[label]?.poll()
    }

    class Monitor(private val test: GameTestInfo, private val label: String) {
        internal val result = AtomicReference<Result<Unit>>()

        val isFinished
            get() = result.get() != null

        fun check() {
            val result = result.get() ?: fail("Computer $label did not finish")
            val error = result.exceptionOrNull()
            if (error != null) fail(error.message ?: error.toString())
        }

        private fun fail(message: String): Nothing {
            val computer =
                ServerContext.get(test.level.server).registry().computers.firstOrNull { it.label == label }
            if (computer == null) {
                throw GameTestAssertException(message)
            } else {
                val pos = computer.position
                val relativePos = pos.subtract(test.structureBlockPos)
                throw GameTestAssertPosException(message, pos, relativePos, (test as GameTestInfoAccessor).`computercraft$getTick`())
            }
        }
    }
}
