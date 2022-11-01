package dan200.computercraft.test.core.computer

import dan200.computercraft.api.lua.ILuaAPI
import dan200.computercraft.api.lua.ILuaContext
import dan200.computercraft.core.lua.ILuaMachine
import dan200.computercraft.core.lua.MachineEnvironment
import dan200.computercraft.core.lua.MachineResult
import kotlinx.coroutines.*
import java.io.InputStream
import kotlin.coroutines.CoroutineContext

/**
 * An [ILuaMachine] which runs Kotlin functions instead.
 */
abstract class KotlinLuaMachine(environment: MachineEnvironment) : ILuaMachine, AbstractLuaTaskContext() {
    override val context: ILuaContext = environment.context

    override fun addAPI(api: ILuaAPI) = addApi(api)

    override fun loadBios(bios: InputStream): MachineResult = MachineResult.OK

    override fun handleEvent(eventName: String?, arguments: Array<out Any>?): MachineResult {
        if (hasEventListeners) {
            queueEvent(eventName, arguments)
        } else {
            val task = getTask()
            if (task != null) CoroutineScope(NeverDispatcher() + CoroutineName("Computer")).launch { task() }
        }

        return MachineResult.OK
    }

    override fun printExecutionState(out: StringBuilder) {}

    /**
     * Get the next task to execute on this computer.
     */
    protected abstract fun getTask(): (suspend KotlinLuaMachine.() -> Unit)?

    /**
     * A [CoroutineDispatcher] which only allows resuming from the computer thread. In practice, this means the only
     * way to yield is with [pullEvent].
     */
    private class NeverDispatcher : CoroutineDispatcher() {
        private val expectedGroup = Thread.currentThread().threadGroup

        override fun dispatch(context: CoroutineContext, block: Runnable) {
            if (Thread.currentThread().threadGroup != expectedGroup) {
                throw UnsupportedOperationException("Cannot perform arbitrary yields")
            }

            block.run()
        }
    }
}
