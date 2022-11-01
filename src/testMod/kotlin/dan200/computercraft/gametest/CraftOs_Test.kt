/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.gametest

import dan200.computercraft.gametest.api.GameTestHolder
import dan200.computercraft.gametest.api.Timeouts
import dan200.computercraft.gametest.api.sequence
import dan200.computercraft.gametest.api.thenComputerOk
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper

@GameTestHolder
class CraftOs_Test {
    /**
     * Sends a rednet message to another a computer and back again.
     */
    @GameTest(timeoutTicks = Timeouts.COMPUTER_TIMEOUT)
    fun Sends_basic_rednet_messages(context: GameTestHelper) = context.sequence { thenComputerOk("main") }
}
