package dan200.computercraft.test.core.computer

import dan200.computercraft.api.lua.ILuaAPI
import dan200.computercraft.api.lua.ILuaContext
import dan200.computercraft.core.lua.ILuaMachine
import dan200.computercraft.core.lua.MachineEnvironment
import dan200.computercraft.core.lua.MachineResult
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.InputStream

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
            if (task != null) CoroutineScope(Dispatchers.Unconfined + CoroutineName("Computer")).launch { task() }
        }

        return MachineResult.OK
    }

    override fun printExecutionState(out: StringBuilder) {}

    /**
     * Get the next task to execute on this computer.
     */
    protected abstract fun getTask(): (suspend KotlinLuaMachine.() -> Unit)?
}
