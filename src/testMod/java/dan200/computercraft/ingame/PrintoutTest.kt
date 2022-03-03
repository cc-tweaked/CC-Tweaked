package dan200.computercraft.ingame

import dan200.computercraft.ingame.api.positionAtArmorStand
import dan200.computercraft.ingame.api.sequence
import dan200.computercraft.ingame.api.thenScreenshot
import net.minecraft.gametest.framework.GameTestHelper

class PrintoutTest {
    // @GameTest(batch = "Printout_Test.In_frame_at_night", timeoutTicks = Timeouts.CLIENT_TIMEOUT)
    fun In_frame_at_night(helper: GameTestHelper) = helper.sequence {
        this
            .thenExecute { helper.positionAtArmorStand() }
            .thenScreenshot()
    }
}
