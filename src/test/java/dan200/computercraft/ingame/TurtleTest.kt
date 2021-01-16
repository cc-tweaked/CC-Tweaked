package dan200.computercraft.ingame

import dan200.computercraft.ingame.api.GameTest
import dan200.computercraft.ingame.api.TestContext
import dan200.computercraft.ingame.api.checkComputerOk

class TurtleTest {
    @GameTest(required = false)
    suspend fun `Unequip refreshes peripheral`(context: TestContext) = context.checkComputerOk(1)
}
