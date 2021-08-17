package dan200.computercraft.ingame

import dan200.computercraft.ingame.api.*
import dan200.computercraft.shared.Registry
import dan200.computercraft.shared.peripheral.modem.wired.BlockCable
import net.minecraft.util.math.BlockPos

class ModemTest {
    @GameTest
    suspend fun `Have peripherals`(context: TestContext) = context.checkComputerOk(15)

    @GameTest
    suspend fun `Gains peripherals`(context: TestContext) {
        val position = BlockPos(2, 0, 2)
        context.checkComputerOk(16, "initial")

        context.setBlock(position, BlockCable.correctConnections(
            context.level, context.offset(position),
            Registry.ModBlocks.CABLE.get().defaultBlockState().setValue(BlockCable.CABLE, true)
        ))

        context.checkComputerOk(16)
    }
}
