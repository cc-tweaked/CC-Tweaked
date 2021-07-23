package dan200.computercraft.ingame

import dan200.computercraft.ingame.api.GameTest
import dan200.computercraft.ingame.api.TestContext
import dan200.computercraft.ingame.api.checkComputerOk

class CraftOsTest {
    /**
     * Sends a rednet message to another a computer and back again.
     */
    @GameTest
    suspend fun `Sends basic rednet messages`(context: TestContext) = context.checkComputerOk(13)
}
