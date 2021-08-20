package dan200.computercraft.ingame

import dan200.computercraft.ingame.api.*
import dan200.computercraft.shared.Registry
import dan200.computercraft.shared.peripheral.modem.wired.BlockCable
import net.minecraft.util.math.BlockPos

class Modem_Test {
    @GameTest
    fun Have_peripherals(helper: GameTestHelper) = helper.sequence { thenComputerOk(15) }

    @GameTest
    fun Gains_peripherals(helper: GameTestHelper) = helper.sequence {
        val position = BlockPos(2, 2, 2)
        this
            .thenComputerOk(16, "initial")
            .thenExecute {
                helper.setBlock(position, BlockCable.correctConnections(
                    helper.level, helper.absolutePos(position),
                    Registry.ModBlocks.CABLE.get().defaultBlockState().setValue(BlockCable.CABLE, true)
                ))
            }
            .thenComputerOk(16)
    }
}
