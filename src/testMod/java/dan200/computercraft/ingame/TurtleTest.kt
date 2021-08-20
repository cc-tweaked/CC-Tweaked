package dan200.computercraft.ingame

import dan200.computercraft.ingame.api.GameTest
import dan200.computercraft.ingame.api.GameTestHelper
import dan200.computercraft.ingame.api.sequence
import dan200.computercraft.ingame.api.thenComputerOk

class Turtle_Test {
    @GameTest(timeoutTicks = TIMEOUT)
    fun Unequip_refreshes_peripheral(helper: GameTestHelper) = helper.sequence { thenComputerOk(1) }

    /**
     * Checks turtles can sheer sheep (and drop items)
     *
     * @see [#537](https://github.com/SquidDev-CC/CC-Tweaked/issues/537)
     */
    @GameTest(timeoutTicks = TIMEOUT)
    fun Shears_sheep(helper: GameTestHelper) = helper.sequence { thenComputerOk(5) }

    /**
     * Checks turtles can place lava.
     *
     * @see [#518](https://github.com/SquidDev-CC/CC-Tweaked/issues/518)
     */
    @GameTest(timeoutTicks = TIMEOUT)
    fun Place_lava(helper: GameTestHelper) = helper.sequence { thenComputerOk(5) }

    /**
     * Checks turtles can place when waterlogged.
     *
     * @see [#385](https://github.com/SquidDev-CC/CC-Tweaked/issues/385)
     */
    @GameTest(timeoutTicks = TIMEOUT)
    fun Place_waterlogged(helper: GameTestHelper) = helper.sequence { thenComputerOk(7) }

    /**
     * Checks turtles can pick up lava
     *
     * @see [#297](https://github.com/SquidDev-CC/CC-Tweaked/issues/297)
     */
    @GameTest(timeoutTicks = TIMEOUT)
    fun Gather_lava(helper: GameTestHelper) = helper.sequence { thenComputerOk(8) }

    /**
     * Checks turtles can hoe dirt.
     *
     * @see [#258](https://github.com/SquidDev-CC/CC-Tweaked/issues/258)
     */
    @GameTest(timeoutTicks = TIMEOUT)
    fun Hoe_dirt(helper: GameTestHelper) = helper.sequence { thenComputerOk(9) }

    /**
     * Checks turtles can place monitors
     *
     * @see [#691](https://github.com/SquidDev-CC/CC-Tweaked/issues/691)
     */
    @GameTest(timeoutTicks = TIMEOUT)
    fun Place_monitor(helper: GameTestHelper) = helper.sequence { thenComputerOk(10) }

    /**
     * Checks turtles can place into compostors. These are non-typical inventories, so
     * worth testing.
     */
    @GameTest(timeoutTicks = TIMEOUT)
    fun Use_compostors(helper: GameTestHelper) = helper.sequence { thenComputerOk(11) }

    /**
     * Checks turtles can be cleaned in cauldrons.
     *
     * Currently not required as turtles can no longer right-click cauldrons.
     */
    @GameTest
    fun Cleaned_with_cauldrons(helper: GameTestHelper) = helper.sequence { thenComputerOk(12) }

    companion object {
        const val TIMEOUT = 200
    }
}
