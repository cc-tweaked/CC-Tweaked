package dan200.computercraft.core.apis

import dan200.computercraft.ComputerCraft
import dan200.computercraft.api.lua.ILuaAPI
import dan200.computercraft.api.lua.MethodResult
import dan200.computercraft.api.peripheral.IPeripheral
import dan200.computercraft.api.peripheral.IWorkMonitor
import dan200.computercraft.core.computer.BasicEnvironment
import dan200.computercraft.core.computer.ComputerSide
import dan200.computercraft.core.computer.IComputerEnvironment
import dan200.computercraft.core.filesystem.FileSystem
import dan200.computercraft.core.terminal.Terminal
import dan200.computercraft.core.tracking.TrackingField
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime


abstract class NullApiEnvironment : IAPIEnvironment {
    private val computerEnv = BasicEnvironment()

    override fun getComputerID(): Int = 0
    override fun getComputerEnvironment(): IComputerEnvironment = computerEnv
    override fun getMainThreadMonitor(): IWorkMonitor = throw IllegalStateException("Work monitor not available")
    override fun getTerminal(): Terminal = throw IllegalStateException("Terminal not available")
    override fun getFileSystem(): FileSystem = throw IllegalStateException("Terminal not available")
    override fun shutdown() {}
    override fun reboot() {}
    override fun setOutput(side: ComputerSide?, output: Int) {}
    override fun getOutput(side: ComputerSide?): Int = 0
    override fun getInput(side: ComputerSide?): Int = 0
    override fun setBundledOutput(side: ComputerSide?, output: Int) {}
    override fun getBundledOutput(side: ComputerSide?): Int = 0
    override fun getBundledInput(side: ComputerSide?): Int = 0
    override fun setPeripheralChangeListener(listener: IAPIEnvironment.IPeripheralChangeListener?) {}
    override fun getPeripheral(side: ComputerSide?): IPeripheral? = null
    override fun getLabel(): String? = null
    override fun setLabel(label: String?) {}
    override fun startTimer(ticks: Long): Int = 0
    override fun cancelTimer(id: Int) {}
    override fun addTrackingChange(field: TrackingField, change: Long) {}
}

class EventResult(val name: String, val args: Array<Any?>)

class AsyncRunner : NullApiEnvironment() {
    private val eventStream: Channel<Array<Any?>> = Channel(Int.MAX_VALUE)
    private val apis: MutableList<ILuaAPI> = mutableListOf()

    override fun queueEvent(event: String?, vararg args: Any?) {
        ComputerCraft.log.debug("Queue event $event ${args.contentToString()}")
        if (eventStream.trySend(arrayOf(event, *args)).isFailure) {
            throw IllegalStateException("Queue is full")
        }
    }

    override fun shutdown() {
        super.shutdown()
        eventStream.close()
        apis.forEach { it.shutdown() }
    }

    fun <T : ILuaAPI> addApi(api: T): T {
        apis.add(api)
        api.startup()
        return api
    }

    suspend fun resultOf(toRun: MethodResult): Array<Any?> {
        var running = toRun
        while (running.callback != null) running = runOnce(running)
        return running.result ?: empty
    }

    private suspend fun runOnce(obj: MethodResult): MethodResult {
        val callback = obj.callback ?: throw NullPointerException("Callback cannot be null")

        val result = obj.result
        val filter: String? = if (result.isNullOrEmpty() || result[0] !is String) {
            null
        } else {
            result[0] as String
        }

        return callback.resume(pullEventImpl(filter))
    }

    private suspend fun pullEventImpl(filter: String?): Array<Any?> {
        for (event in eventStream) {
            ComputerCraft.log.debug("Pulled event ${event.contentToString()}")
            val eventName = event[0] as String
            if (filter == null || eventName == filter || eventName == "terminate") return event
        }

        throw IllegalStateException("No more events")
    }

    suspend fun pullEvent(filter: String? = null): EventResult {
        val result = pullEventImpl(filter)
        return EventResult(result[0] as String, result.copyOfRange(1, result.size))
    }

    companion object {
        private val empty: Array<Any?> = arrayOf()

        @OptIn(ExperimentalTime::class)
        fun runTest(timeout: Duration = Duration.seconds(5), fn: suspend AsyncRunner.() -> Unit) {
            runBlocking {
                val runner = AsyncRunner()
                try {
                    withTimeout(timeout) { fn(runner) }
                } finally {
                    runner.shutdown()
                }
            }
        }
    }
}
