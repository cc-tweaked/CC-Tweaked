package dan200.computercraft.ingame

import dan200.computercraft.ComputerCraft
import dan200.computercraft.ingame.api.sequence
import dan200.computercraft.ingame.api.thenComputerOk
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraftforge.gametest.GameTestHolder

@GameTestHolder(ComputerCraft.MOD_ID)
class CraftOs_Test {
    /**
     * Sends a rednet message to another a computer and back again.
     */
    @GameTest(timeoutTicks = 200)
    fun Sends_basic_rednet_messages(context: GameTestHelper) = context.sequence { thenComputerOk("main") }
}
