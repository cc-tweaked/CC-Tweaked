package dan200.computercraft.ingame

import dan200.computercraft.ingame.api.GameTest
import dan200.computercraft.ingame.api.GameTestHelper
import dan200.computercraft.ingame.api.sequence
import dan200.computercraft.ingame.api.thenComputerOk

class CraftOs_Test {
    /**
     * Sends a rednet message to another a computer and back again.
     */
    @GameTest
    fun Sends_basic_rednet_messages(context: GameTestHelper) = context.sequence { thenComputerOk("main") }
}
