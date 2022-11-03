/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.test.core.computer

import dan200.computercraft.api.lua.ILuaAPI
import dan200.computercraft.core.ComputerContext
import dan200.computercraft.core.computer.Computer
import dan200.computercraft.core.computer.ComputerThread
import dan200.computercraft.core.computer.TimeoutState
import dan200.computercraft.core.computer.mainthread.NoWorkMainThreadScheduler
import dan200.computercraft.core.lua.MachineEnvironment
import dan200.computercraft.core.lua.MachineResult
import dan200.computercraft.core.terminal.Terminal
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

typealias FakeComputerTask = (state: TimeoutState) -> MachineResult

/**
 * Creates "fake" computers, which just run user-defined tasks rather than Lua code.
 */
class KotlinComputerManager : AutoCloseable {

    private val machines: MutableMap<Computer, Queue<FakeComputerTask>> = HashMap()
    private val context = ComputerContext(BasicEnvironment(), ComputerThread(1), NoWorkMainThreadScheduler()) { DummyLuaMachine(it) }
    private val errorLock: Lock = ReentrantLock()
    private val hasError = errorLock.newCondition()

    @Volatile
    private var error: Throwable? = null
    override fun close() {
        try {
            context.ensureClosed(1, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            throw IllegalStateException("Runtime thread was interrupted", e)
        }
    }

    fun context(): ComputerContext {
        return context
    }

    /**
     * Create a new computer which pulls from our task queue.
     *
     * @return The computer. This will not be started yet, you must call [Computer.turnOn] and
     * [Computer.tick] to do so.
     */
    fun create(): Computer {
        val queue: Queue<FakeComputerTask> = ConcurrentLinkedQueue()
        val computer = Computer(context, BasicEnvironment(), Terminal(51, 19, true), 0)
        computer.addApi(QueuePassingAPI(queue)) // Inject an extra API to pass the queue to the machine.
        machines[computer] = queue
        return computer
    }

    /**
     * Create and start a new computer which loops forever.
     */
    fun createLoopingComputer() {
        val computer = create()
        enqueueForever(computer) {
            Thread.sleep(100)
            MachineResult.OK
        }
        computer.turnOn()
        computer.tick()
    }

    /**
     * Enqueue a task on a computer.
     *
     * @param computer The computer to enqueue the work on.
     * @param task     The task to run.
     */
    fun enqueue(computer: Computer, task: FakeComputerTask) {
        machines[computer]!!.offer(task)
    }

    /**
     * Enqueue a repeated task on a computer. This is automatically requeued when the task finishes, meaning the task
     * queue is never empty.
     *
     * @param computer The computer to enqueue the work on.
     * @param task     The task to run.
     */
    private fun enqueueForever(computer: Computer, task: FakeComputerTask) {
        machines[computer]!!.offer {
            val result = task(it)
            enqueueForever(computer, task)
            computer.queueEvent("some_event", null)
            result
        }
    }

    /**
     * Sleep for a given period, immediately propagating any exceptions thrown by a computer.
     *
     * @param delay The duration to sleep for.
     * @param unit  The time unit the duration is measured in.
     * @throws Exception An exception thrown by a running computer.
     */
    @Throws(Exception::class)
    fun sleep(delay: Long, unit: TimeUnit?) {
        errorLock.lock()
        try {
            rethrowIfNeeded()
            if (hasError.await(delay, unit)) rethrowIfNeeded()
        } finally {
            errorLock.unlock()
        }
    }

    /**
     * Start a computer and wait for it to finish.
     *
     * @param computer The computer to wait for.
     * @throws Exception An exception thrown by a running computer.
     */
    @Throws(Exception::class)
    fun startAndWait(computer: Computer) {
        computer.turnOn()
        computer.tick()
        do {
            sleep(100, TimeUnit.MILLISECONDS)
        } while (context.computerScheduler().hasPendingWork() || computer.isOn)

        rethrowIfNeeded()
    }

    @Throws(Exception::class)
    private fun rethrowIfNeeded() {
        val error = error ?: return
        throw error
    }

    private class QueuePassingAPI constructor(val tasks: Queue<FakeComputerTask>) : ILuaAPI {
        override fun getNames(): Array<String> = arrayOf()
    }

    private inner class DummyLuaMachine(private val environment: MachineEnvironment) : KotlinLuaMachine(environment) {
        private var tasks: Queue<FakeComputerTask>? = null
        override fun addAPI(api: ILuaAPI) {
            super.addAPI(api)
            if (api is QueuePassingAPI) tasks = api.tasks
        }

        override fun getTask(): (suspend KotlinLuaMachine.() -> Unit)? {
            try {
                val tasks = this.tasks ?: throw NullPointerException("Not received tasks yet")
                val task = tasks.remove()
                return {
                    try {
                        task(environment.timeout)
                    } catch (e: Throwable) {
                        reportError(e)
                    }
                }
            } catch (e: Throwable) {
                reportError(e)
                return null
            }
        }

        override fun close() {}

        private fun reportError(e: Throwable) {
            errorLock.lock()
            try {
                if (error == null) {
                    error = e
                    hasError.signal()
                } else {
                    error!!.addSuppressed(e)
                }
            } finally {
                errorLock.unlock()
            }

            if (e is Exception || e is AssertionError) return else throw e
        }
    }
}
