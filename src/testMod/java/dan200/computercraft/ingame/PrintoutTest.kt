package dan200.computercraft.ingame

import dan200.computercraft.ingame.api.*

class PrintoutTest {
    @GameTest(batch = "client:Printout_Test.In_frame_at_night", timeoutTicks = Timeouts.CLIENT_TIMEOUT)
    fun In_frame_at_night(helper: GameTestHelper) = helper.sequence {
        this
            .thenExecute { helper.positionAtArmorStand() }
            .thenScreenshot()
    }
}
