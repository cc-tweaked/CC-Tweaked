package dan200.computercraft.ingame

import dan200.computercraft.ComputerCraft
import dan200.computercraft.ingame.api.sequence
import dan200.computercraft.ingame.api.thenComputerOk
import dan200.computercraft.shared.Registry
import dan200.computercraft.shared.peripheral.modem.wired.BlockCable
import net.minecraft.core.BlockPos
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraftforge.gametest.GameTestHolder

@GameTestHolder(ComputerCraft.MOD_ID)
class Modem_Test {
    @GameTest(timeoutTicks = TIMEOUT)
    fun Have_peripherals(helper: GameTestHelper) = helper.sequence { thenComputerOk() }

    @GameTest(timeoutTicks = TIMEOUT)
    fun Gains_peripherals(helper: GameTestHelper) = helper.sequence {
        val position = BlockPos(2, 2, 2)
        this
            .thenComputerOk(marker = "initial")
            .thenExecute {
                helper.setBlock(position, BlockCable.correctConnections(
                    helper.level, helper.absolutePos(position),
                    Registry.ModBlocks.CABLE.get().defaultBlockState().setValue(BlockCable.CABLE, true)
                ))
            }
            .thenComputerOk()
    }

    /**
     * Sends a modem message to another computer on the same network
     */
    @GameTest(timeoutTicks = TIMEOUT)
    fun Transmits_messages(context: GameTestHelper) = context.sequence { thenComputerOk("receive") }

    companion object {
        const val TIMEOUT = 200
    }
}
