package dan200.computercraft.ingame

import dan200.computercraft.ingame.api.GameTest
import dan200.computercraft.ingame.api.TestContext
import dan200.computercraft.ingame.api.checkComputerOk

class TurtleTest {
    @GameTest(required = false)
    suspend fun `Unequip refreshes peripheral`(context: TestContext) = context.checkComputerOk(1)

    /**
     * Checks turtles can sheer sheep (and drop items)
     *
     * @see [#537](https://github.com/SquidDev-CC/CC-Tweaked/issues/537)
     */
    @GameTest(required = false)
    suspend fun `Shears sheep`(context: TestContext) = context.checkComputerOk(5)

    /**
     * Checks turtles can place lava.
     *
     * @see [#518](https://github.com/SquidDev-CC/CC-Tweaked/issues/518)
     */
    @GameTest(required = false)
    suspend fun `Place lava`(context: TestContext) = context.checkComputerOk(5)

    /**
     * Checks turtles can place when waterlogged.
     *
     * @see [#385](https://github.com/SquidDev-CC/CC-Tweaked/issues/385)
     */
    @GameTest(required = false)
    suspend fun `Place waterlogged`(context: TestContext) = context.checkComputerOk(7)

    /**
     * Checks turtles can place when waterlogged.
     *
     * @see [#297](https://github.com/SquidDev-CC/CC-Tweaked/issues/297)
     */
    @GameTest(required = false)
    suspend fun `Gather lava`(context: TestContext) = context.checkComputerOk(8)

    /**
     * Checks turtles can place when waterlogged.
     *
     * @see [#258](https://github.com/SquidDev-CC/CC-Tweaked/issues/258)
     */
    @GameTest(required = false)
    suspend fun `Hoe dirt`(context: TestContext) = context.checkComputerOk(9)
}
