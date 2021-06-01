package dan200.computercraft.ingame

import dan200.computercraft.ingame.api.GameTest
import dan200.computercraft.ingame.api.TestContext
import dan200.computercraft.ingame.api.checkComputerOk

class TurtleTest {
    @GameTest
    suspend fun `Unequip refreshes peripheral`(context: TestContext) = context.checkComputerOk(1)

    /**
     * Checks turtles can sheer sheep (and drop items)
     *
     * @see [#537](https://github.com/SquidDev-CC/CC-Tweaked/issues/537)
     */
    @GameTest
    suspend fun `Shears sheep`(context: TestContext) = context.checkComputerOk(5)

    /**
     * Checks turtles can place lava.
     *
     * @see [#518](https://github.com/SquidDev-CC/CC-Tweaked/issues/518)
     */
    @GameTest
    suspend fun `Place lava`(context: TestContext) = context.checkComputerOk(5)

    /**
     * Checks turtles can place when waterlogged.
     *
     * @see [#385](https://github.com/SquidDev-CC/CC-Tweaked/issues/385)
     */
    @GameTest
    suspend fun `Place waterlogged`(context: TestContext) = context.checkComputerOk(7)

    /**
     * Checks turtles can pick up lava
     *
     * @see [#297](https://github.com/SquidDev-CC/CC-Tweaked/issues/297)
     */
    @GameTest
    suspend fun `Gather lava`(context: TestContext) = context.checkComputerOk(8)

    /**
     * Checks turtles can hoe dirt.
     *
     * @see [#258](https://github.com/SquidDev-CC/CC-Tweaked/issues/258)
     */
    @GameTest
    suspend fun `Hoe dirt`(context: TestContext) = context.checkComputerOk(9)

    /**
     * Checks turtles can place monitors
     *
     * @see [#691](https://github.com/SquidDev-CC/CC-Tweaked/issues/691)
     */
    @GameTest
    suspend fun `Place monitor`(context: TestContext) = context.checkComputerOk(10)

    /**
     * Checks turtles can place into compostors. These are non-typical inventories, so
     * worth testing.
     */
    @GameTest
    suspend fun `Use compostors`(context: TestContext) = context.checkComputerOk(11)

    /**
     * Checks turtles can be cleaned in cauldrons.
     */
    @GameTest
    suspend fun `Cleaned with cauldrons`(context: TestContext) = context.checkComputerOk(12)
}
